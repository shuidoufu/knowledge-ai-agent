package com.example.aiagent.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 网页 HTML 转 Markdown 知识库文档工具
 * 使用 jsoup 解析收藏的网页 HTML 文件，提取正文并转换为符合 RAG 加载规范的 Markdown 文档。
 * 支持四种输入：
 * 1. 浏览器书签文件（Chrome/Edge 的 Bookmarks JSON、导出的 NETSCAPE 书签 HTML、
 *    Chrome 导出的 # Bookmarks 格式 Markdown）：自动解析书签链接 → 批量下载网页 → 转换 Markdown
 * 2. 网页 HTML 目录：逐个转换目录下的 .html/.htm 文件
 * 3. 单个网页 HTML 文件：转换该文件
 *
 * 转换规则：
 * 1. 剔除脚本、样式、导航、广告等噪音元素，优先提取 article/main 等正文区域
 * 2. 标题 h1-h6 转 # 标题、pre/code 转代码块、table 转 Markdown 表格
 * 3. 列表、引用、链接、加粗斜体等格式保留
 * 4. 在 ## 标题前添加 --- 分割线（代码围栏内跳过），压缩多余空行
 *
 * 使用方式（项目根目录执行，推荐脚本）：
 *   html-to-md.bat "C:\Users\xxx\AppData\Local\Google\Chrome\User Data\Default\Bookmarks"
 *   html-to-md.bat bookmarks.html
 *   html-to-md.bat bookmarks.md
 *   html-to-md.bat D:\网页收藏目录
 * 也可直接：
 *   mvnw exec:java -q -Dexec.mainClass="com.example.aiagent.rag.HtmlToMarkdownConverter" \
 *        -Dexec.args="--input <书签文件或目录> --output src/main/resources/document"
 */
public class HtmlToMarkdownConverter {

    /** 噪音元素选择器（脚本、样式、导航、广告、评论区等，正文提取前移除） */
    private static final String NOISE_SELECTOR =
            "script, style, noscript, template, iframe, svg, canvas, audio, video, form, " +
            "nav, header, footer, aside, [hidden], " +
            ".ad, .ads, .adsbygoogle, .advertisement, .advertising, .banner, .sidebar, " +
            ".breadcrumb, .toolbar, .pagination, .menu, .toc, .share, .social, " +
            ".comment, .comments, .related, .recommend, .copyright, .popup, .modal, " +
            ".subscribe, .newsletter, .cookie, " +
            ".article-info-box, .toolbox-list, .more-toolbox, .recommend-box, .hide-article-box";

    /** 正文区域选择器（按优先级依次尝试，未命中时退回 body） */
    private static final String MAIN_CONTENT_SELECTOR =
            "article, main, [role=main], " +
            ".entry-content, .post-content, .article, .post, .markdown-body, " +
            ".docs-content, .documentation, .prose, #content, #main";

    /** 匹配连续的三个以上空行，压缩为最多两个空行 */
    private static final Pattern EXCESS_BLANK_LINES = Pattern.compile("\\n{3,}");

    /** 行尾空白 */
    private static final Pattern TRAILING_WHITESPACE = Pattern.compile("[ \\t]+$", Pattern.MULTILINE);

    /** 通用文件名（如浏览器"另存为"的 index.html），此时用页面标题作为输出文件名 */
    private static final Pattern GENERIC_FILE_NAME = Pattern.compile(
            "(?i)^(index|default|untitled|无标题)\\.html?$"
    );

    /** Markdown 书签链接格式：[标题](https://链接) */
    private static final Pattern MARKDOWN_LINK = Pattern.compile(
            "\\[([^\\[\\]]*)\\]\\((https?://[^)\\s]+)\\)"
    );

    /** 书签模式下载并发线程数 */
    private static final int DOWNLOAD_THREADS = 4;

    /** 书签模式下载超时（毫秒） */
    private static final int DOWNLOAD_TIMEOUT_MS = 20000;

    /** 书签模式下载失败重试次数 */
    private static final int DOWNLOAD_RETRIES = 2;

