package com.bobo.aismartcloud.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
@Slf4j
public class TCMVectorStoreConfig {

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
        log.info("[RAG Config] Embedding 模型维度: {}", openAiEmbeddingModel.dimensions());
        return SimpleVectorStore.builder(openAiEmbeddingModel).build();
    }

}
