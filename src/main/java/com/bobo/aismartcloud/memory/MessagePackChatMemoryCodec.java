package com.bobo.aismartcloud.memory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.msgpack.jackson.dataformat.MessagePackFactory;
import org.springframework.ai.chat.messages.Message;

import java.util.Collections;
import java.util.List;

/**
 * 聊天记忆 ↔ byte[] 的 MessagePack 编解码器。
 *
 * 使用 Jackson + MessagePackFactory 集成（jackson-dataformat-msgpack），
 * 相比直接用 msgpack-core 反射 API 兼容性更好，跨语言可读。
 *
 * 线程安全：ObjectMapper 实例可复用。
 */
public class MessagePackChatMemoryCodec {

    private final ObjectMapper mapper = new ObjectMapper(new MessagePackFactory());

    /**
     * 把 Spring AI 消息列表编码为 MessagePack 字节数组。
     */
    public byte[] encode(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return new byte[0];
        }
        try {
            return mapper.writeValueAsBytes(messages.stream().map(SerializedMessage::from).toList());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encode chat memory with MessagePack", e);
        }
    }

    /**
     * 从 MessagePack 字节数组还原 Spring AI 消息列表。
     * 输入为 null 或空数组时返回空列表。
     */
    public List<Message> decode(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return Collections.emptyList();
        }
        try {
            List<SerializedMessage> raw = mapper.readValue(
                    bytes,
                    new TypeReference<List<SerializedMessage>>() {});
            return raw.stream().map(SerializedMessage::toMessage).toList();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decode chat memory with MessagePack", e);
        }
    }
}
