-- 启用扩展
CREATE EXTENSION IF NOT EXISTS vector;

-- 创建独立 schema
CREATE SCHEMA IF NOT EXISTS ai;

-- RAG 向量文档表（Spring AI 默认使用 ai_document 表名）
CREATE TABLE IF NOT EXISTS ai.ai_document (
    id VARCHAR(36) PRIMARY KEY,
    content TEXT NOT NULL,
    metadata JSONB NOT NULL DEFAULT '{}',
    embedding vector(1024)
);

-- 创建索引（HNSW 算法，召回率高且查询快）
CREATE INDEX IF NOT EXISTS ai.ai_document_idx ON ai.ai_document
    USING hnsw (embedding vector_cosine_ops);

-- 追踪来源文件的元数据表
CREATE TABLE IF NOT EXISTS ai.ai_source_files (
    id BIGSERIAL PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    file_hash VARCHAR(64) NOT NULL UNIQUE,
    file_size BIGINT,
    document_count INT DEFAULT 0,
    category VARCHAR(100),
    tags TEXT[],
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE
);