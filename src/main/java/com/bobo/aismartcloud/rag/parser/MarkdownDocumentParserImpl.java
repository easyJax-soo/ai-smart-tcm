package com.bobo.aismartcloud.rag.parser;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Markdown 文档解析器
 */
@Component
public class MarkdownDocumentParserImpl implements DocumentParser {

    @Override
    public boolean supports(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null) return false;
        return filename.endsWith(".md");
    }

    @Override
    public List<Document> parse(MultipartFile file, String fileHash, String category, List<String> tags) {
        String fileName = file.getOriginalFilename();
        Resource resource = new InputStreamResource(() -> {
            try {
                return file.getInputStream();
            } catch (IOException e) {
                throw new RuntimeException("文件读取失败", e);
            }
        });

        MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                .withHorizontalRuleCreateDocument(true)
                .withIncludeCodeBlock(false)
                .withIncludeBlockquote(false)
                .withAdditionalMetadata("file_name", fileName)
                .withAdditionalMetadata("file_hash", fileHash)
                .build();

        MarkdownDocumentReader reader = new MarkdownDocumentReader(resource, config);
        List<Document> docs = reader.get();

        // 补充元数据（包含分类和标签，供向量检索时过滤）
        for (int i = 0; i < docs.size(); i++) {
            Map<String, Object> meta = new HashMap<>(docs.get(i).getMetadata());
            meta.put("chunk_index", i);
            meta.put("source_file", fileName);
            meta.put("source_hash", fileHash);
            meta.put("file_type", "md");
            if (category != null) meta.put("category", category);
            if (tags != null) meta.put("tags", tags);
            docs.set(i, new Document(docs.get(i).getId(), docs.get(i).getText(), meta));
        }
        return docs;
    }

    @Override
    public String getSupportedTypes() {
        return "Markdown (.md)";
    }
}