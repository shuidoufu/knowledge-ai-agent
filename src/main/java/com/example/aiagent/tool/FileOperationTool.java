package com.example.aiagent.tool;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.example.aiagent.constant.FileConstant;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 文件操作工具类
 * 为 AI Agent 提供安全的文件系统操作能力。
 * 所有文件操作都被限定在 {user.dir}/tmp/file/ 目录下，防止路径穿越攻击。
 * 支持的操作：读取、写入、追加、删除、复制、移动、列表、信息查询、全文搜索、创建目录。
 */
public class FileOperationTool {

    /** 文件操作根目录，所有文件操作限定在此目录下 */
    private final String FILE_DIR = FileConstant.FILE_SAVE_DIR + "/file";

    /**
     * 安全解析文件路径
     * 将用户输入的文件名解析为绝对路径，同时校验最终路径是否在允许的根目录下。
     * 防止通过 "../" 等相对路径进行路径穿越攻击。
     * @param fileName 用户传入的文件名（可能包含子目录路径）
     * @return 规范化后的绝对路径
     * @throws SecurityException 如果解析后的路径超出了允许的根目录
     */
    private String safeResolve(String fileName) {
        // 将用户输入与根目录拼接后规范化，消除 "../" 等相对路径
        String normalized = Paths.get(FILE_DIR, fileName).normalize().toString();
        // 校验规范化后的路径必须以根目录开头，防止路径穿越
        if (!normalized.startsWith(Paths.get(FILE_DIR).normalize().toString())) {
            throw new SecurityException("Path traversal is not allowed: " + fileName);
        }
        return normalized;
    }

    /**
     * 读取文件内容
     * @param fileName 文件名（可包含子目录路径）
     * @return 文件文本内容，或错误提示信息
     */
    @Tool(description = "Read content from a file")
    public String readFile(@ToolParam(description = "Name of the file to read") String fileName) {
        try {
            String filePath = safeResolve(fileName);
            File file = new File(filePath);
            if (!file.exists()) {
                return "File not found: " + fileName;
            }
            if (!file.isFile()) {
                return "Not a file: " + fileName;
            }
            return FileUtil.readUtf8String(filePath);
        } catch (SecurityException e) {
            return "Security error: " + e.getMessage();
        } catch (Exception e) {
            return "Error reading file: " + e.getMessage();
        }
    }

    /**
     * 写入文件（覆盖写入）
     * 如果文件不存在则创建，如果已存在则覆盖。会自动创建不存在的父目录。
     * @param fileName 文件名（可包含子目录路径）
     * @param content  要写入的文本内容
     * @return 操作结果提示
     */
    @Tool(description = "Write content to a file (creates or overwrites)")
    public String writeFile(
            @ToolParam(description = "Name of the file to write") String fileName,
            @ToolParam(description = "Content to write to the file") String content) {
        try {
            String filePath = safeResolve(fileName);
            // 确保根目录存在
            FileUtil.mkdir(FILE_DIR);
            // 确保子目录存在
            File parentDir = new File(filePath).getParentFile();
            if (parentDir != null) {
                FileUtil.mkdir(parentDir.getAbsolutePath());
            }
            FileUtil.writeUtf8String(content, filePath);
            return "File written successfully: " + fileName;
        } catch (SecurityException e) {
            return "Security error: " + e.getMessage();
        } catch (Exception e) {
            return "Error writing file: " + e.getMessage();
        }
    }

    /**
     * 追加内容到文件末尾
     * 在已有文件末尾追加新内容，不覆盖原有内容。
     * @param fileName 文件名
     * @param content  要追加的内容
     * @return 操作结果提示
     */
    @Tool(description = "Append content to the end of an existing file")
    public String appendToFile(
            @ToolParam(description = "Name of the file to append to") String fileName,
            @ToolParam(description = "Content to append") String content) {
        try {
            String filePath = safeResolve(fileName);
            File file = new File(filePath);
            if (!file.exists()) {
                return "File not found: " + fileName;
            }
            if (!file.isFile()) {
                return "Not a file: " + fileName;
            }
            // Hutool 的 appendUtf8String 会在文件末尾追加内容
            FileUtil.appendUtf8String(content, filePath);
            return "Content appended successfully to: " + fileName;
        } catch (SecurityException e) {
            return "Security error: " + e.getMessage();
        } catch (Exception e) {
            return "Error appending to file: " + e.getMessage();
        }
    }

