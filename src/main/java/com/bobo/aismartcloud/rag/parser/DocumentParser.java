package com.bobo.aismartcloud.rag.parser;

import org.springframework.ai.document.Document;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文档解析器接口
 * <p>
 * 定义不同文件格式的解析规范，后续新增格式只需实现此接口即可
 */
public interface DocumentParser {

    /**
     * 判断是否支持该文件类型
     *
     * @param file 上传的文件
     * @return 是否支持
     */
    boolean supports(MultipartFile file);

    /**
     * 解析文件为 Document 列表
     *
     * @param file      上传的文件
     * @param fileHash  文件哈希（用于元数据追踪）
     * @param category  分类（如"体质辨识"、"食疗养生"）
     * @param tags      标签列表
     * @return 切分后的文档列表
     */
    List<Document> parse(MultipartFile file, String fileHash, String category, List<String> tags);

    /**
     * 获取支持的媒体类型描述
     *
     * @return 如 "Markdown (.md)"
     */
    String getSupportedTypes();
}