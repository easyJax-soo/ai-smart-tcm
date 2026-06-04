package com.bobo.aismartcloud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 会话历史列表项
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatHistoryItem {
    /** 会话 ID（UUID 字符串） */
    private String chatId;
    /** 标题：取首条 USER 消息截断 30 字 */
    private String title;
    /** 最近一次更新时间 */
    private LocalDateTime updatedAt;
    /** 当前会话中的消息条数（已应用窗口截断） */
    private Integer messageCount;
}
