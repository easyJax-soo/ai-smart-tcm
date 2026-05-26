package com.bobo.aismartcloud.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@Slf4j
public class TCMPgVectorStoreConfig {

    @Bean
    PgVectorStore pgVectorStore(JdbcTemplate jdbcTemplate, OpenAiEmbeddingModel openAiEmbeddingModel) {
        log.info("[RAG PGVector Config] 维度: {}, schema: {}, 表名: {}",
                openAiEmbeddingModel.dimensions(), "ai", "ai_document");
        return PgVectorStore.builder(jdbcTemplate, openAiEmbeddingModel)
                .schemaName("ai")
                .vectorTableName("ai_document")
                .dimensions(1024)
                .initializeSchema(false)
                .build();
    }
}