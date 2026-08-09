package com.example.aiagent.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 知识库文档加载器
 * 加载 classpath:document/*.md 文件，使用 MarkdownDocumentReader 解析为文档切片
 * 优化点：
 * 1. status 标签取文件主题（去序号前缀），替代旧的无意义截取规则
 * 2. 把切片标题拼入正文开头，使标题词参与向量化（embedding 仅基于正文，metadata 不参与）
 */
@Slf4j
@Component
public class KnowledgeAppDocumentLoader {
    private final ResourcePatternResolver resourcePatternResolver;

    KnowledgeAppDocumentLoader(ResourcePatternResolver resourcePatternResolver) {
        this.resourcePatternResolver = resourcePatternResolver;
    }

    /**
     * 加载所有 Markdown 文档，按分割线切分为切片
     * 包括 classpath:document/*.md（主知识库）和 classpath:document/yuque-sync/*.md（语雀同步）
     * @return 文档切片列表
     */
    public List<Document> loadMarkdowns() {
        List<Document> allDocuments = new ArrayList<>();
        try {
            // 加载主知识库文档
            Resource[] mainResources = resourcePatternResolver.getResources("classpath:document/*.md");
            for (Resource resource : mainResources) {
                parseDocument(resource, allDocuments);
            }

            // 额外加载语雀同步的文档（子目录不存在时静默跳过）
            try {
                Resource[] yuqueResources = resourcePatternResolver.getResources("classpath:document/yuque-sync/*.md");
                for (Resource resource : yuqueResources) {
                    parseDocument(resource, allDocuments);
                }
            } catch (IOException e) {
                log.debug("语雀同步目录不存在或为空，跳过: {}", e.getMessage());
            }

        } catch (IOException e) {
            log.error("Markdown 文档加载失败", e);
        }
        return allDocuments;
    }

    /**
     * 解析单篇 Markdown 文档，提取文档切片
     */
    private void parseDocument(Resource resource, List<Document> allDocuments) {
        String fileName = resource.getFilename();
        if (fileName == null) {
            return;
        }
        // 获取文档主题作为标签（去序号前缀，如 "1. JAVA.md" → "JAVA"）
        String status = extractTopic(fileName);
        // 旧规则：取文件名倒数第 4-5 位字符作为标签（已废弃，保留参考）
        // String status = fileName.substring(fileName.length() - 6, fileName.length() - 4);
        // 每篇文档的配置
        MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                .withHorizontalRuleCreateDocument(true)
                .withIncludeCodeBlock(false)
                .withIncludeBlockquote(false)
                .withAdditionalMetadata("filename", fileName)
                .withAdditionalMetadata("status", status)
                .build();
        MarkdownDocumentReader reader = new MarkdownDocumentReader(resource, config);
        List<Document> docs = reader.get();
        for (Document doc : docs) {
            // 过滤空切片
            if (doc.getText() == null || doc.getText().isBlank()) {
                continue;
            }
            // 标题拼入正文开头：标题词参与向量化，否则搜标题无法命中（embedding 仅基于正文）
            String title = (String) doc.getMetadata().get("title");
            String text = doc.getText();
            if (title != null && !title.isBlank() && !text.startsWith(title)) {
                text = title + "\n" + text;
            }
            allDocuments.add(Document.builder()
                    .id(doc.getId())
                    .text(text)
                    .metadata(doc.getMetadata())
                    .build());
        }
    }

    /**
     * 从文件名提取主题标签（去 .md 后缀和序号前缀，如 "13.SpringAI-RAG知识库基础.md" → "SpringAI-RAG知识库基础"）
     * @param fileName 文件名
     * @return 主题标签
     */
    private String extractTopic(String fileName) {
        String name = fileName.replaceAll("\\.md$", "");
        return name.replaceFirst("^\\d+\\.\\s*", "");
    }

}