    /** 书签模式下载请求头（完整浏览器 UA，部分站点校验 UA） */
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36";

    /** 书签下载的 HTML 缓存目录（已存在的文件跳过下载） */
    private static final String HTML_CACHE_DIR = "tmp/bookmarks-html";

    /** 书签模式下载携带的 Cookie（--cookie / --cookie-file 传入，用于绕过站点风控） */
    private static String cookieHeader;

    /** 处理统计 */
    private static int processedCount;
    private static int skippedCount;

    /**
     * 主入口
     * 不传参数时使用默认路径：--input html-notes --output src/main/resources/document
     * 输入支持三种：浏览器书签文件（JSON / 导出 HTML）、网页 HTML 目录、单个 HTML 文件
     */
    public static void main(String[] args) throws IOException {
        String inputDir = "html-notes";
        String outputDir = "src/main/resources/document";

        if (args.length > 0) {
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--input":
                        if (i + 1 < args.length) {
                            inputDir = args[++i];
                        }
                        break;
                    case "--output":
                        if (i + 1 < args.length) {
                            outputDir = args[++i];
                        }
                        break;
                    case "--cookie":
                        if (i + 1 < args.length) {
                            cookieHeader = args[++i].trim();
                        }
                        break;
                    case "--cookie-file":
                        if (i + 1 < args.length) {
                            cookieHeader = Files.readString(Paths.get(args[++i]), StandardCharsets.UTF_8).trim();
                        }
                        break;
                    case "--help":
                    case "-h":
                        printUsage();
                        return;
                }
            }
        } else {
            System.out.println("使用默认路径：");
            System.out.println("  --input " + inputDir);
            System.out.println("  --output " + outputDir);
            System.out.println("可通过 --input 和 --output 参数自定义路径");
            System.out.println();
        }

        Path inputPath = Paths.get(inputDir);
        Path outputPath = Paths.get(outputDir);

        if (!Files.exists(inputPath)) {
            System.err.println("错误：输入文件或目录不存在: " + inputPath.toAbsolutePath());
            return;
        }

        Files.createDirectories(outputPath);

        processedCount = 0;
        skippedCount = 0;

        if (Files.isDirectory(inputPath)) {
            // 网页 HTML 目录模式
            processDirectory(inputPath, outputPath);
        } else if (isBookmarksJson(inputPath)) {
            // 浏览器书签 JSON 模式（Chrome/Edge 的 Bookmarks 文件）
            processBookmarksJson(inputPath, outputPath);
        } else if (isBookmarkHtmlFile(inputPath)) {
            // 浏览器导出的书签 HTML 模式（NETSCAPE-Bookmark-file-1）
            processBookmarksHtml(inputPath, outputPath);
        } else if (isMarkdownFile(inputPath)) {
            // Markdown 书签导出模式（Chrome 导出的 # Bookmarks 格式）
            processBookmarksMarkdown(inputPath, outputPath);
        } else if (isHtmlFile(inputPath)) {
            // 单个网页 HTML 文件模式
            processFile(inputPath, outputPath.resolve(inputPath.getFileName()));
        } else {
            System.err.println("错误：不支持的文件类型: " + inputPath.toAbsolutePath());
            System.out.println("支持：书签 JSON（Chrome/Edge Bookmarks）、书签导出 HTML/Markdown、网页 HTML 目录、单个 HTML 文件");
            return;
        }

        System.out.println();
        System.out.println("转换完成：处理 " + processedCount + " 个文件" + (skippedCount > 0 ? "，跳过 " + skippedCount + " 个空文件" : ""));
        System.out.println("输出目录: " + outputPath.toAbsolutePath());
        System.out.println("重启后端服务后自动加载到知识库");
    }

    private static void printUsage() {
        System.out.println("网页 HTML 转 Markdown 知识库文档工具");
        System.out.println();
        System.out.println("用法:");
        System.out.println("  HtmlToMarkdownConverter --input <书签文件或网页目录> --output <输出目录>");
        System.out.println();
        System.out.println("示例:");
        System.out.println("  Chrome/Edge 书签 JSON：");
        System.out.println("    HtmlToMarkdownConverter --input \"C:\\Users\\xxx\\AppData\\Local\\Google\\Chrome\\User Data\\Default\\Bookmarks\"");
        System.out.println("  浏览器导出的书签 HTML/Markdown（含 # Bookmarks 格式）：");
        System.out.println("    HtmlToMarkdownConverter --input bookmarks.html");
        System.out.println("    HtmlToMarkdownConverter --input bookmarks.md");
        System.out.println("  网页收藏目录（批量转换 HTML）：");
        System.out.println("    HtmlToMarkdownConverter --input ./html-notes --output src/main/resources/document");
        System.out.println();
        System.out.println("书签模式说明:");
        System.out.println("  1. 自动解析书签链接（去重，过滤 chrome:// 等内部链接）");
        System.out.println("  2. 并发下载网页到 " + HTML_CACHE_DIR + "（已存在跳过），失败链接打印提示");
        System.out.println("  3. 下载的 HTML 自动转换 Markdown 输出");
        System.out.println();
        System.out.println("Cookie 支持（绕过 CSDN 等站点风控）:");
        System.out.println("  --cookie \"xxx=yyy; aaa=bbb\"        直接传入 Cookie 头");
        System.out.println("  --cookie-file <文件路径>            从文件读取 Cookie（推荐，避免命令行转义）");
        System.out.println("  获取方式：浏览器 F12 → Network → 刷新页面 → 任选请求 → 右键 Copy → Copy as cURL，");
        System.out.println("  提取其中的 Cookie 字段");
        System.out.println();
        System.out.println("转换规则:");
        System.out.println("  1. 剔除导航、广告、评论区等噪音，提取正文");
        System.out.println("  2. 标题/代码块/表格/列表/引用转为 Markdown 格式");
        System.out.println("  3. 在 ## 标题前添加 --- 分割线，支持分片检索");
    }

    /**
     * 遍历目录：普通网页 HTML 逐个转换，书签 HTML（NETSCAPE 格式）收集后按书签模式
     * 下载收藏的网页再转换，避免书签列表被当作普通网页转换
     */
    private static void processDirectory(Path inputDir, Path outputDir) throws IOException {
        List<Path> bookmarkFiles = new ArrayList<>();
        Files.walkFileTree(inputDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String name = file.getFileName().toString().toLowerCase();
                if (name.endsWith(".html") || name.endsWith(".htm")) {
                    if (isBookmarkHtmlFile(file)) {
                        bookmarkFiles.add(file);
                    } else {
                        processFile(file, resolveOutputPath(file, inputDir, outputDir));
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
        // 目录中的书签文件：解析链接 → 下载收藏的网页 → 转换保存
        for (Path bookmarkFile : bookmarkFiles) {
            System.out.println("识别到书签文件: " + bookmarkFile.getFileName() + "，按书签模式处理");
            processBookmarksHtml(bookmarkFile, outputDir);
        }
    }

    /**
     * 保持子目录结构输出
     */
    private static Path resolveOutputPath(Path sourceFile, Path inputDir, Path outputDir) {
        Path relativePath = inputDir.relativize(sourceFile.getParent());
        Path targetDir = outputDir.resolve(relativePath);
        return targetDir.resolve(sourceFile.getFileName());
    }

    /**
     * 处理单篇 HTML 文件：解析 → 提取正文 → 转 Markdown → 清洗 → 输出
     */
    private static void processFile(Path inputFile, Path outputFile) throws IOException {
        System.out.println("处理: " + inputFile.toAbsolutePath());

        Document doc = Jsoup.parse(inputFile.toFile(), null);
        Element body = doc.body();
        if (body == null) {
            System.out.println("  → 跳过: 页面无内容");
            skippedCount++;
            return;
        }
        removeNoise(body);
        Element main = selectMainContent(doc);
        String markdown = convertToMarkdown(main);

        // 正文无一级标题时，用页面标题兜底
        if (!markdown.startsWith("# ") && !doc.title().isBlank()) {
            markdown = "# " + doc.title().trim() + "\n\n" + markdown;
        }

        String processed = processContent(markdown);
        if (processed.isBlank()) {
            System.out.println("  → 跳过: 提取不到正文内容");
            skippedCount++;
            return;
        }

        // 通用文件名（index.html 等）时用页面标题命名，便于知识库识别主题
        String fileName = resolveOutputFileName(inputFile, doc.title());
        Path target = outputFile.getParent().resolve(fileName);
        Files.createDirectories(target.getParent());
        Files.writeString(target, processed, StandardCharsets.UTF_8);

        long originalLines = inputFile.toFile().length();
        long processedLines = processed.lines().count();
        System.out.println("  → 输出: " + target.toAbsolutePath()
                + " (" + originalLines + " 字节 → " + processedLines + " 行)");
        processedCount++;
    }

    /**
     * 移除脚本、样式、导航、广告等噪音元素
     */
    private static void removeNoise(Element root) {
        root.select(NOISE_SELECTOR).remove();
    }

    /**
     * 选择正文区域：优先 article/main，其次常见正文容器，最后退回 body
     */
    private static Element selectMainContent(Document doc) {
        Element main = doc.selectFirst(MAIN_CONTENT_SELECTOR);
        return main != null ? main : doc.body();
    }

    /**
     * 将正文元素树转换为 Markdown 文本
     */
    private static String convertToMarkdown(Element container) {
        StringBuilder sb = new StringBuilder();
        convertBlocks(container, sb);
        return sb.toString();
    }

    /**
     * 递归转换块级子元素
     */
    private static void convertBlocks(Element container, StringBuilder sb) {
        for (Element child : container.children()) {
            convertBlock(child, sb);
        }
    }

    /**
     * 转换单个块级元素为 Markdown
     */
    private static void convertBlock(Element el, StringBuilder sb) {
        String tag = el.tagName();
        switch (tag) {
            case "h1", "h2", "h3", "h4", "h5", "h6" -> {
                String text = inlineText(el).trim();
                if (!text.isEmpty()) {
                    int level = tag.charAt(1) - '0';
                    sb.append("#".repeat(level)).append(' ').append(text).append("\n\n");
                }
            }
            case "p" -> {
                String text = inlineText(el).trim();
                if (!text.isEmpty()) {
                    sb.append(text).append("\n\n");
                }
            }
            case "pre" -> appendCodeBlock(el, sb);
            case "ul", "ol" -> convertList(el, sb, 0);
            case "table" -> appendTable(el, sb);
            case "blockquote" -> {
                String text = inlineText(el).replace("\n", "\n> ").trim();
                if (!text.isEmpty()) {
                    sb.append("> ").append(text).append("\n\n");
                }
            }
            case "hr" -> sb.append("---\n\n");
            case "img" -> appendImage(sb, el);
            case "script", "style" -> {
                // 防御：噪音选择器漏掉的残留
            }
            default -> {
                if (el.isBlock()) {
                    int before = sb.length();
                    convertBlocks(el, sb);
                    if (sb.length() > before) {
                        sb.append('\n');
                    }
                } else {
                    String text = inlineText(el).trim();
                    if (!text.isEmpty()) {
                        sb.append(text).append("\n\n");
                    }
                }
            }
        }
    }

    /**
     * 转换 pre 代码块，带语言标识（如 language-java）
     */
    private static void appendCodeBlock(Element pre, StringBuilder sb) {
        Element code = pre.selectFirst("code");
        String text = (code != null ? rawText(code) : rawText(pre)).strip();
        if (text.isEmpty()) {
            return;
        }
        String lang = code != null ? extractCodeLanguage(code.className()) : "";
        sb.append("```").append(lang).append("\n").append(text).append("\n```\n\n");
    }

    /**
     * 提取代码语言标识（language-java / lang-java → java）
     */
    private static String extractCodeLanguage(String className) {
        if (className == null || className.isBlank()) {
            return "";
        }
        for (String token : className.trim().split("\\s+")) {
            if (token.startsWith("language-")) {
                return token.substring("language-".length());
            }
            if (token.startsWith("lang-")) {
                return token.substring("lang-".length());
            }
        }
        return "";
    }

    /**
     * 提取元素内原始文本（保留缩进和换行，用于代码块）
     */
    private static String rawText(Element el) {
        StringBuilder sb = new StringBuilder();
        appendRawText(el, sb);
        return sb.toString();
    }

    private static void appendRawText(Element el, StringBuilder sb) {
        for (Node node : el.childNodes()) {
            if (node instanceof TextNode textNode) {
                sb.append(textNode.getWholeText());
            } else if (node instanceof Element child) {
                appendRawText(child, sb);
            }
        }
    }

    /**
     * 转换无序/有序列表，支持嵌套
     */
    private static void convertList(Element listEl, StringBuilder sb, int depth) {
        for (Element li : listEl.children()) {
            if ("li".equals(li.tagName())) {
                appendListItem(li, sb, depth);
            }
        }
    }

    /**
     * 转换单个列表项（行内内容 + 嵌套子列表）
     */
    private static void appendListItem(Element li, StringBuilder sb, int depth) {
        sb.append("  ".repeat(depth));
        boolean ordered = "ol".equals(li.parent().tagName());
        sb.append(ordered ? "1. " : "- ");
        boolean hasContent = false;
        for (Node node : li.childNodes()) {
            if (node instanceof TextNode textNode) {
                String text = textNode.text().trim();
                if (!text.isEmpty()) {
                    sb.append(text);
                    hasContent = true;
                }
            } else if (node instanceof Element child) {
                String tag = child.tagName();
                if ("ul".equals(tag) || "ol".equals(tag)) {
                    if (hasContent) {
                        sb.append('\n');
                    }
                    convertList(child, sb, depth + 1);
                } else if ("br".equals(tag)) {
                    sb.append('\n');
                } else {
                    String text = inlineText(child).trim();
                    if (!text.isEmpty()) {
                        sb.append(text);
                        hasContent = true;
                    }
                }
            }
        }
        sb.append('\n');
    }

    /**
     * 转换 table 为 Markdown 表格（首行作为表头，自动补齐列数）
     */
    private static void appendTable(Element table, StringBuilder sb) {
        List<List<String>> rows = new ArrayList<>();
        for (Element tr : table.select("tr")) {
            List<String> row = new ArrayList<>();
            for (Element cell : tr.select("th, td")) {
                row.add(inlineText(cell).replaceAll("\\s*\n\\s*", " ").trim());
            }
            if (!row.isEmpty()) {
                rows.add(row);
            }
        }
        if (rows.isEmpty()) {
            return;
        }
        int cols = rows.stream().mapToInt(List::size).max().orElse(0);
        if (cols == 0) {
            return;
        }
        for (int i = 0; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            while (row.size() < cols) {
                row.add("");
            }
            sb.append("| ").append(String.join(" | ", row)).append(" |\n");
            if (i == 0) {
                sb.append("| ").append(String.join(" | ", Collections.nCopies(cols, "---"))).append(" |\n");
            }
        }
        sb.append('\n');
    }

    /**
     * 提取元素的行内 Markdown 文本（加粗、斜体、行内代码、链接、图片）
     */
    private static String inlineText(Node node) {
        StringBuilder sb = new StringBuilder();
        if (node instanceof TextNode textNode) {
            sb.append(textNode.text());
        } else if (node instanceof Element el) {
            String tag = el.tagName();
            switch (tag) {
                case "br" -> sb.append('\n');
                case "strong", "b" -> appendWrapped(sb, el, "**");
                case "em", "i" -> appendWrapped(sb, el, "*");
                case "code" -> appendWrapped(sb, el, "`");
                case "a" -> appendLink(sb, el);
                case "img" -> appendImage(sb, el);
                default -> {
                    for (Node child : el.childNodes()) {
                        sb.append(inlineText(child));
                    }
                }
            }
        }
        return sb.toString();
    }

    /**
     * 行内加粗/斜体/代码包裹
     * 只递归子节点内容，避免对自身标签重复命中造成无限递归
     */
    private static void appendWrapped(StringBuilder sb, Element el, String marker) {
        String text = innerText(el).trim();
        if (!text.isEmpty()) {
            sb.append(marker).append(text).append(marker);
        }
    }

    /**
     * 提取元素内部文本（递归子节点，不包含元素自身标签）
     */
    private static String innerText(Element el) {
        StringBuilder sb = new StringBuilder();
        for (Node child : el.childNodes()) {
            sb.append(inlineText(child));
        }
        return sb.toString();
    }

    /**
     * 转换链接为 [文本](地址)，空链接/锚点/脚本链接仅保留文本
     */
    private static void appendLink(StringBuilder sb, Element el) {
        String text = innerText(el).trim();
        String href = el.attr("href");
        if (href.isBlank() || href.startsWith("javascript:") || href.startsWith("#")) {
            sb.append(text);
        } else if (text.isEmpty()) {
            sb.append(href);
        } else {
            sb.append('[').append(text).append("](").append(href).append(')');
        }
    }

    /**
     * 转换图片为 ![alt](src)
     * 无 alt 的图片（点赞/收藏/分享等图标）直接跳过，避免 URL 噪音参与向量化
     * base64 内嵌图片跳过
     */
    private static void appendImage(StringBuilder sb, Element el) {
        String src = el.attr("src");
        String alt = el.attr("alt").trim();
        if (src.startsWith("data:") || alt.isEmpty()) {
            return;
        }
        sb.append("![").append(alt).append("](").append(src).append(')');
    }

    /**
     * 执行清洗规则：## 标题前加 --- 分割线（代码围栏内跳过）、压缩空行、去除行尾空白
     */
    private static String processContent(String content) {
        String result = addHorizontalRules(content);
        result = EXCESS_BLANK_LINES.matcher(result).replaceAll("\n\n");
        result = TRAILING_WHITESPACE.matcher(result).replaceAll("");
        return result.stripLeading() + "\n";
    }

    /**
     * 在 ## 标题前添加 --- 分割线
     * 第一个 ## 不加，后续每个 ## 前加 ---；``` 代码围栏内的 ## 行不处理
     */
    private static String addHorizontalRules(String content) {
        StringBuilder sb = new StringBuilder();
        String[] lines = content.split("\n", -1);
        boolean inFence = false;
        boolean isFirstHeading = true;
        for (String line : lines) {
            if (line.trim().startsWith("```")) {
                inFence = !inFence;
            }
            if (!inFence && line.startsWith("## ")) {
                if (!isFirstHeading) {
                    sb.append("---\n");
                }
                isFirstHeading = false;
            }
            sb.append(line).append("\n");
        }
        return sb.toString();
    }

    /**
     * 确定输出文件名：通用文件名（index.html 等）用页面标题命名，其余保留原名改 .md 后缀
     */
    private static String resolveOutputFileName(Path inputFile, String pageTitle) {
        String name = inputFile.getFileName().toString();
        if (GENERIC_FILE_NAME.matcher(name).matches() && pageTitle != null && !pageTitle.isBlank()) {
            String cleanTitle = sanitizeFileName(pageTitle);
            if (!cleanTitle.isBlank()) {
                return cleanTitle + ".md";
            }
        }
        return name.replaceAll("(?i)\\.html?$", "") + ".md";
    }

    /**
     * 清洗文件名中的非法字符与重复连接符
     */
    private static String sanitizeFileName(String title) {
        String clean = title.replaceAll("[\\\\/:*?\"<>|\\r\\n\\t]", "-");
        clean = clean.replaceAll("[\\-]{2,}", "-").trim();
        return clean.replaceAll("[\\.\\s]+$", "");
    }

    /**
     * 判断是否为浏览器书签 JSON 文件
     * 只认 .json 扩展名或无扩展名的 Bookmarks 文件（Chrome/Edge 的书签文件即精确名为 Bookmarks），
     * 避免书签导出 HTML/Markdown（文件名也可能含 bookmarks）被误判为 JSON
     */
    private static boolean isBookmarksJson(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".json") || name.equals("bookmarks");
    }

    /**
     * 判断是否为浏览器导出的书签 HTML（NETSCAPE-Bookmark-file-1 格式，按文件头识别）
     */
    private static boolean isBookmarkHtmlFile(Path path) throws IOException {
        if (!isHtmlFile(path)) {
            return false;
        }
        byte[] head = new byte[512];
        int read;
        try (InputStream in = Files.newInputStream(path)) {
            read = in.read(head);
        }
        String headText = new String(head, 0, Math.max(read, 0), StandardCharsets.ISO_8859_1);
        return headText.contains("NETSCAPE-Bookmark");
    }

    /**
     * 判断是否为网页 HTML 文件
     */
    private static boolean isHtmlFile(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".html") || name.endsWith(".htm");
    }

    /**
     * 判断是否为 Markdown 书签导出文件（Chrome 导出的 # Bookmarks 格式，含 [标题](链接) 书签）
     */
    private static boolean isMarkdownFile(Path path) {
        return path.getFileName().toString().toLowerCase().endsWith(".md");
    }

    /**
     * 判断是否为可下载的 http/https 链接
     */
    private static boolean isHttpUrl(String url) {
        return url != null && (url.startsWith("http://") || url.startsWith("https://"));
    }

    /**
     * 书签 JSON 模式：解析 Chrome/Edge Bookmarks 文件，下载书签网页并转换
     */
    private static void processBookmarksJson(Path bookmarksFile, Path outputDir) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(bookmarksFile.toFile());
        Map<String, String> urlToName = new LinkedHashMap<>();
        JsonNode roots = root.path("roots");
        for (JsonNode rootNode : roots) {
            collectBookmarkNode(rootNode, urlToName);
        }
        runBookmarkPipeline(urlToName, outputDir);
    }

    /**
     * 递归收集书签 JSON 节点中的网页链接（type=url），按 URL 去重
     */
    private static void collectBookmarkNode(JsonNode node, Map<String, String> urlToName) {
        if (node == null || !node.isObject()) {
            return;
        }
        if ("url".equals(node.path("type").asText(""))) {
            String url = node.path("url").asText("");
            if (isHttpUrl(url)) {
                String name = node.path("name").asText("").trim();
                if (name.isEmpty()) {
                    name = url;
                }
                urlToName.putIfAbsent(url, name);
            }
            return;
        }
        JsonNode children = node.path("children");
        if (children.isArray()) {
            for (JsonNode child : children) {
                collectBookmarkNode(child, urlToName);
            }
        }
    }

    /**
     * 书签 HTML 模式：解析浏览器导出的 NETSCAPE 书签 HTML，下载书签网页并转换
     */
    private static void processBookmarksHtml(Path bookmarksFile, Path outputDir) throws IOException {
        Document doc = Jsoup.parse(bookmarksFile.toFile(), null);
        Map<String, String> urlToName = new LinkedHashMap<>();
        for (Element a : doc.select("a[href]")) {
            String url = a.attr("href");
            if (isHttpUrl(url)) {
                String name = a.text().trim();
                if (name.isEmpty()) {
                    name = url;
                }
                urlToName.putIfAbsent(url, name);
            }
        }
        runBookmarkPipeline(urlToName, outputDir);
    }

    /**
     * Markdown 书签模式：解析 Chrome 导出的 Markdown 书签文件（# Bookmarks 格式的 [标题](链接)），
     * 下载书签网页并转换
     */
    private static void processBookmarksMarkdown(Path bookmarksFile, Path outputDir) throws IOException {
        String content = Files.readString(bookmarksFile, StandardCharsets.UTF_8);
        Map<String, String> urlToName = new LinkedHashMap<>();
        Matcher matcher = MARKDOWN_LINK.matcher(content);
        while (matcher.find()) {
            String url = matcher.group(2);
            String name = matcher.group(1).trim();
            if (name.isEmpty()) {
                name = url;
            }
            urlToName.putIfAbsent(url, name);
        }
        runBookmarkPipeline(urlToName, outputDir);
    }

    /**
     * 书签处理管线：下载全部书签网页到缓存目录，再逐个转换为 Markdown
     */
    private static void runBookmarkPipeline(Map<String, String> urlToName, Path outputDir) throws IOException {
        if (urlToName.isEmpty()) {
            System.out.println("书签中未找到可下载的 http/https 链接");
            return;
        }
        System.out.println("书签模式：解析到 " + urlToName.size() + " 个网页链接");
        Path cacheDir = Paths.get(HTML_CACHE_DIR);
        Files.createDirectories(cacheDir);

        // 预计算唯一缓存文件名（同名书签标题加序号，避免并发下载互相覆盖）
        Map<String, String> urlToFile = new LinkedHashMap<>();
        Map<String, Integer> nameCounts = new HashMap<>();
        for (Map.Entry<String, String> entry : urlToName.entrySet()) {
            String base = sanitizeFileName(entry.getValue());
            int count = nameCounts.merge(base, 1, Integer::sum);
            urlToFile.put(entry.getKey(), count == 1 ? base + ".html" : base + "-" + count + ".html");
        }

        // 并发下载，失败链接记录并打印
        List<String> failedUrls = Collections.synchronizedList(new ArrayList<>());
        List<Path> downloaded = Collections.synchronizedList(new ArrayList<>());
        ExecutorService pool = Executors.newFixedThreadPool(DOWNLOAD_THREADS);
        List<Future<?>> futures = new ArrayList<>();
        for (Map.Entry<String, String> entry : urlToFile.entrySet()) {
            String url = entry.getKey();
            String fileName = entry.getValue();
            futures.add(pool.submit(() -> {
                try {
                    downloaded.add(downloadBookmark(url, fileName, cacheDir));
                } catch (IOException e) {
                    failedUrls.add(url + " (" + e.getMessage() + ")");
                }
            }));
        }
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception ignored) {
                // 单个任务失败已在任务内记录
            }
        }
        pool.shutdown();

        System.out.println("下载完成：成功 " + downloaded.size() + " / 失败 " + failedUrls.size());
        if (!failedUrls.isEmpty()) {
            System.out.println("失败链接（可稍后重试或手动打开）：");
            failedUrls.stream().limit(20).forEach(u -> System.out.println("  " + u));
            if (failedUrls.size() > 20) {
                System.out.println("  ... 共 " + failedUrls.size() + " 条失败");
            }
        }

        // 转换下载的 HTML
        for (Path html : downloaded) {
            processFile(html, outputDir.resolve(html.getFileName()));
        }
    }

    /**
     * 下载单个书签网页到缓存目录，已存在则跳过下载
     * @return 下载的 HTML 文件路径
     */
    private static Path downloadBookmark(String url, String fileName, Path cacheDir) throws IOException {
        Path target = cacheDir.resolve(fileName);
        if (Files.exists(target)) {
            return target;
        }
        IOException lastError = null;
        for (int attempt = 1; attempt <= DOWNLOAD_RETRIES; attempt++) {
            try {
                downloadUrl(url, target);
                System.out.println("下载: " + fileName + " → " + target.getFileName());
                return target;
            } catch (IOException e) {
                lastError = e;
                if (attempt < DOWNLOAD_RETRIES) {
                    try {
                        Thread.sleep(1000L * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
        throw lastError;
    }

    /**
     * 下载网页到目标文件（携带浏览器请求头，编码由后续 jsoup 按页面声明自动识别）
     */
    private static void downloadUrl(String url, Path target) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        try {
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(DOWNLOAD_TIMEOUT_MS);
            conn.setReadTimeout(DOWNLOAD_TIMEOUT_MS);
            conn.setRequestProperty("User-Agent", USER_AGENT);
            conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            conn.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
            if (cookieHeader != null && !cookieHeader.isBlank()) {
                conn.setRequestProperty("Cookie", cookieHeader);
            }
            int code = conn.getResponseCode();
            if (code != 200) {
                throw new IOException("HTTP " + code);
            }
            try (InputStream in = conn.getInputStream();
                 OutputStream out = Files.newOutputStream(target)) {
                in.transferTo(out);
            }
        } finally {
            conn.disconnect();
        }
    }

}
