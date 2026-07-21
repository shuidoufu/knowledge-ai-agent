package com.example.aiagent.rag;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * 语雀文档同步服务
 * 通过语雀 OpenAPI 拉取知识库文档，转换为 Markdown 文件
 * 存储到 document/yuque-sync/ 目录下，供 RAG 知识库加载
 */
@Slf4j
@Service
public class YuqueDocumentSyncService {

    private static final String YUQUE_API_BASE = "https://www.yuque.com/api/v2";
    private static final int DEFAULT_TIMEOUT = 30000;

    @Value("${yuque.token:}")
    private String token;

    @Value("${yuque.namespace:}")
    private String namespace;

    private final ResourcePatternResolver resourcePatternResolver;

    private File yuqueSyncDir;

    public YuqueDocumentSyncService(ResourcePatternResolver resourcePatternResolver) {
        this.resourcePatternResolver = resourcePatternResolver;
    }

    /**
     * 初始化语雀同步目录
     */
    @PostConstruct
    public void init() {
        yuqueSyncDir = resolveYuqueSyncDirectory();
        if (yuqueSyncDir != null) {
            yuqueSyncDir.mkdirs();
            log.info("语雀同步目录: {}", yuqueSyncDir.getAbsolutePath());
        }
    }

    /**
     * 执行语雀文档同步
     * 从语雀知识库拉取文档并写入 yuque-sync 目录
     * @return 成功同步的文档数量
     */
    public int syncDocuments() {
        if (StrUtil.isBlank(token)) {
            log.warn("语雀 Token 未配置，跳过同步。请在 application.yml 中配置 yuque.token");
            return 0;
        }
        if (StrUtil.isBlank(namespace)) {
            log.warn("语雀知识库路径未配置，跳过同步。请在 application.yml 中配置 yuque.namespace");
            return 0;
        }

        log.info("开始同步语雀知识库: {}", namespace);

        // 1. 获取知识库文档列表
        List<JSONObject> docList = fetchDocList();
        if (docList == null || docList.isEmpty()) {
            log.warn("语雀知识库中未找到文档或无法访问: {}", namespace);
            return 0;
        }

        // 确保目录存在
        if (yuqueSyncDir == null) {
            log.error("语雀同步目录初始化失败");
            return 0;
        }
        yuqueSyncDir.mkdirs();

        int successCount = 0;
        List<String> failedDocs = new ArrayList<>();

        // 2. 遍历每篇文档，获取内容并写入文件
        for (JSONObject docInfo : docList) {
            try {
                String docId = getDocId(docInfo);
                String docTitle = docInfo.getStr("title", "untitled");
                if (StrUtil.isBlank(docId)) {
                    continue;
                }

                // 获取单篇文档详情
                JSONObject docDetail = fetchDocDetail(docId);
                if (docDetail == null) {
                    failedDocs.add(docTitle);
                    continue;
                }

                // 写入文件
                writeDocToFile(docTitle, docDetail);
                successCount++;

            } catch (Exception e) {
                String title = docInfo.getStr("title", "unknown");
                failedDocs.add(title);
                log.error("同步文档失败: {}", title, e);
            }
        }

        log.info("语雀文档同步完成: 成功 {} 篇, 失败 {} 篇", successCount, failedDocs.size());
        if (!failedDocs.isEmpty()) {
            log.warn("同步失败的文档: {}", failedDocs);
        }

        return successCount;
    }

    /**
     * 获取知识库文档列表
     */
    private List<JSONObject> fetchDocList() {
        try {
            String url = YUQUE_API_BASE + "/repos/" + namespace + "/docs";
            HttpResponse response = HttpRequest.get(url)
                    .header("X-Auth-Token", token)
                    .header("User-Agent", "ai-agent")
                    .timeout(DEFAULT_TIMEOUT)
                    .execute();

            if (response.getStatus() != 200) {
                log.error("获取语雀文档列表失败, status: {}, body: {}", response.getStatus(), response.body());
                return null;
            }

            String body = response.body();
            if (StrUtil.isBlank(body)) {
                return null;
            }

            JSONObject json = JSONUtil.parseObj(body);
            JSONArray data = json.getJSONArray("data");
            if (data == null || data.isEmpty()) {
                return null;
            }

            List<JSONObject> docList = new ArrayList<>();
            for (int i = 0; i < data.size(); i++) {
                docList.add(data.getJSONObject(i));
            }
            return docList;

        } catch (Exception e) {
            log.error("获取语雀文档列表异常", e);
            return null;
        }
    }