    /**
     * 删除文件
     * @param fileName 要删除的文件名
     * @return 操作结果提示
     */
    @Tool(description = "Delete a file")
    public String deleteFile(@ToolParam(description = "Name of the file to delete") String fileName) {
        try {
            String filePath = safeResolve(fileName);
            File file = new File(filePath);
            if (!file.exists()) {
                return "File not found: " + fileName;
            }
            if (!file.isFile()) {
                return "Not a file: " + fileName + " (use deleteDirectory for directories)";
            }
            if (file.delete()) {
                return "File deleted successfully: " + fileName;
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
     * 复制文件
     * 将源文件复制到目标位置。目标文件已存在时会被覆盖。
     * 目标路径可以是新的文件名或新的子目录位置。
     * @param sourceFileName 源文件名
     * @param destFileName   目标文件名
     * @return 操作结果提示
     */
    @Tool(description = "Copy a file to a new location (within the same base directory)")
    public String copyFile(
            @ToolParam(description = "Source file name") String sourceFileName,
            @ToolParam(description = "Destination file name") String destFileName) {
        try {
            String sourcePath = safeResolve(sourceFileName);
            String destPath = safeResolve(destFileName);
            File srcFile = new File(sourcePath);
            if (!srcFile.exists()) {
                return "Source file not found: " + sourceFileName;
            }
            if (!srcFile.isFile()) {
                return "Source is not a file: " + sourceFileName;
            }
            // 确保目标目录存在
            File destParent = new File(destPath).getParentFile();
            if (destParent != null) {
                FileUtil.mkdir(destParent.getAbsolutePath());
            }
            // 使用 REPLACE_EXISTING 允许覆盖已存在的目标文件
            Files.copy(srcFile.toPath(), new File(destPath).toPath(), StandardCopyOption.REPLACE_EXISTING);
            return "File copied successfully from " + sourceFileName + " to " + destFileName;
        } catch (SecurityException e) {
            return "Security error: " + e.getMessage();
        } catch (Exception e) {
            return "Error copying file: " + e.getMessage();
        }
    }

    /**
     * 移动或重命名文件
     * 将源文件移动到新位置，也可用于重命名文件。
     * 目标路径可以是新文件名或新子目录位置。
     * @param sourceFileName 当前文件名
     * @param destFileName   新文件名
     * @return 操作结果提示
     */
    @Tool(description = "Move or rename a file")
    public String moveFile(
            @ToolParam(description = "Current file name") String sourceFileName,
            @ToolParam(description = "New file name") String destFileName) {
        try {
            String sourcePath = safeResolve(sourceFileName);
            String destPath = safeResolve(destFileName);
            File srcFile = new File(sourcePath);
            if (!srcFile.exists()) {
                return "Source file not found: " + sourceFileName;
            }
            if (!srcFile.isFile()) {
                return "Source is not a file: " + sourceFileName;
            }
            // 确保目标目录存在
            File destParent = new File(destPath).getParentFile();
            if (destParent != null) {
                FileUtil.mkdir(destParent.getAbsolutePath());
            }
            Files.move(srcFile.toPath(), new File(destPath).toPath(), StandardCopyOption.REPLACE_EXISTING);
            return "File moved/renamed successfully from " + sourceFileName + " to " + destFileName;
        } catch (SecurityException e) {
            return "Security error: " + e.getMessage();
        } catch (Exception e) {
            return "Error moving file: " + e.getMessage();
        }
    }

    /**
     * 列出目录内容
     * 以表格形式列出指定目录下的所有文件和子目录，包含类型、大小、最后修改时间等信息。
     * @param subDir 子目录名（传入空字符串或 null 则列出根目录）
     * @return 格式化的目录内容列表
     */
    @Tool(description = "List all files in the directory with size and last modified time")
    public String listFiles(@ToolParam(description = "Subdirectory name (optional, pass empty string for root)") String subDir) {
        try {
            String dirPath;
            if (StrUtil.isBlank(subDir)) {
                dirPath = FILE_DIR;
            } else {
                dirPath = safeResolve(subDir);
            }
            File dir = new File(dirPath);
            if (!dir.exists()) {
                return "Directory not found: " + (StrUtil.isBlank(subDir) ? "(root)" : subDir);
            }
            if (!dir.isDirectory()) {
                return "Not a directory: " + (StrUtil.isBlank(subDir) ? "(root)" : subDir);
            }
            File[] files = dir.listFiles();
            if (files == null || files.length == 0) {
                return "(empty directory)";
            }
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault());
            List<String> lines = new ArrayList<>();
            lines.add("Contents of " + dir.getAbsolutePath() + ":");
            // 表头：类型、大小、修改时间、名称
            lines.add(String.format("%-5s %-12s %-20s %s", "TYPE", "SIZE", "LAST MODIFIED", "NAME"));
            lines.add("-".repeat(80));
            for (File f : files) {
                String type = f.isDirectory() ? "[DIR]" : "[FILE]";
                String size = f.isFile() ? formatFileSize(f.length()) : "-";
                String modified = dtf.format(Instant.ofEpochMilli(f.lastModified()));
                lines.add(String.format("%-5s %-12s %-20s %s", type, size, modified, f.getName()));
            }
            return String.join("\n", lines);
        } catch (SecurityException e) {
            return "Security error: " + e.getMessage();
        } catch (Exception e) {
            return "Error listing files: " + e.getMessage();
        }
    }

    /**
     * 获取文件详细信息
     * 返回文件的完整信息：路径、类型、大小、最后修改时间、读写权限、文件扩展名等。
     * @param fileName 文件名
     * @return 格式化的文件信息
     */
    @Tool(description = "Get detailed information about a file")
    public String getFileInfo(@ToolParam(description = "Name of the file") String fileName) {
        try {
            String filePath = safeResolve(fileName);
            File file = new File(filePath);
            if (!file.exists()) {
                return "File not found: " + fileName;
            }
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault());
            List<String> info = new ArrayList<>();
            info.add("File: " + fileName);
            info.add("Full path: " + file.getAbsolutePath());
            info.add("Type: " + (file.isDirectory() ? "Directory" : "File"));
            info.add("Size: " + (file.isFile() ? formatFileSize(file.length()) : "-"));
            info.add("Last modified: " + dtf.format(Instant.ofEpochMilli(file.lastModified())));
            info.add("Readable: " + file.canRead());
            info.add("Writable: " + file.canWrite());
            if (file.isFile()) {
                // 提取文件扩展名
                String ext = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf('.') + 1) : "(none)";
                info.add("Extension: " + ext);
            }
            return String.join("\n", info);
        } catch (SecurityException e) {
            return "Security error: " + e.getMessage();
        } catch (Exception e) {
            return "Error getting file info: " + e.getMessage();
        }
    }

