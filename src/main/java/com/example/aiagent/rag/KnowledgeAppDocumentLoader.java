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
        // 获取文档最后几个字符作为标签
        String status = fileName.substring(fileName.length() - 6, fileName.length() - 4);
        // 每篇文档的配置
        MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                .withHorizontalRuleCreateDocument(true)
                .withIncludeCodeBlock(false)
                .withIncludeBlockquote(false)
                .withAdditionalMetadata("filename", fileName)
                .withAdditionalMetadata("status", status)
                .build();
        MarkdownDocumentReader reader = new MarkdownDocumentReader(resource, config);
        allDocuments.addAll(reader.get());
    }
}
