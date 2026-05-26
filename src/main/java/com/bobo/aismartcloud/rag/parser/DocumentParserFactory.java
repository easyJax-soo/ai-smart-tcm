package com.bobo.aismartcloud.rag.parser;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

/**
 * 文档解析器工厂
 * <p>
 * 根据文件类型自动选择对应的解析器
 */
@Component
@Slf4j
public class DocumentParserFactory {

    @Resource
    private List<DocumentParser> parsers;

    /**
     * 获取支持的解析器
     *
     * @param file 上传的文件
     * @return 解析器（如果找不到返回空）
     */
    public Optional<DocumentParser> getParser(MultipartFile file) {
        return parsers.stream()
                .filter(p -> p.supports(file))
                .findFirst();
    }

    /**
     * 解析文件
     *
     * @param file      上传的文件
     * @param fileHash  文件哈希
     * @param category  分类
     * @param tags      标签列表
     * @return 文档列表
     * @throws IllegalArgumentException 如果没有支持的解析器
     */
    public List<Document> parse(MultipartFile file, String fileHash, String category, List<String> tags) {
        DocumentParser parser = getParser(file)
                .orElseThrow(() -> new IllegalArgumentException(
                        "不支持的文件格式: " + file.getOriginalFilename() +
                        "，当前支持的格式: " + getSupportedTypes()));
        return parser.parse(file, fileHash, category, tags);
    }

    /**
     * 获取所有支持的格式描述
     *
     * @return 格式描述列表
     */
    public String getSupportedTypes() {
        return parsers.stream()
                .map(DocumentParser::getSupportedTypes)
                .reduce((a, b) -> a + ", " + b)
                .orElse("无");
    }
}