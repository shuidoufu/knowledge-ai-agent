package com.example.aiagent.tool;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.example.aiagent.constant.FileConstant;
import com.example.aiagent.service.ImageProxyService;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PDF 文件生成工具
 * 为 AI Agent 提供 PDF 生成能力，支持多段落文本、标题、对齐方式以及图片插入。
 * 图片通过 Markdown 图片语法（![alt](url)）识别，支持网络 URL、本地路径和 Base64 编码。
 * 生成的 PDF 文件保存在 {user.dir}/tmp/pdf/ 目录下。
 * 使用 iText 7 引擎，支持中文字体渲染。
 */
@Slf4j
public class PDFGenerationTool {

    /** 图片代理服务（携带完整浏览器请求头下载，绕过防盗链） */
    private final ImageProxyService imageProxyService;

    /**
     * 构造 PDF 生成工具
     *
     * @param imageProxyService 图片代理服务（下载网络图片）
     */
    public PDFGenerationTool(ImageProxyService imageProxyService) {
        this.imageProxyService = imageProxyService;
    }

    /** PDF 文件保存根目录 */
    private static final String PDF_DIR = FileConstant.FILE_SAVE_DIR + "/pdf";

    /** 临时目录，用于存放从网络下载的图片 */
    private static final String TEMP_DIR = FileConstant.FILE_SAVE_DIR + "/download";

    /** 匹配 Markdown 图片语法：![alt](src) */
    private static final Pattern IMAGE_PATTERN = Pattern.compile("!\\[([^]]*)\\]\\(([^)]+)\\)");

    /** PDF 页面可用宽度（A4 横向减去边距，约 500pt） */
    private static final float PAGE_WIDTH_PT = 500f;

    /** 图片在 PDF 中的最大高度（约 A4 高度的 60%） */
    private static final float IMAGE_MAX_HEIGHT_PT = 400f;

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
     * 安全解析临时文件路径
     */
    private String safeResolveTemp(String fileName) {
        String normalized = Paths.get(TEMP_DIR, fileName).normalize().toString();
        if (!normalized.startsWith(Paths.get(TEMP_DIR).normalize().toString())) {
            throw new SecurityException("Path traversal is not allowed: " + fileName);
        }
        return normalized;
    }

