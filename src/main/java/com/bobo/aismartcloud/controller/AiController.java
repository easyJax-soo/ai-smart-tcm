package com.bobo.aismartcloud.controller;

import com.bobo.aismartcloud.agent.MyManus;
import com.bobo.aismartcloud.app.TCMApp;
import com.bobo.aismartcloud.dto.ChatHistoryItem;
import com.bobo.aismartcloud.dto.HistoryMessage;
import com.bobo.aismartcloud.mapper.ChatMemoryEntity;
import com.bobo.aismartcloud.memory.MessagePackChatMemoryCodec;
import com.bobo.aismartcloud.memory.TwoTierChatMemoryRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.minimax.MiniMaxChatModel;
import org.springframework.ai.minimax.api.MiniMaxApi;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/ai")
@Slf4j
public class AiController {

    /** 标题截断长度 */
    private static final int TITLE_MAX_LEN = 30;

    @Resource
    private TCMApp tcmAppApp;

    @Resource
    private ToolCallback[] allTools;

    @Resource
    private MiniMaxChatModel miniMaxChatModel;

    @Resource
    private TwoTierChatMemoryRepository chatMemoryRepository;

    @Resource
    private MessagePackChatMemoryCodec chatMemoryCodec;

    @GetMapping("/tcm_app/chat/sync")
    public String doChatWithLoveAppSync(String message, String chatId) {
        return tcmAppApp.doChat(message, chatId);
    }

    @GetMapping(value = "/tcm_app/chat/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatByStream(String message, String chatId) {
        return tcmAppApp.doChatByStream(message, chatId);
    }

    @GetMapping("/tcm_app/chat/sse")
    public Flux<ServerSentEvent<String>> doChatWithTCMSSE(String message, String chatId) {
        return tcmAppApp.doChatByStream(message, chatId)
                .map(chunk -> ServerSentEvent.<String>builder()
                        .data(chunk)
                        .build());
    }

    @GetMapping("/tcm_app/chat/sse/emitter")
    public SseEmitter doChatWithLoveAppSseEmitter(String message, String chatId) {

        SseEmitter emitter = new SseEmitter(180000L);

        tcmAppApp.doChatByStream(message, chatId)
                .subscribe(
                        chunk -> {
                            try {
                                emitter.send(chunk);
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            }
                        },

                        emitter::completeWithError,

                        emitter::complete
                );

        return emitter;
    }


    @GetMapping("/manus/chat")
    public SseEmitter doChatWithManus(String message) {
        MyManus yuManus = new MyManus(allTools, miniMaxChatModel);
        return yuManus.runStream(message);
    }

    /* ============================================================
     *  会话历史（侧边栏用）
     * ============================================================ */

    /**
     * 列出所有会话，按更新时间倒序
     */
    @GetMapping("/chat-history")
    public List<ChatHistoryItem> listChatHistory() {
        return chatMemoryRepository.findAll().stream().map(this::toHistoryItem).toList();
    }

    /**
     * 拉取某次会话的全部消息（按消息顺序）
     */
    @GetMapping("/chat-history/{chatId}/messages")
    public List<HistoryMessage> getChatMessages(@PathVariable String chatId) {
        List<Message> messages = chatMemoryRepository.findByConversationId(chatId);
        if (messages == null || messages.isEmpty()) {
            return Collections.emptyList();
        }
        return messages.stream()
                .map(m -> HistoryMessage.builder()
                        .role(roleOf(m.getMessageType()))
                        .content(m.getText())
                        .build())
                .toList();
    }

    /**
     * 删除某次会话
     */
    @DeleteMapping("/chat-history/{chatId}")
    public void deleteChatHistory(@PathVariable String chatId) {
        chatMemoryRepository.deleteByConversationId(chatId);
    }

    /* ---------- 私有辅助 ---------- */

    private ChatHistoryItem toHistoryItem(ChatMemoryEntity entity) {
        byte[] bytes = entity.getMessages();
        int count = 0;
        String title = "新对话";
        if (bytes != null && bytes.length > 0) {
            try {
                List<Message> messages = chatMemoryCodec.decode(bytes);
                count = messages.size();
                title = messages.stream()
                        .filter(m -> m.getMessageType() == MessageType.USER)
                        .findFirst()
                        .map(m -> truncate(m.getText(), TITLE_MAX_LEN))
                        .orElse("新对话");
            } catch (Exception e) {
                log.warn("[AiController] 解码 chatId={} 失败，使用默认标题", entity.getChatId(), e);
            }
        }
        return ChatHistoryItem.builder()
                .chatId(entity.getChatId())
                .title(title)
                .updatedAt(entity.getUpdatedAt() == null ? LocalDateTime.now() : entity.getUpdatedAt())
                .messageCount(count)
                .build();
    }

    private static String truncate(String s, int max) {
        if (s == null) return "新对话";
        String trimmed = s.replaceAll("\\s+", " ").trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max) + "...";
    }

    private static String roleOf(MessageType type) {
        return switch (type) {
            case USER -> "user";
            case ASSISTANT, SYSTEM -> "ai";
            case TOOL -> "tool";
        };
    }
}
