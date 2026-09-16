package com.example.aiagent.controller;

import com.example.aiagent.model.KnowledgeDocumentDetailDTO;
import com.example.aiagent.model.KnowledgeDocumentPageDTO;
import com.example.aiagent.service.AuthService;
import com.example.aiagent.service.KnowledgeDocumentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 知识库文档管理接口（仅管理员可用，权限校验在服务层）
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/ai/knowledge/document")
public class KnowledgeDocumentController {

    private final KnowledgeDocumentService knowledgeDocumentService;
    private final AuthService authService;

    /**
     * 分页查询知识库文档
     * GET /api/ai/knowledge/document/list
     */
    @GetMapping("/list")
    public KnowledgeDocumentPageDTO list(@RequestParam(required = false) String keyword,
                                         @RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "10") int size,
                                         @RequestParam(defaultValue = "time_desc") String sort,
                                         HttpServletRequest request) {
        return knowledgeDocumentService.listDocuments(currentUsername(request), keyword, page, size, sort);
    }

    /**
     * 查看文档当前内容与分块结果
     * GET /api/ai/knowledge/document/{filename}/content
     */
    @GetMapping("/{filename}/content")
    public KnowledgeDocumentDetailDTO content(@PathVariable String filename, HttpServletRequest request) {
        return knowledgeDocumentService.getDocumentDetail(currentUsername(request), filename);
    }

    /**
     * 上传 Markdown 文档（后台异步预处理与向量化）
     * POST /api/ai/knowledge/document/upload
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        return knowledgeDocumentService.uploadDocument(currentUsername(request), file);
    }

    /**
     * 重新入库（清理该文档已入库切片后重跑预处理与向量化）
     * POST /api/ai/knowledge/document/{filename}/reindex
     */
    @PostMapping("/{filename}/reindex")
    public Map<String, Object> reindex(@PathVariable String filename, HttpServletRequest request) {
        return knowledgeDocumentService.reindexDocument(currentUsername(request), filename);
    }

    /**
     * 删除文档（同时清理其向量切片）
     * DELETE /api/ai/knowledge/document/{filename}
     */
    @DeleteMapping("/{filename}")
    public Map<String, Object> delete(@PathVariable String filename, HttpServletRequest request) {
        return knowledgeDocumentService.deleteDocument(currentUsername(request), filename);
    }

    /**
     * 批量删除文档
     * POST /api/ai/knowledge/document/batch-delete
     * Body: { "filenames": ["a.md"] }
     */
    @PostMapping("/batch-delete")
    public Map<String, Object> batchDelete(@RequestBody Map<String, List<String>> body, HttpServletRequest request) {
        List<String> filenames = body == null ? null : body.get("filenames");
        return knowledgeDocumentService.batchDeleteDocuments(currentUsername(request), filenames);
    }

    /**
     * 取当前登录用户名，未登录直接抛出 401
     */
    private String currentUsername(HttpServletRequest request) {
        String username = authService.getCurrentUsername(request);
        if (username == null) {
            throw new IllegalArgumentException("未登录或登录已过期");
        }
        return username;
    }
}
