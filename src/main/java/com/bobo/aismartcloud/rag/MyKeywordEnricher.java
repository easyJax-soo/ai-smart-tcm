package com.bobo.aismartcloud.rag;

import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.minimax.MiniMaxChatModel;
import org.springframework.ai.model.transformer.KeywordMetadataEnricher;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 基于 AI 自动解析关键词并添加到元信息中（为文档补充元信息）
 *
 * */
@Component
public class MyKeywordEnricher {

    @Resource
    private MiniMaxChatModel miniMaxChatModel;

    public List<Document> enrichDocuments(List<Document> documents) {
        KeywordMetadataEnricher enricher = new KeywordMetadataEnricher(this.miniMaxChatModel, 5);
        return enricher.apply(documents);
    }
}