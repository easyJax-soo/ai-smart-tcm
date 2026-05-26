package com.bobo.aismartcloud.rag.service;

import cn.hutool.crypto.digest.DigestUtil;
import com.bobo.aismartcloud.rag.entity.TCMRagSourceFile;
import com.bobo.aismartcloud.rag.mapper.TCMRagSourceFileMapper;
import com.bobo.aismartcloud.rag.parser.DocumentParserFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * 向量数据库管理服务
 * <p>
 * 负责：上传文件 → 解析 → 嵌入生成向量 → 写入 pgvector
 */
@Service
@Slf4j
public class TCMVectorDbService {

    private final PgVectorStore pgVectorStore;
    private final TCMRagSourceFileMapper sourceFileMapper;
    private final DocumentParserFactory parserFactory;
    private final JdbcTemplate jdbcTemplate;

    public TCMVectorDbService(
            PgVectorStore pgVectorStore,
            TCMRagSourceFileMapper sourceFileMapper,
            DocumentParserFactory parserFactory,
            JdbcTemplate jdbcTemplate) {
        this.pgVectorStore = pgVectorStore;
        this.sourceFileMapper = sourceFileMapper;
        this.parserFactory = parserFactory;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 上传文件并生成向量
     *
     * @param file      上传的文件
     * @param category  分类
     * @param tags      标签列表
     * @return 写入的文档数量
     */
    @Transactional
    public int uploadAndEmbed(MultipartFile file, String category, List<String> tags) {
        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            throw new IllegalArgumentException("文件名为空");
        }

        long fileSize = file.getSize();
        String fileHash = computeHash(file);

        // 检查是否已存在（hash 去重）
        if (sourceFileMapper.findByHash(fileHash).isPresent()) {
            log.warn("[VectorDB] 文件已存在，跳过: {} (hash={})", fileName, fileHash);
            return 0;
        }

        // 1. 写入源文件记录
        TCMRagSourceFile sourceFile = TCMRagSourceFile.builder()
                .fileName(fileName)
                .fileHash(fileHash)
                .fileSize(fileSize)
                .documentCount(0)
                .category(category)
                .tags(tags)
                .build();
        sourceFileMapper.insert(sourceFile);

        // 2. 解析文件（自动选择解析器）
        List<Document> documents = parserFactory.parse(file, fileHash, category, tags);

        if (documents.isEmpty()) {
            log.warn("[VectorDB] 文档为空: {}", fileName);
            return 0;
        }

        // 3. 写入向量库
        pgVectorStore.add(documents);

        // 4. 更新文档数量
        sourceFileMapper.findByHash(fileHash).ifPresent(f ->
                sourceFileMapper.updateDocumentCount(f.getId(), documents.size()));

        log.info("[VectorDB] 上传成功: {} → {} 个切片, category={}, tags={}", fileName, documents.size(), category, tags);
        return documents.size();
    }

    /**
     * 计算文件 MD5 哈希
     */
    private String computeHash(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            return DigestUtil.md5Hex(is);
        } catch (IOException e) {
            throw new RuntimeException("文件哈希计算失败", e);
        }
    }

    /**
     * 删除指定文件的向量数据（按 hash）
     * <p>
     * 同时删除 ai_document 向量记录和 ai_source_files 元数据（软删除）
     */
    @Transactional
    public void deleteByHash(String fileHash) {
        // 1. 删除向量记录
        jdbcTemplate.update("""
                DELETE FROM ai.ai_document
                WHERE metadata->>'source_hash' = ?
                """, fileHash);

        // 2. 软删除源文件记录
        sourceFileMapper.deleteByHash(fileHash);

        log.info("[VectorDB] 删除文件向量: hash={}", fileHash);
    }

    /**
     * 查询所有已上传的源文件
     */
    public List<TCMRagSourceFile> listSourceFiles() {
        return sourceFileMapper.findAll();
    }

    /**
     * 获取当前支持的文档格式
     */
    public String getSupportedTypes() {
        return parserFactory.getSupportedTypes();
    }
}