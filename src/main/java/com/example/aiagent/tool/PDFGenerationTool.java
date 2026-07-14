package com.example.aiagent.tool;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.example.aiagent.constant.FileConstant;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * PDF 文件生成工具
 * 为 AI Agent 提供 PDF 生成能力，支持多段落文本、标题、对齐方式等。
 * 生成的 PDF 文件保存在 {user.dir}/tmp/pdf/ 目录下。
 * 使用 iText 7 引擎，支持中文字体渲染。
 */
@Slf4j
public class PDFGenerationTool {

    /** PDF 文件保存根目录 */
    private static final String PDF_DIR = FileConstant.FILE_SAVE_DIR + "/pdf";

    /** 中文字体列表：{字体标识, 编码}，统一使用 2-param createFont */
    private static final String[][] CHINESE_FONTS = {
            // Windows 系统字体（TTC 用 ,0 指定集合中的第一个字体）
            {"C:/Windows/Fonts/msyh.ttc,0", "Identity-H"},           // 微软雅黑
            {"C:/Windows/Fonts/simsun.ttc,0", "Identity-H"},         // 宋体
            {"C:/Windows/Fonts/msyhbd.ttc,0", "Identity-H"},        // 微软雅黑加粗
            // font-asian 模块字体
            {"STSongStd-Light", "UniGB-UCS2-H"},
            {"STSong-Light", "UniGB-UCS2-H"},
    };

    /**
     * 安全解析文件路径，防止路径穿越
     */
    private String safeResolve(String fileName) {
        String normalized = Paths.get(PDF_DIR, fileName).normalize().toString();
        if (!normalized.startsWith(Paths.get(PDF_DIR).normalize().toString())) {
            throw new SecurityException("Path traversal is not allowed: " + fileName);
        }
        return normalized;
    }

    /**
     * 生成 PDF 文件
     * 将传入的文本内容生成为 PDF 文件。支持以下特性：
     * 
     *   - 多段落文本（按换行分隔）
     *   - 标题行自动识别（# 开头的行）
     *   - 中文字体渲染（自动回退）
     *   - 路径安全校验
     * 
     * @param fileName 保存的文件名（建议以 .pdf 结尾）
     * @param content  PDF 文本内容（多段文字用空行分隔）
     * @return 操作结果提示
     */
    @Tool(description = "Generate a PDF file with given content (supports Chinese text, multi-paragraph, # headings)")
    public String generatePDF(
            @ToolParam(description = "Name of the PDF file (e.g. report.pdf)") String fileName,
            @ToolParam(description = "Content to be included in the PDF (use blank lines to separate paragraphs, # for headings)") String content) {

        // 参数校验
        if (StrUtil.isBlank(fileName)) {
            return "Error: File name cannot be empty";
        }
        if (StrUtil.isBlank(content)) {
            return "Error: Content cannot be empty";
        }

        // 自动补全 .pdf 后缀
        if (!fileName.toLowerCase().endsWith(".pdf")) {
            fileName = fileName + ".pdf";
        }

        try {
            String filePath = safeResolve(fileName);

            // 确保目录存在
            FileUtil.mkdir(PDF_DIR);

            // 创建 PDF 文档
            try (PdfWriter writer = new PdfWriter(filePath);
                 PdfDocument pdf = new PdfDocument(writer);
                 Document document = new Document(pdf)) {

                // 设置中文字体（带自动回退）
                PdfFont font = loadChineseFont();
                document.setFont(font);

                // 调试日志：输出 content 的前 100 个字符，检查是否已经乱码
                String preview = content.length() > 100 ? content.substring(0, 100) + "..." : content;
                log.info("PDF content preview: {}", preview);

                // 按行分割内容，逐行处理
                String[] lines = content.split("\n");
                for (String line : lines) {
                    String trimmed = line.trim();

                    if (trimmed.isEmpty()) {
                        // 空行 → 添加一个空段落作为间距
                        document.add(new Paragraph("\n").setFontSize(8));
                        continue;
                    }

                    // 识别标题行（# 开头）
                    if (trimmed.startsWith("## ")) {
                        String titleText = trimmed.substring(3);
                        Paragraph heading = new Paragraph(titleText)
                                .setFontSize(16)
                                .setFontColor(ColorConstants.DARK_GRAY)
                                .setMarginBottom(8)
                                .setMarginTop(6);
                        document.add(heading);
                    } else if (trimmed.startsWith("# ")) {
                        String titleText = trimmed.substring(2);
                        Paragraph heading = new Paragraph(titleText)
                                .setFontSize(22)
                                .setFontColor(ColorConstants.BLUE)
                                .setTextAlignment(TextAlignment.CENTER)
                                .setMarginBottom(14)
                                .setMarginTop(10);
                        document.add(heading);
                    } else if (trimmed.startsWith("---") || trimmed.startsWith("***")) {
                        // 分隔线 → 留空行
                        document.add(new Paragraph("\n").setFontSize(6));
                    } else {
                        // 普通段落
                        Paragraph paragraph = new Paragraph(trimmed)
                                .setFontSize(12)
                                .setMarginBottom(8);
                        document.add(paragraph);
                    }
                }
            }

            return "PDF generated successfully: " + fileName + " (size: " + formatFileSize(new File(filePath).length()) + ")";

        } catch (SecurityException e) {
            return "Security error: " + e.getMessage();
        } catch (Exception e) {
            return "Error generating PDF: " + e.getMessage();
        }
    }

