package com.example.aiagent.tool;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.example.aiagent.constant.FileConstant;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.File;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 资源下载工具
 * 为 AI Agent 提供网络资源下载能力，支持从 URL 下载文件到本地。
 * 下载的文件保存在 {user.dir}/tmp/download/ 目录下。
 * 包含路径安全校验、URL 校验、文件名自动推断等功能。
 */
public class ResourceDownloadTool {

    /** 下载文件保存根目录 */
    private static final String DOWNLOAD_DIR = FileConstant.FILE_SAVE_DIR + "/download";

    /**
     * 安全解析文件路径，防止路径穿越
     */
    private String safeResolve(String fileName) {
        String normalized = Paths.get(DOWNLOAD_DIR, fileName).normalize().toString();
        if (!normalized.startsWith(Paths.get(DOWNLOAD_DIR).normalize().toString())) {
            throw new SecurityException("Path traversal is not allowed: " + fileName);
        }
        return normalized;
    }

    /**
     * 从 URL 中提取文件名
     */
    private String extractFileNameFromUrl(String url) {
        // 去掉查询参数
        String cleanUrl = url.contains("?") ? url.substring(0, url.indexOf('?')) : url;
        // 取最后一段作为文件名
        String fileName = cleanUrl.substring(cleanUrl.lastIndexOf('/') + 1);
        if (StrUtil.isBlank(fileName)) {
            fileName = "download_" + System.currentTimeMillis();
        }
        return fileName;
    }

    /**
     * 校验 URL 格式是否合法
     */
    private String validateUrl(String url) {
        if (StrUtil.isBlank(url)) {
            return "URL cannot be empty";
        }
        url = url.trim();
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return "Invalid URL: must start with http:// or https://";
        }
        return null; // 校验通过
    }

    /**
     * 从网络 URL 下载资源到本地
     * 支持 HTTP/HTTPS 协议的资源下载。如果不提供文件名，会自动从 URL 推断。
     * @param url      资源的网络 URL
     * @param fileName 保存的文件名（可选，不提供则从 URL 自动提取）
     * @return 下载结果提示（含文件路径和大小）
     */
    @Tool(description = "Download a resource from a URL and save to local file. Auto-detects filename from URL if not provided.")
    public String downloadResource(
            @ToolParam(description = "URL of the resource to download (http/https)") String url,
            @ToolParam(description = "Name to save the file as (optional, will auto-detect from URL if empty)") String fileName) {

        // URL 校验
        String urlError = validateUrl(url);
        if (urlError != null) {
            return "Error: " + urlError;
        }

        // 自动推断文件名
        if (StrUtil.isBlank(fileName)) {
            fileName = extractFileNameFromUrl(url);
        }

        try {
            String filePath = safeResolve(fileName);
            FileUtil.mkdir(DOWNLOAD_DIR);

            // 执行下载
            HttpUtil.downloadFile(url, new File(filePath));

            File savedFile = new File(filePath);
            long fileSize = savedFile.length();

            return "Resource downloaded successfully: " + fileName
                    + " (size: " + formatFileSize(fileSize) + ")"
                    + "\nPath: " + filePath;

        } catch (SecurityException e) {
            return "Security error: " + e.getMessage();
        } catch (Exception e) {
            return "Error downloading resource: " + e.getMessage();
        }
    }

    /**
     * 列出已下载的文件
     * @return 格式化的下载文件列表
     */
    @Tool(description = "List all downloaded files with size and time info")
    public String listDownloads() {
        try {
            File dir = new File(DOWNLOAD_DIR);
            if (!dir.exists() || !dir.isDirectory()) {
                return "(no downloaded files yet)";
            }
            File[] files = dir.listFiles();
            if (files == null || files.length == 0) {
                return "(empty download directory)";
            }
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault());
            List<String> lines = new ArrayList<>();
            lines.add("Downloaded files (" + files.length + " total):");
            lines.add(String.format("%-12s %-20s %s", "SIZE", "LAST MODIFIED", "NAME"));
            lines.add("-".repeat(65));
            for (File f : files) {
                String size = f.isFile() ? formatFileSize(f.length()) : "-";
                String modified = dtf.format(Instant.ofEpochMilli(f.lastModified()));
                lines.add(String.format("%-12s %-20s %s", size, modified, f.getName()));
            }
            return String.join("\n", lines);
        } catch (Exception e) {
            return "Error listing downloads: " + e.getMessage();
        }
    }

    /**
     * 删除已下载的文件
     * @param fileName 要删除的文件名
     * @return 操作结果提示
     */
    @Tool(description = "Delete a downloaded file")
    public String deleteDownload(
            @ToolParam(description = "Name of the downloaded file to delete") String fileName) {
        if (StrUtil.isBlank(fileName)) {
            return "Error: File name cannot be empty";
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
                return "Downloaded file deleted successfully: " + fileName;
            } else {
                return "Failed to delete file: " + fileName;
            }
        } catch (SecurityException e) {
            return "Security error: " + e.getMessage();
        } catch (Exception e) {
            return "Error deleting file: " + e.getMessage();
        }
    }

    /**
     * 格式化文件大小
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
}
