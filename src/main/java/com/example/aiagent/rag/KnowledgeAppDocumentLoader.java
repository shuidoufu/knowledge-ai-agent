package com.example.aiagent.rag;

import cn.hutool.crypto.digest.DigestUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 知识库文档加载器
 * 加载知识库目录下的 Markdown 文档，使用 MarkdownDocumentReader 解析为文档切片
 * 优化点：
 * 1. status 标签取文件主题（去序号前缀）
 * 2. 把切片标题拼入正文开头，使标题词参与向量化
 */
@Slf4j
@Component
public class KnowledgeAppDocumentLoader {

    /** 知识库文档目录（默认源码目录；打包成 jar 运行时不具备可写性） */
    @Value("${app.knowledge.document-dir:src/main/resources/document}")
    private String documentDir;

    /** 语雀同步子目录名 */
    private static final String YUQUE_SYNC_DIR = "yuque-sync";

    private final ResourcePatternResolver resourcePatternResolver;

    KnowledgeAppDocumentLoader(ResourcePatternResolver resourcePatternResolver) {
        this.resourcePatternResolver = resourcePatternResolver;
    }

    /**
     * 加载所有 Markdown 文档，按分割线切分为切片
     * 包括知识库目录及其 yuque-sync 子目录
     *
     * @return 文档切片列表
     */
    public List<Document> loadMarkdowns() {
        List<Document> allDocuments = new ArrayList<>();
        try {
            for (Resource resource : resolveDocumentResources()) {
                allDocuments.addAll(parseMarkdown(resource));
            }
        } catch (IOException e) {
            log.error("Markdown 文档加载失败", e);
        }
        return allDocuments;
    }

    /**
     * 解析单个 Markdown 资源为文档切片
     *
     * @param resource Markdown 资源
     * @return 文档切片列表（已过滤空切片、已把标题拼入正文）
     */
    public List<Document> parseMarkdown(Resource resource) {
        List<Document> chunks = new ArrayList<>();
        if (resource == null) {
            return chunks;
        }
        String fileName = resource.getFilename();
        if (fileName == null) {
            return chunks;
        }
        // 获取文档主题作为标签（去序号前缀，如 "1. JAVA.md" → "JAVA"）
        String status = extractTopic(fileName);
        MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                .withHorizontalRuleCreateDocument(true)
                .withIncludeCodeBlock(false)
                .withIncludeBlockquote(false)
                .withAdditionalMetadata("filename", fileName)
                .withAdditionalMetadata("status", status)
                .build();
        MarkdownDocumentReader reader = new MarkdownDocumentReader(resource, config);
        for (Document doc : reader.get()) {
            // 过滤空切片
            if (doc.getText() == null || doc.getText().isBlank()) {
                continue;
            }
            // 标题拼入正文开头，使标题词参与向量化
            String title = (String) doc.getMetadata().get("title");
            String text = doc.getText();
            if (title != null && !title.isBlank() && !text.startsWith(title)) {
                text = title + "\n" + text;
            }
            chunks.add(Document.builder()
                    .id(doc.getId())
                    .text(text)
                    .metadata(doc.getMetadata())
                    .build());
        }
        return chunks;
    }

    /**
     * 解析知识库文档目录：配置目录以文件系统形式存在时优先使用（运行时写入的文档无需重新编译即可加载），
     * 不存在时返回 null 表示回退 classpath（打包成 jar 运行时的场景）
     *
     * @return 文档目录路径；不存在时返回 null
     */
    public Path resolveDocumentDirectory() {
        Path dir = Paths.get(documentDir);
        return Files.isDirectory(dir) ? dir : null;
    }

    /**
     * 从文件名提取主题标签（去 .md 后缀和序号前缀，如 "13.SpringAI-RAG知识库基础.md" → "SpringAI-RAG知识库基础"）
     *
     * @param fileName 文件名
     * @return 主题标签
     */
    public static String extractTopic(String fileName) {
        String name = fileName.replaceAll("\\.md$", "");
        return name.replaceFirst("^\\d+\\.\\s*", "");
    }

    /**
     * 生成切片稳定 id：文件名 + 内容 MD5
     *
     * @param filename 文件名
     * @param text     切片正文
     * @return 稳定 id
     */
    public static String stableDocumentId(String filename, String text) {
        return filename + "#" + DigestUtil.md5Hex(text);
    }

    /**
     * 解析知识库文档资源：目录以文件系统形式存在时扫 file: 路径，否则扫 classpath
     */
    private List<Resource> resolveDocumentResources() throws IOException {
        List<Resource> resources = new ArrayList<>();
        Path dir = resolveDocumentDirectory();
        if (dir == null) {
            resources.addAll(Arrays.asList(resourcePatternResolver.getResources("classpath:document/*.md")));
            String yuqueSyncPattern = "classpath:document/" + YUQUE_SYNC_DIR + "/*.md";
            resources.addAll(Arrays.asList(resourcePatternResolver.getResources(yuqueSyncPattern)));
            return resources;
        }
        String base = dir.toUri().toString();
        if (!base.endsWith("/")) {
            base = base + "/";
        }
        Resource[] mainResources = resourcePatternResolver.getResources(base + "*.md");
        resources.addAll(Arrays.asList(mainResources));
        Path yuqueSyncDir = dir.resolve(YUQUE_SYNC_DIR);
        if (Files.isDirectory(yuqueSyncDir)) {
            resources.addAll(Arrays.asList(resourcePatternResolver.getResources(base + YUQUE_SYNC_DIR + "/*.md")));
        }
        return resources;
    }

}
