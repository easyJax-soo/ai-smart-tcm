package com.bobo.aismartcloud.config;

import com.bobo.aismartcloud.mapper.ChatMemoryMapper;
import com.bobo.aismartcloud.memory.MessagePackChatMemoryCodec;
import com.bobo.aismartcloud.memory.TwoTierChatMemoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

/**
 * 聊天记忆配置：组装 PGSQL + Redis + MessagePack 三件套。
 *
 * 对外只暴露一个 ChatMemory bean，对 TCMApp / 任何 chat
 * 应用完全透明 —— 业务代码继续按 ChatMemory 接口编程。
 */
@Slf4j
@Configuration
public class MemoryConfig {

    @Bean
    public MessagePackChatMemoryCodec messagePackChatMemoryCodec() {
        return new MessagePackChatMemoryCodec();
    }

    @Bean
    public TwoTierChatMemoryRepository twoTierChatMemoryRepository(
            ChatMemoryMapper chatMemoryMapper,
            @Autowired(required = false) StringRedisTemplate redisTemplate,
            MessagePackChatMemoryCodec codec,
            @Value("${chat.memory.redis.ttl:24h}") Duration redisTtl,
            @Value("${chat.memory.redis.key-prefix:chat:memory:}") String keyPrefix
    ) {
        log.info("[MemoryConfig] TwoTierChatMemoryRepository init, redisEnabled={}, ttl={}, keyPrefix='{}'",
                redisTemplate != null, redisTtl, keyPrefix);
        return new TwoTierChatMemoryRepository(
                chatMemoryMapper,
                codec,
                redisTemplate,
                redisTtl,
                keyPrefix);
    }

    @Bean
    public ChatMemory chatMemory(
            TwoTierChatMemoryRepository repository,
            @Value("${chat.memory.window-size:20}") int windowSize
    ) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(windowSize)
                .build();
    }
}
