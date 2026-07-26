package com.example.aiagent.rag;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.regex.Pattern;

/**
 * 知识库文档预处理工具
 * 对原始 Markdown 笔记进行清洗和格式化，生成符合 RAG 加载规范的文档
 * 
 * 处理规则：
 * 1. 去除 HTML 内联标签（如 &lt;font&gt;、&lt;u&gt; 等），仅保留纯文本
 * 2. 在 ## 标题前添加 --- 分割线（用于 MarkdownDocumentReader 按主题切片）
 * 3. 清洗多余的空行和尾部空白
 * 
 * 使用方式（项目根目录执行）：
 *   java src/main/java/com/example/aiagent/rag/DocumentPreprocessor.java \
 *        --input ./原始笔记目录 \
 *        --output src/main/resources/document/yuque-sync
 */
@Slf4j
public class DocumentPreprocessor {

    /** 匹配所有 HTML 标签（包括内联样式属性） */
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile(
            "</?[a-zA-Z][^>]*>"
    );

    /** 匹配 ## 二级标题（前面加 --- 分隔） */
    private static final Pattern H2_PATTERN = Pattern.compile(
            "^## ", Pattern.MULTILINE
    );

    /** 匹配连续的三个以上空行，压缩为最多两个空行 */
    private static final Pattern EXCESS_BLANK_LINES = Pattern.compile(
            "\\n{4,}"
    );

    /** 行尾空白 */
    private static final Pattern TRAILING_WHITESPACE = Pattern.compile(
            "[ \\t]+$", Pattern.MULTILINE
    );

    /**
     * 主入口
     * 不传参数时使用默认路径：--input my-notes --output src/main/resources/document/yuque-sync
     */
    public static void main(String[] args) throws IOException {
        String inputDir = "my-notes";
        String outputDir = "src/main/resources/document/yuque-sync";

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

        if (!Files.exists(inputPath) || !Files.isDirectory(inputPath)) {
            System.err.println("错误：输入目录不存在或不是目录: " + inputPath.toAbsolutePath());
            return;
        }

        // 创建输出目录
        Files.createDirectories(outputPath);

        // 遍历处理所有 .md 文件
        processDirectory(inputPath, outputPath);
    }

    private static void printUsage() {
        System.out.println("知识库文档预处理工具");
        System.out.println();
        System.out.println("用法:");
        System.out.println("  DocumentPreprocessor --input <原始笔记目录> --output <输出目录>");
        System.out.println();
        System.out.println("示例:");
        System.out.println("  DocumentPreprocessor --input ./my-notes --output src/main/resources/document/yuque-sync");
        System.out.println();
        System.out.println("处理规则:");
        System.out.println("  1. 去除 HTML 内联标签（<font>、<u> 等），保留纯文本");
        System.out.println("  2. 在 ## 标题前添加 --- 分割线，支持分片检索");
        System.out.println("  3. 清洗多余空行和行尾空格");
    }

    /**
     * 遍历目录，处理所有 .md 文件
     */
    private static void processDirectory(Path inputDir, Path outputDir) throws IOException {
        Files.walkFileTree(inputDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toString().endsWith(".md")) {
                    processFile(file, resolveOutputPath(file, inputDir, outputDir));
                }
                return FileVisitResult.CONTINUE;
            }
        });
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
     * 处理单篇 Markdown 文件
     */
    private static void processFile(Path inputFile, Path outputFile) throws IOException {
        System.out.println("处理: " + inputFile.toAbsolutePath());

        String content = Files.readString(inputFile, StandardCharsets.UTF_8);
        String processed = processContent(content);

        // 创建目标目录并写入
        Files.createDirectories(outputFile.getParent());
        Files.writeString(outputFile, processed, StandardCharsets.UTF_8);

        long originalLines = content.lines().count();
        long processedLines = processed.lines().count();
        System.out.println("  → 输出: " + outputFile.toAbsolutePath()
                + " (" + originalLines + " 行 → " + processedLines + " 行)");
    }

    /**
     * 执行所有清洗和格式化规则
     */
    static String processContent(String content) {
        if (content == null || content.isBlank()) {
            return content;
        }

        // 1. 去除 HTML 内联标签
        String result = HTML_TAG_PATTERN.matcher(content).replaceAll("");

        // 2. 在 ## 标题前添加 --- 分割线
        //    注意：第一个标题前不加 ---
        result = addHorizontalRules(result);

        // 3. 压缩多余空行（超过 3 个换行的压缩为 2 个）
        result = EXCESS_BLANK_LINES.matcher(result).replaceAll("\n\n");

        // 4. 去除行尾空白
        result = TRAILING_WHITESPACE.matcher(result).replaceAll("");

        // 5. 去除文件首尾空行
        result = result.stripLeading() + "\n";

        return result;
    }

    /**
     * 在 ## 标题前添加 --- 分割线
     * 第一个 ## 不加，后续每个 ## 前加 ---
     */
    private static String addHorizontalRules(String content) {
        StringBuilder sb = new StringBuilder();
        String[] lines = content.split("\n", -1);
        boolean isFirstHeading = true;

        for (String line : lines) {
            if (line.startsWith("## ")) {
                if (!isFirstHeading) {
                    sb.append("---\n");
                }
                isFirstHeading = false;
            }
            sb.append(line).append("\n");
        }

        return sb.toString();
    }
}
