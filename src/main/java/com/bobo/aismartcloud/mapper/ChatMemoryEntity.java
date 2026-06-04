package com.bobo.aismartcloud.mapper;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * ai.ai_chat_memory 表映射实体
 *
 * chatId  → chat_id    VARCHAR(64) PK
 * messages → messages   BYTEA
 * updatedAt → updated_at TIMESTAMP
 */
@Data
@TableName("ai.ai_chat_memory")
public class ChatMemoryEntity {

    @TableId(value = "chat_id", type = IdType.INPUT)
    private String chatId;

    @TableField("messages")
    private byte[] messages;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
