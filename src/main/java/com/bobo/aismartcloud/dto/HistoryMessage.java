package com.bobo.aismartcloud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会话内的单条历史消息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoryMessage {
    /** "user" 或 "ai" */
    private String role;
    /** 消息文本 */
    private String content;
}