    /**
     * 在文件中搜索文本
     * 遍历工作目录下的所有文件，查找包含指定关键词的文件。
     * 仅搜索 UTF-8 编码的文本文件，会跳过非文本文件导致的读取异常。
     * @param keyword 要搜索的关键词
     * @return 匹配的文件列表，包含相对路径和文件大小
     */
    @Tool(description = "Search for text content across all files in the directory (supports simple text search)")
    public String searchInFiles(@ToolParam(description = "Text to search for") String keyword) {
        if (StrUtil.isBlank(keyword)) {
            return "Search keyword cannot be empty";
        }
        File dir = new File(FILE_DIR);
        if (!dir.exists() || !dir.isDirectory()) {
            return "File directory does not exist";
        }
        List<String> results = new ArrayList<>();
        try (Stream<Path> pathStream = Files.walk(dir.toPath())) {
            // 遍历所有文件，过滤出包含关键词的文件
            List<Path> matchedFiles = pathStream
                    .filter(Files::isRegularFile)
                    .filter(p -> {
                        try {
                            String content = Files.readString(p, StandardCharsets.UTF_8);
                            return content.contains(keyword);
                        } catch (Exception e) {
                            // 无法读取（如二进制文件）则跳过
                            return false;
                        }
                    })
                    .collect(Collectors.toList());

            if (matchedFiles.isEmpty()) {
                return "No files found containing: " + keyword;
            }
            results.add("Found " + matchedFiles.size() + " file(s) containing \"" + keyword + "\":");
            results.add("");
            for (Path p : matchedFiles) {
                // 计算相对于工作目录的路径，便于用户识别
                String relativePath = dir.toPath().relativize(p).toString();
                long size = Files.size(p);
                results.add("  " + relativePath + " (" + formatFileSize(size) + ")");
            }
            return String.join("\n", results);
        } catch (Exception e) {
            return "Error searching files: " + e.getMessage();
        }
    }

    /**
     * 创建目录
     * 在工作目录下创建新的子目录。支持创建多级目录（如 "a/b/c"）。
     * @param dirName 目录名（可包含多级路径）
     * @return 操作结果提示
     */
    @Tool(description = "Create a new directory under the file workspace")
    public String createDirectory(@ToolParam(description = "Name of the directory to create") String dirName) {
        try {
            String dirPath = safeResolve(dirName);
            File dir = new File(dirPath);
            if (dir.exists()) {
                return "Directory already exists: " + dirName;
            }
            // Hutool 的 mkdir 支持创建多级目录
            FileUtil.mkdir(dirPath);
            return "Directory created successfully: " + dirName;
        } catch (SecurityException e) {
            return "Security error: " + e.getMessage();
        } catch (Exception e) {
            return "Error creating directory: " + e.getMessage();
        }
    }

    /**
     * 格式化文件大小为人类可读的字符串
     * 自动选择合适的单位（B、KB、MB、GB），保留一位小数。
     * @param bytes 文件字节数
     * @return 格式化后的字符串，如 "1.5 MB"
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
}