    /**
     * 获取单篇文档详情
     */
    private JSONObject fetchDocDetail(String docId) {
        try {
            String url = YUQUE_API_BASE + "/repos/" + namespace + "/docs/" + docId;
            HttpResponse response = HttpRequest.get(url)
                    .header("X-Auth-Token", token)
                    .header("User-Agent", "ai-agent")
                    .timeout(DEFAULT_TIMEOUT)
                    .execute();

            if (response.getStatus() != 200) {
                log.error("获取语雀文档详情失败, docId: {}, status: {}", docId, response.getStatus());
                return null;
            }

            String body = response.body();
            if (StrUtil.isBlank(body)) {
                return null;
            }

            JSONObject json = JSONUtil.parseObj(body);
            return json.getJSONObject("data");

        } catch (Exception e) {
            log.error("获取语雀文档详情异常, docId: {}", docId, e);
            return null;
        }
    }

    /**
     * 将文档内容写入文件
     */
    private void writeDocToFile(String title, JSONObject docDetail) throws IOException {
        // 获取文档内容，优先取 body（Markdown 格式）
        String content = docDetail.getStr("body", "");
        String format = docDetail.getStr("format", "markdown");

        if (StrUtil.isBlank(content)) {
            log.warn("文档内容为空, title: {}", title);
            return;
        }

        // 如果是 lake 格式，body 可能不是纯 Markdown，尝试取 body_html 或 content
        if ("lake".equals(format) && !isPlainText(content)) {
            String bodyHtml = docDetail.getStr("body_html", "");
            if (StrUtil.isNotBlank(bodyHtml)) {
                content = bodyHtml;
            }
        }

        // 构建文件内容：标题 + 正文
        String fileContent = "# " + title + "\n\n" + content;

        // 安全文件名：替换特殊字符
        String safeFileName = title.replaceAll("[\\\\/:*?\"<>|]", "-")
                .replaceAll("\\s+", "_")
                .replaceAll("_+", "_");
        // 限制文件名长度
        if (safeFileName.length() > 100) {
            safeFileName = safeFileName.substring(0, 100);
        }
        String filePath = yuqueSyncDir.getAbsolutePath() + File.separator + safeFileName + ".md";

        // 写入文件（覆盖已存在的同名文档，保持同步）
        Files.writeString(Path.of(filePath), fileContent, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        log.info("语雀文档已同步: {} ({})", title, filePath);
    }

    /**
     * 从文档列表项中获取文档 ID 或 slug
     */
    private String getDocId(JSONObject docInfo) {
        String id = docInfo.getStr("id");
        if (StrUtil.isNotBlank(id)) {
            return id;
        }
        return docInfo.getStr("slug");
    }

    /**
     * 判断内容是否为纯文本（非 JSON 结构）
     */
    private boolean isPlainText(String content) {
        return !content.trim().startsWith("{") && !content.trim().startsWith("[");
    }

    /**
     * 解析语雀同步目录
     * 优先使用源码目录 src/main/resources/document/yuque-sync/（开发阶段），
     * 回退到 classpath:document/yuque-sync/（生产 JAR 包）
     */
    private File resolveYuqueSyncDirectory() {
        // 优先使用源码目录，便于版本控制
        File sourceDir = new File("src/main/resources/document/yuque-sync");
        if (sourceDir.exists() || sourceDir.mkdirs()) {
            log.info("语雀同步目录: {}", sourceDir.getAbsolutePath());
            return sourceDir;
        }

        // 回退：从 classpath 解析
        try {
            Resource docResource = resourcePatternResolver.getResource("classpath:document/");
            if (docResource.exists()) {
                File docDir = docResource.getFile();
                if (docDir.isDirectory()) {
                    File yuqueDir = new File(docDir, "yuque-sync");
                    yuqueDir.mkdirs();
                    log.info("语雀同步目录: {}", yuqueDir.getAbsolutePath());
                    return yuqueDir;
                }
            }
        } catch (Exception e) {
            log.debug("无法从 classpath 解析 document 目录", e);
        }

        // 最终回退：当前工作目录下
        File fallback = new File("yuque-sync");
        fallback.mkdirs();
        log.info("语雀同步目录: {}", fallback.getAbsolutePath());
        return fallback;
    }
}
