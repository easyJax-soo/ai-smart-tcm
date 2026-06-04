package com.bobo.aismartcloud.memory;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.Collections;
import java.util.Map;

/**
 * MessagePack 序列化的中转 record。
 *
 * Spring AI 的 Message 是多态接口（UserMessage / AssistantMessage / SystemMessage），
 * 直接走 MessagePack 多态序列化需要注册自定义 Serializer，复杂且脆弱。
 * 用扁平 record 中转可以让 schema 自描述、跨语言可读，序列化器只面对稳定结构。
 *
 * 注意：Spring AI 1.1.6 的 public 构造器全部只接受单参 String，
 * 带 metadata 的构造器要么 private 要么 protected。本类只持久化 type + content，
 * metadata 字段保留为 schema 占位（向后兼容，未来若放开访问级别可立即启用）。
 */
public record SerializedMessage(String type, String content, Map<String, Object> metadata) {

    public static SerializedMessage from(Message m) {
        Map<String, Object> meta = m.getMetadata() != null ? m.getMetadata() : Collections.emptyMap();
        return new SerializedMessage(m.getMessageType().name(), m.getText(), meta);
    }

    /**
     * 还原成 Spring AI 的具体消息类型。
     * 当前支持 USER / ASSISTANT / SYSTEM，TOOL 暂不处理。
     */
    public Message toMessage() {
        return switch (MessageType.valueOf(type)) {
            case USER -> new UserMessage(content);
            case ASSISTANT -> new AssistantMessage(content);
            case SYSTEM -> new SystemMessage(content);
            case TOOL -> throw new UnsupportedOperationException(
                    "ToolResponseMessage 暂未实现序列化");
        };
    }
}