    /**
     * 生成 PDF 文件
     * 将传入的文本内容生成为 PDF 文件。支持以下特性：
     * 
     *   - 多段落文本（按换行分隔）
     *   - 标题行自动识别（# 和 ## 开头的行）
     *   - 图片插入（Markdown 图片语法：![alt](url)）
     *   - 图片支持网络 URL、本地路径和 Base64 编码
     *   - 中文字体渲染（自动回退）
     *   - 路径安全校验
     * 
     * 图片使用示例：
     *   ![风景图](https://example.com/landscape.jpg)
     *   ![本地图片](/path/to/image.png)
     * 
     * @param fileName 保存的文件名（建议以 .pdf 结尾）
     * @param content  PDF 文本内容（多段文字用空行分隔，支持 Markdown 图片语法）
     * @return 操作结果提示
     */
    @Tool(description = "Generate a PDF file with given content (supports Chinese text, multi-paragraph, # headings, and images via ![alt](url) syntax)")
    public String generatePDF(
            @ToolParam(description = "Name of the PDF file (e.g. report.pdf)") String fileName,
            @ToolParam(description = "Content to be included in the PDF (use blank lines to separate paragraphs, # for headings, ![alt](url) for images)") String content) {

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

        List<String> tempFiles = new ArrayList<>();

        try {
            String filePath = safeResolve(fileName);

            // 确保目录存在
            FileUtil.mkdir(PDF_DIR);
            FileUtil.mkdir(TEMP_DIR);

            // 创建 PDF 文档
            try (PdfWriter writer = new PdfWriter(filePath);
                 PdfDocument pdf = new PdfDocument(writer);
                 Document document = new Document(pdf)) {

                // 设置中文字体（带自动回退）
                PdfFont font = loadChineseFont();
                document.setFont(font);

                // 调试日志：输出 content 的前 100 个字符
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

                    // 检查是否为图片行（整行只有图片语法）
                    if (isImageOnlyLine(trimmed)) {
                        addImageToDocument(document, trimmed, tempFiles);
                        continue;
                    }

                    // 检查行中是否包含图片语法（文字和图片混合）
                    if (containsImageSyntax(trimmed)) {
                        processMixedContentLine(document, trimmed, font, tempFiles);
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

            // 清理 PDF 生成过程中下载的临时图片文件（仅删除下载的临时文件，不删 PDF）
            for (String tempFile : tempFiles) {
                try {
                    FileUtil.del(tempFile);
                } catch (Exception e) {
                    log.warn("Failed to delete temp image file: {}", tempFile, e);
                }
            }

            return "PDF generated successfully: " + fileName + " (size: " + formatFileSize(new File(filePath).length()) + ")"
                    + "\nDownload URL: /api/files/pdf/" + fileName;

        } catch (SecurityException e) {
            return "Security error: " + e.getMessage();
        } catch (Exception e) {
            return "Error generating PDF: " + e.getMessage();
        }
    }

    /**
     * 检查一行是否只包含图片语法
     */
    private boolean isImageOnlyLine(String line) {
        Matcher matcher = IMAGE_PATTERN.matcher(line);
        if (matcher.matches()) {
            return true;
        }
        // 也支持行前后有少量空白或描述文字但实质只有一张图片
        String stripped = line.replaceAll(IMAGE_PATTERN.pattern(), "").trim();
        return stripped.isEmpty() && IMAGE_PATTERN.matcher(line).find();
    }

    /**
     * 检查一行中是否包含图片语法
     */
    private boolean containsImageSyntax(String line) {
        return IMAGE_PATTERN.matcher(line).find();
    }

    /**
     * 处理纯图片行：将图片添加到文档
     */
    private void addImageToDocument(Document document, String line, List<String> tempFiles) {
        Matcher matcher = IMAGE_PATTERN.matcher(line);
        if (matcher.find()) {
            String altText = matcher.group(1);
            String src = matcher.group(2).trim();
            Image image = loadImage(src, tempFiles);
            if (image != null) {
                if (StrUtil.isNotBlank(altText)) {
                    // 添加图片标题
                    document.add(new Paragraph(altText)
                            .setFontSize(10)
                            .setFontColor(ColorConstants.GRAY)
                            .setTextAlignment(TextAlignment.CENTER)
                            .setMarginBottom(2));
                }
                document.add(image);
                // 图片后加间距
                document.add(new Paragraph("\n").setFontSize(6));
            }
        }
    }

    /**
     * 处理混合内容行（文字中嵌入图片，如 "文字 ![alt](src) 文字"）
     * 按图片语法分割，分段渲染文字和图片
     */
    private void processMixedContentLine(Document document, String line, PdfFont font, List<String> tempFiles) {
        Matcher matcher = IMAGE_PATTERN.matcher(line);
        int lastEnd = 0;
        boolean hasContent = false;

        while (matcher.find()) {
            // 图片前的文字
            String before = line.substring(lastEnd, matcher.start()).trim();
            if (StrUtil.isNotBlank(before)) {
                document.add(new Paragraph(before).setFontSize(12).setMarginBottom(8));
                hasContent = true;
            }

            // 图片
            String src = matcher.group(2).trim();
            Image image = loadImage(src, tempFiles);
            if (image != null) {
                document.add(image);
                hasContent = true;
            }

            lastEnd = matcher.end();
        }

        // 图片后的剩余文字
        String after = line.substring(lastEnd).trim();
        if (StrUtil.isNotBlank(after)) {
            document.add(new Paragraph(after).setFontSize(12).setMarginBottom(8));
            hasContent = true;
        }

        if (!hasContent) {
            // 兜底：没有任何内容被添加，把整行当普通文本
            document.add(new Paragraph(line).setFontSize(12).setMarginBottom(8));
        }
    }

    /**
     * 加载图片，支持多种来源：
     *   - http(s):// 网络 URL → 下载到临时文件
     *   - data:image/... Base64 → 直接解码
     *   - 本地文件路径 → 直接读取
     * 
     * @param src       图片来源
     * @param tempFiles 临时文件列表（用于后续清理）
     * @return iText Image 对象，加载失败返回 null
     */
    private Image loadImage(String src, List<String> tempFiles) {
        try {
            ImageData imageData = null;

            if (src.startsWith("http://") || src.startsWith("https://")) {
                // 网络 URL → 下载到临时文件
                imageData = loadImageFromUrl(src, tempFiles);
            } else if (src.startsWith("data:image/")) {
                // Base64 编码图片 → 直接解码
                imageData = loadImageFromBase64(src);
            } else if (src.startsWith("/api/image-proxy?url=")) {
                // 图片代理 URL → 提取原始 URL 后下载
                String realUrl = src.substring("/api/image-proxy?url=".length());
                // URL 可能经过编码，先做简单解码
                realUrl = java.net.URLDecoder.decode(realUrl, java.nio.charset.StandardCharsets.UTF_8);
                imageData = loadImageFromUrl(realUrl, tempFiles);
            } else if (src.startsWith("/api/files/")) {
                // 本地文件通过 HTTP 路径引用（如 /api/files/download/xxx）
                // 解析为本地文件系统路径
                String localPath = FileConstant.FILE_SAVE_DIR + src.substring("/api/files".length());
                imageData = loadImageFromLocal(localPath);
            } else {
                // 本地文件路径 → 直接读取
                imageData = loadImageFromLocal(src);
            }

            if (imageData == null) {
                log.warn("Failed to load image from: {}", src);
                return null;
            }

            Image image = new Image(imageData);

            // 过滤过小图片（防盗链占位图通常尺寸极小，如 1x1 像素）
            if (image.getImageWidth() < 50 || image.getImageHeight() < 50) {
                log.warn("图片尺寸过小，视为无效: {}x{} - {}", image.getImageWidth(), image.getImageHeight(),
                        StrUtil.maxLength(src, 80));
                return null;
            }

            // 安全缩放：确保图片能放入页面，scaleToFit 等比缩放
            image.scaleToFit(PAGE_WIDTH_PT - 20, 500f);

            // 居中显示
            image.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
            image.setMarginTop(6);
            image.setMarginBottom(6);

            return image;

        } catch (Exception e) {
            log.warn("Failed to load image: {} - {}", StrUtil.maxLength(src, 80), e.getMessage());
            return null;
        }
    }

    /**
     * 从网络 URL 加载图片
     * 通过 ImageProxyService 下载（携带完整浏览器请求头 + 多策略 Referer，绕过防盗链），
     * 如果图片为 WEBP 格式，自动转换为 PNG 后交给 iText
     */
    private ImageData loadImageFromUrl(String url, List<String> tempFiles) {
        ImageProxyService.ImageFetchResult result = imageProxyService.fetch(url);
        if (result == null) {
            log.warn("Failed to download image: {}", url);
            return null;
        }
        try {
            String fileName = extractFileNameFromUrl(url);
            String tempFilePath = safeResolveTemp(fileName);
            FileUtil.writeBytes(result.bytes(), tempFilePath);
            tempFiles.add(tempFilePath);

            if ("webp".equals(result.format())) {
                log.info("WEBP->PNG: {}", fileName);
                byte[] png = convertWebpToPng(result.bytes());
                if (png == null) return null;
                String p = tempFilePath + ".png";
                FileUtil.writeBytes(png, p);
                tempFiles.add(p);
                return ImageDataFactory.create(p);
            }

            log.info("Downloaded image: {} ({} bytes)", fileName, result.bytes().length);
            return ImageDataFactory.create(tempFilePath);
        } catch (Exception e) {
            log.warn("loadImageFromUrl failed: {} - {}", url, e.getMessage());
            return null;
        }
    }

    /**
     * 将 WEBP 图片转换为 PNG（通过 ImageIO + TwelveMonkeys WebP 插件）
     */
    private byte[] convertWebpToPng(byte[] webpBytes) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(webpBytes));
            if (image == null) {
                log.warn("ImageIO failed to decode WEBP bytes");
                return null;
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.warn("WEBP to PNG conversion failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从 Base64 编码加载图片
     */
    private ImageData loadImageFromBase64(String dataUri) {
        try {
            // data:image/png;base64,AAAA...
            int commaIndex = dataUri.indexOf(',');
            if (commaIndex < 0) {
                return null;
            }
            String base64 = dataUri.substring(commaIndex + 1).trim();
            byte[] imageBytes = Base64.getDecoder().decode(base64);
            return ImageDataFactory.create(imageBytes);
        } catch (Exception e) {
            log.warn("Failed to decode base64 image", e);
            return null;
        }
    }

    /**
     * 从本地文件路径加载图片
     */
    private ImageData loadImageFromLocal(String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                log.warn("Local image file not found: {}", filePath);
                return null;
            }
            return ImageDataFactory.create(filePath);
        } catch (Exception e) {
            log.warn("Failed to load local image: {}", filePath, e);
            return null;
        }
    }

    /**
     * 从 URL 中提取文件名
     */
    private String extractFileNameFromUrl(String url) {
        // 去掉查询参数
        String cleanUrl = url.contains("?") ? url.substring(0, url.indexOf('?')) : url;
        // 取最后一段作为文件名
        String fileName = cleanUrl.substring(cleanUrl.lastIndexOf('/') + 1);
        if (StrUtil.isBlank(fileName) || !fileName.contains(".")) {
            fileName = "img_" + System.currentTimeMillis() + ".jpg";
        }
        return fileName;
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
