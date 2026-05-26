package com.bobo.aismartcloud.rag.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * RAG 源文件元数据实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TCMRagSourceFile {

    private Long id;
    private String fileName;
    private String fileHash;
    private Long fileSize;
    private Integer documentCount;
    private String category;
    private List<String> tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}