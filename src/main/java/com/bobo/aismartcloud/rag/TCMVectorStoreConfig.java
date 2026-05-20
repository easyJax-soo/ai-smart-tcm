package com.bobo.aismartcloud.rag;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.util.List;

@Configuration
@Slf4j
public class TCMVectorStoreConfig {

    @Resource
    private TCMDocumentLoader tcmDocumentLoader;

    /**
     * 创建 RAG 向量存储
     * <p>
     * 使用硅基流动 OpenAI 兼容的 Embedding 模型将文档向量化后存入 SimpleVectorStore
     *
     * @param openAiEmbeddingModel 硅基流动向量化模型
     * @return 向量存储实例
     */
    @Bean
    @Lazy
    VectorStore tcmVectorStore(OpenAiEmbeddingModel openAiEmbeddingModel) {
        log.info("[RAG Config] Embedding base-url: https://api.siliconflow.cn");
        log.info("[RAG Config] Embedding model: Qwen/Qwen3-Embedding-0.6B");
        log.info("[RAG Config] Embedding API key 前5位: {}", "sk-pflrtmuivsftehfjuelisvwzfsdtwnpljwpaekmdgpbadrhq".substring(0, 5));
        log.info("[RAG Config] 完整请求URL: POST https://api.siliconflow.cn/v1/embeddings");
        SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(openAiEmbeddingModel)
                .build();

        return simpleVectorStore;
    }

}