    /**
     * 列出 PDF 目录中的所有 PDF 文件
     * @return 格式化的 PDF 文件列表
     */
    @Tool(description = "List all PDF files in the PDF directory with size info")
    public String listPdfFiles() {
        try {
            File dir = new File(PDF_DIR);
            if (!dir.exists() || !dir.isDirectory()) {
                return "(no PDF files yet)";
            }
            File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".pdf"));
            if (files == null || files.length == 0) {
                return "(empty PDF directory)";
            }
            List<String> lines = new ArrayList<>();
            lines.add("PDF files (" + files.length + " total):");
            lines.add(String.format("%-5s %-12s %-20s %s", "TYPE", "SIZE", "LAST MODIFIED", "NAME"));
            lines.add("-".repeat(70));
            for (File f : files) {
                String size = formatFileSize(f.length());
                String modified = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                        .withZone(ZoneId.systemDefault())
                        .format(Instant.ofEpochMilli(f.lastModified()));
                lines.add(String.format("%-5s %-12s %-20s %s", "[PDF]", size, modified, f.getName()));
            }
            return String.join("\n", lines);
        } catch (Exception e) {
            return "Error listing PDF files: " + e.getMessage();
        }
    }

    /**
     * 删除 PDF 文件
     * @param fileName 要删除的 PDF 文件名
     * @return 操作结果提示
     */
    @Tool(description = "Delete a PDF file in the PDF directory")
    public String deletePdfFile(
            @ToolParam(description = "Name of the PDF file to delete (e.g. report.pdf)") String fileName) {
        if (StrUtil.isBlank(fileName)) {
            return "Error: File name cannot be empty";
        }
        // 自动补全 .pdf 后缀
        if (!fileName.toLowerCase().endsWith(".pdf")) {
            fileName = fileName + ".pdf";
        }
        try {
            String filePath = safeResolve(fileName);
            File file = new File(filePath);
            if (!file.exists()) {
                return "File not found: " + fileName;
            }
            if (!file.isFile()) {
                return "Not a file: " + fileName;
            }
            if (file.delete()) {
                return "PDF file deleted successfully: " + fileName;
            } else {
                return "Failed to delete PDF file: " + fileName;
            }
        } catch (SecurityException e) {
            return "Security error: " + e.getMessage();
        } catch (Exception e) {
            return "Error deleting PDF file: " + e.getMessage();
        }
    }

    /**
     * 加载中文字体（带自动回退）
     * 按优先级尝试加载中文字体。优先使用 iText 内置 Asian 字体，
     * 不可用则尝试 Windows 系统字体路径。所有字体都失败时回退到内置字体。
     * @return 可用的 PdfFont 实例
         */
        private PdfFont loadChineseFont() {
            for (String[] fontEntry : CHINESE_FONTS) {
                String fontName = fontEntry[0];
                String encoding = fontEntry[1];
                try {
                    PdfFont font = PdfFontFactory.createFont(fontName, encoding);
                    log.info("PDF font loaded: {}", fontName);
                    return font;
                } catch (Exception e) {
                    log.warn("PDF font load failed: {} - {}", fontName, e.getMessage());
                }
            }
            log.error("ALL Chinese fonts failed! PDF will not display Chinese correctly.");
            try {
                return PdfFontFactory.createFont();
            } catch (IOException e) {
                throw new RuntimeException("Failed to load any PDF font", e);
            }
        }

        /**
         * 格式化文件大小
         */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
}
