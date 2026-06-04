package com.bobo.aismartcloud.memory;

import com.bobo.aismartcloud.mapper.ChatMemoryEntity;
import com.bobo.aismartcloud.mapper.ChatMemoryMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 双层聊天记忆仓库：PGSQL 持久化 + Redis 热缓存。
 *
 * 读路径：
 *   1. Redis hit → 直接 decode 返回（毫秒级）
 *   2. Redis miss 或抛异常 → 查 PG ai.ai_chat_memory → 回填 Redis（best-effort）
 *   3. PG 也无 → 返回空列表
 *
 * 写路径：
 *   1. UPSERT 到 PG（durable）
 *   2. 写 Redis（best-effort，失败仅 warn）
 *
 * Redis 容灾：所有 Redis 调用包 try/catch —— Redis 故障不影响 PG 主流程。
 * 如果 Redis 整体未启用（StringRedisTemplate 为 null），自动降级为 PG-only。
 *
 * 职责边界：本类不做窗口裁剪，由上游 MessageWindowChatMemory 负责。
 */
@Slf4j
public class TwoTierChatMemoryRepository implements ChatMemoryRepository {

    private final ChatMemoryMapper mapper;
    private final MessagePackChatMemoryCodec codec;
    private final StringRedisTemplate redis;   // 可为 null
    private final Duration redisTtl;
    private final String keyPrefix;

    public TwoTierChatMemoryRepository(
            ChatMemoryMapper mapper,
            MessagePackChatMemoryCodec codec,
            StringRedisTemplate redis,
            Duration redisTtl,
            String keyPrefix
    ) {
        this.mapper = Objects.requireNonNull(mapper, "mapper");
        this.codec = Objects.requireNonNull(codec, "codec");
        this.redis = redis;
        this.redisTtl = Objects.requireNonNull(redisTtl, "redisTtl");
        this.keyPrefix = keyPrefix == null ? "chat:memory:" : keyPrefix;
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        byte[] cached = readFromRedis(conversationId);
        if (cached != null) {
            return codec.decode(cached);
        }
        ChatMemoryEntity entity = mapper.selectById(conversationId);
        if (entity == null || entity.getMessages() == null || entity.getMessages().length == 0) {
            return Collections.emptyList();
        }
        byte[] bytes = entity.getMessages();
        writeToRedis(conversationId, bytes);
        return codec.decode(bytes);
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        if (messages == null) {
            return;
        }
        byte[] bytes = codec.encode(messages);
        ChatMemoryEntity entity = new ChatMemoryEntity();
        entity.setChatId(conversationId);
        entity.setMessages(bytes);
        entity.setUpdatedAt(LocalDateTime.now());
        mapper.upsert(entity);
        writeToRedis(conversationId, bytes);
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        mapper.deleteById(conversationId);
        if (redis != null) {
            try {
                redis.delete(redisKey(conversationId));
            } catch (Exception e) {
                log.warn("[ChatMemory] Redis delete failed, chatId={}", conversationId, e);
            }
        }
    }

    @Override
    public List<String> findConversationIds() {
        return mapper.selectAllChatIds();
    }

    /** 全量实体查询（侧边栏列表用，需要 messages + updated_at 解码出 title 和 messageCount） */
    public List<ChatMemoryEntity> findAll() {
        return mapper.selectAll();
    }

    /* ============ Redis 内部方法（容灾） ============ */

    private byte[] readFromRedis(String chatId) {
        if (redis == null) {
            return null;
        }
        try {
            String base64 = redis.opsForValue().get(redisKey(chatId));
            if (base64 == null) {
                return null;
            }
            return Base64.getDecoder().decode(base64);
        } catch (Exception e) {
            log.warn("[ChatMemory] Redis read failed, fallback to PG, chatId={}", chatId, e);
            return null;
        }
    }

    private void writeToRedis(String chatId, byte[] bytes) {
        if (redis == null) {
            return;
        }
        try {
            redis.opsForValue().set(
                    redisKey(chatId),
                    Base64.getEncoder().encodeToString(bytes),
                    redisTtl);
        } catch (Exception e) {
            log.warn("[ChatMemory] Redis write failed, chatId={}", chatId, e);
        }
    }

    private String redisKey(String chatId) {
        return keyPrefix + chatId;
    }

    /**
     * 诊断用，便于日志/Actuator 暴露当前是否走双层。
     */
    public boolean isRedisEnabled() {
        return redis != null;
    }

    @Override
    public String toString() {
        return "TwoTierChatMemoryRepository{redis=" + isRedisEnabled()
                + ", ttl=" + redisTtl
                + ", keyPrefix='" + keyPrefix + "'}";
    }
}
