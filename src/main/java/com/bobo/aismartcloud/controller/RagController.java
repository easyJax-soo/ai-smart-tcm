package com.bobo.aismartcloud.controller;

import com.bobo.aismartcloud.app.TCMApp;
import com.bobo.aismartcloud.rag.entity.TCMRagSourceFile;
import com.bobo.aismartcloud.rag.service.TCMVectorDbService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * RAG 向量数据库管理接口
 */
@RestController
@RequestMapping("/rag")
@Tag(name = "RAG 向量库管理")
@Slf4j
public class RagController {

    @Resource
    private TCMVectorDbService tcmVectorDbService;

    @Resource
    private TCMApp tcmApp;

    @Operation(summary = "上传文件并生成向量",
            description = "支持 Markdown (.md) 格式，后续可通过实现 DocumentParser 接口扩展更多格式")
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadAndEmbed(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "tags", required = false) List<String> tags) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "文件不能为空"));
        }
        try {
            int count = tcmVectorDbService.uploadAndEmbed(file, category, tags);
            if (count == 0) {
                return ResponseEntity.ok(Map.of(
                        "message", "文件已存在，无需重复上传",
                        "chunks", 0));
            }
            return ResponseEntity.ok(Map.of(
                    "message", "上传成功",
                    "fileName", file.getOriginalFilename(),
                    "category", category != null ? category : "",
                    "tags", tags != null ? tags : List.of(),
                    "chunks", count));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("[RAG Controller] 上传失败", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "上传失败: " + e.getMessage()));
        }
    }

    @Operation(summary = "查询所有已上传的源文件列表")
    @GetMapping("/files")
    public ResponseEntity<List<TCMRagSourceFile>> listSourceFiles() {
        return ResponseEntity.ok(tcmVectorDbService.listSourceFiles());
    }

    @Operation(summary = "删除指定文件的向量数据")
    @Parameters({
            @Parameter(name = "fileHash", description = "文件 MD5 哈希值", in = ParameterIn.PATH, required = true)
    })
    @DeleteMapping("/files/{fileHash}")
    public ResponseEntity<Map<String, String>> deleteByHash(@PathVariable String fileHash) {
        tcmVectorDbService.deleteByHash(fileHash);
        return ResponseEntity.ok(Map.of("message", "删除成功"));
    }

    @Operation(summary = "查询当前支持的文档格式")
    @GetMapping("/supported-types")
    public ResponseEntity<Map<String, Object>> getSupportedTypes() {
        return ResponseEntity.ok(Map.of(
                "supportedTypes", tcmVectorDbService.getSupportedTypes(),
                "extensible", true,
                "note", "如需支持新格式，请实现 DocumentParser 接口并添加 @Component 注解"));
    }


    @Operation(summary = "获取RAG根据知识库回答问题")
    @GetMapping("/ask-Rag")
    public Flux<String> getRagResp(String message) {
        String chatId = UUID.randomUUID().toString();
        return tcmApp.doChatWithPgVectorRagByStream(message, chatId);
    }


}