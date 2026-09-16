package com.example.aiagent.service;

import com.example.aiagent.model.DocumentStatus;
import com.example.aiagent.model.KnowledgeDocumentDTO;
import com.example.aiagent.model.KnowledgeDocumentDetailDTO;
import com.example.aiagent.model.KnowledgeDocumentPageDTO;
import com.example.aiagent.rag.DocumentPreprocessor;
import com.example.aiagent.rag.KnowledgeAppDocumentLoader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * 知识库文档服务
 * 提供文档的上传、删除、列表、内容查看与重新入库，上传与入库由后台单线程串行执行：
 * 预处理 → 落盘 → 分块 → 向量化；进行中与失败的阶段记录在内存任务表中，成功后移除
 */
@Slf4j
@Service
public class KnowledgeDocumentService {

    /** 切片元数据中的文件名键 */
    private static final String METADATA_FILENAME = "filename";

    /** 语雀同步子目录名 */
    private static final String YUQUE_SYNC_DIR = "yuque-sync";

    /** Markdown 后缀 */
    private static final String MARKDOWN_SUFFIX = ".md";

    /** 单个文档大小上限（与 multipart 配置一致） */
    private static final long MAX_UPLOAD_BYTES = 5L * 1024 * 1024;

    /** 文件名长度上限 */
    private static final int MAX_FILENAME_LENGTH = 120;

    /** 每页条数上限 */
    private static final int MAX_PAGE_SIZE = 50;

    private final KnowledgeAppDocumentLoader documentLoader;
    private final VectorStore vectorStore;
    private final MongoTemplate mongoTemplate;
    private final UserService userService;
    private final ThreadPoolTaskExecutor knowledgeDocumentExecutor;

    @Value("${spring.ai.vectorstore.mongodb.collection-name:vector_store}")
    private String collectionName;

    /** 任务表：仅保留进行中与失败的文档（成功后的终态由磁盘文件与向量切片数推导） */
    private final Map<String, DocumentTask> tasks = new ConcurrentHashMap<>();

    public KnowledgeDocumentService(KnowledgeAppDocumentLoader documentLoader,
                                    @Qualifier("knowledgeVectorStore") VectorStore vectorStore,
                                    MongoTemplate mongoTemplate,
                                    UserService userService,
                                    @Qualifier("knowledgeDocumentExecutor")
                                    ThreadPoolTaskExecutor knowledgeDocumentExecutor) {
        this.documentLoader = documentLoader;
        this.vectorStore = vectorStore;
        this.mongoTemplate = mongoTemplate;
        this.userService = userService;
        this.knowledgeDocumentExecutor = knowledgeDocumentExecutor;
    }

    /**
     * 分页查询知识库文档（按关键字模糊匹配文件名与显示名）
     *
     * @param username 当前登录用户名
     * @param keyword  搜索关键字，可为空
     * @param page     页码，从 1 开始
     * @param size     每页条数
     * @param sort     排序方式：time_desc（默认）｜time_asc｜name_asc
     * @return 分页结果
     */
    public KnowledgeDocumentPageDTO listDocuments(String username, String keyword, int page, int size, String sort) {
        requireAdmin(username);
        List<DocumentFile> files = listDocumentFiles();
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        List<DocumentFile> matched = new ArrayList<>();
        for (DocumentFile file : files) {
            if (normalizedKeyword.isEmpty()
                    || file.filename().toLowerCase(Locale.ROOT).contains(normalizedKeyword)
                    || file.name().toLowerCase(Locale.ROOT).contains(normalizedKeyword)) {
                matched.add(file);
            }
        }
        matched.sort(resolveComparator(sort));

        int total = matched.size();
        int pageSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        int pageIndex = Math.max(1, page);
        int totalPages = total == 0 ? 0 : (total + pageSize - 1) / pageSize;
        int fromIndex = Math.min((pageIndex - 1) * pageSize, total);
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<DocumentFile> pageFiles = matched.subList(fromIndex, toIndex);

        List<String> pageFilenames = pageFiles.stream().map(DocumentFile::filename).toList();
        Map<String, Integer> chunkCounts = countChunks(pageFilenames);

        List<KnowledgeDocumentDTO> items = new ArrayList<>(pageFiles.size());
        for (DocumentFile file : pageFiles) {
            int chunkCount = chunkCounts.getOrDefault(file.filename(), 0);
            DocumentTask task = tasks.get(file.filename());
            items.add(new KnowledgeDocumentDTO(file.filename(), file.name(), file.size(), file.updatedAt(),
                    chunkCount, resolveStatus(task, chunkCount), resolveErrorMessage(task)));
        }
        return new KnowledgeDocumentPageDTO(total, pageIndex, pageSize, totalPages, items);
    }

    /**
     * 查询单个文档的当前内容与分块结果
     *
     * @param username 当前登录用户名
     * @param filename 文件名
     * @return 文档详情
     */
    public KnowledgeDocumentDetailDTO getDocumentDetail(String username, String filename) {
        requireAdmin(username);
        String safeFilename = sanitizeFilename(filename);
        Path target = resolveDocumentFile(safeFilename);
        if (!Files.isRegularFile(target)) {
            throw new IllegalArgumentException("文档「" + safeFilename + "」不存在或已被删除");
        }
        String content;
        long size;
        LocalDateTime updatedAt;
        try {
            content = Files.readString(target, StandardCharsets.UTF_8);
            size = Files.size(target);
            updatedAt = toLocalDateTime(Files.getLastModifiedTime(target).toInstant());
        } catch (IOException e) {
            throw new IllegalArgumentException("文档读取失败：" + e.getMessage());
        }

        List<Document> parsedChunks = documentLoader.parseMarkdown(new FileSystemResource(target));
        List<KnowledgeDocumentDetailDTO.Chunk> chunks = new ArrayList<>(parsedChunks.size());
        for (int i = 0; i < parsedChunks.size(); i++) {
            Document chunk = parsedChunks.get(i);
            Object title = chunk.getMetadata() == null ? null : chunk.getMetadata().get("title");
            chunks.add(new KnowledgeDocumentDetailDTO.Chunk(i + 1,
                    title == null ? "" : String.valueOf(title), chunk.getText(), chunk.getText().length()));
        }

        int chunkCount = countChunks(List.of(safeFilename)).getOrDefault(safeFilename, 0);
        DocumentTask task = tasks.get(safeFilename);
        return new KnowledgeDocumentDetailDTO(safeFilename, KnowledgeAppDocumentLoader.extractTopic(safeFilename),
                size, updatedAt, resolveStatus(task, chunkCount), resolveErrorMessage(task), chunkCount, content,
                chunks);
    }

    /**
     * 上传知识库文档：校验通过后先落盘原始内容，预处理与向量化在后台执行
     *
     * @param username 当前登录用户名
     * @param file     上传的 Markdown 文件
     * @return 受理结果
     */
    public Map<String, Object> uploadDocument(String username, MultipartFile file) {
        requireAdmin(username);
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择要上传的 Markdown 文档");
        }
        String filename = sanitizeFilename(file.getOriginalFilename());
        String content = decodeMarkdownContent(readBytes(file));
        if (isRunning(filename)) {
            throw new IllegalArgumentException("文档「" + filename + "」正在处理中，请稍后再试");
        }
        boolean duplicated = listDocumentFiles().stream()
                .anyMatch(item -> item.filename().equalsIgnoreCase(filename));
        if (duplicated) {
            throw new IllegalArgumentException("文档「" + filename + "」已存在，如需更新请先删除后再上传");
        }
        writeAtomically(resolveDocumentFile(filename), content);
        submitTask(filename);
        log.info("知识库文档已受理: filename={}, chars={}", filename, content.length());
        return Map.of("filename", filename,
                "message", "已开始处理文档「" + filename + "」，预处理与向量化将在后台完成");
    }

    /**
     * 重新入库：删除该文档已入库的切片后重跑预处理、分块与向量化
     *
     * @param username 当前登录用户名
     * @param filename 文件名
     * @return 受理结果
     */
    public Map<String, Object> reindexDocument(String username, String filename) {
        requireAdmin(username);
        String safeFilename = sanitizeFilename(filename);
        if (!Files.isRegularFile(resolveDocumentFile(safeFilename))) {
            throw new IllegalArgumentException("文档「" + safeFilename + "」不存在或已被删除");
        }
        requireNotRunning(safeFilename);
        submitTask(safeFilename);
        log.info("知识库文档重新入库已受理: filename={}", safeFilename);
        return Map.of("filename", safeFilename, "message", "已开始重新入库文档「" + safeFilename + "」");
    }

    /**
     * 删除文档：先清理该文档的向量切片，再删除磁盘文件
     *
     * @param username 当前登录用户名
     * @param filename 文件名
     * @return 删除结果
     */
    public Map<String, Object> deleteDocument(String username, String filename) {
        requireAdmin(username);
        String safeFilename = sanitizeFilename(filename);
        requireNotRunning(safeFilename);
        Path target = resolveDocumentFile(safeFilename);
        if (!Files.isRegularFile(target)) {
            // 文件已被外部删除时清理遗留的任务记录，避免该文件名无法再次上传
            tasks.remove(safeFilename);
            throw new IllegalArgumentException("文档「" + safeFilename + "」不存在或已被删除");
        }
        // 先删向量再删文件：若文件先删除成功而向量清理失败，会残留无法再清理的孤儿切片
        int removedChunks = countChunks(List.of(safeFilename)).getOrDefault(safeFilename, 0);
        vectorStore.delete(filenameFilter(safeFilename));
        deleteFile(target, safeFilename);
        tasks.remove(safeFilename);
        log.info("知识库文档已删除: filename={}, 清理切片={}", safeFilename, removedChunks);
        return Map.of("filename", safeFilename,
                "message", "已删除文档「" + safeFilename + "」，同时清理 " + removedChunks + " 个向量切片");
    }

    /**
     * 批量删除文档：校验全部目标后再逐个清理向量与文件
     *
     * @param username  当前登录用户名
     * @param filenames 文件名列表
     * @return 删除结果
     */
    public Map<String, Object> batchDeleteDocuments(String username, List<String> filenames) {
        requireAdmin(username);
        if (filenames == null || filenames.isEmpty()) {
            throw new IllegalArgumentException("请选择要删除的文档");
        }
        List<String> safeFilenames = new ArrayList<>();
        for (String filename : filenames) {
            String safeFilename = sanitizeFilename(filename);
            if (safeFilenames.contains(safeFilename)) {
                continue;
            }
            requireNotRunning(safeFilename);
            if (!Files.isRegularFile(resolveDocumentFile(safeFilename))) {
                tasks.remove(safeFilename);
                throw new IllegalArgumentException("文档「" + safeFilename + "」不存在或已被删除，请刷新后重试");
            }
            safeFilenames.add(safeFilename);
        }
        long removedChunks = 0;
        for (String safeFilename : safeFilenames) {
            removedChunks += countChunks(List.of(safeFilename)).getOrDefault(safeFilename, 0);
            vectorStore.delete(filenameFilter(safeFilename));
            deleteFile(resolveDocumentFile(safeFilename), safeFilename);
            tasks.remove(safeFilename);
        }
        log.info("知识库文档批量删除完成: count={}, 清理切片={}", safeFilenames.size(), removedChunks);
        return Map.of("deleted", safeFilenames.size(),
                "message", "已删除 " + safeFilenames.size() + " 个文档，同时清理 " + removedChunks + " 个向量切片");
    }

    /**
     * 后台执行文档流水线：预处理（原地覆盖） → 分块 → 清理旧切片 → 向量化
     */
    private void runPipeline(String filename) {
        DocumentStatus stage = DocumentStatus.PREPROCESSING;
        try {
            Path target = resolveDocumentFile(filename);
            tasks.put(filename, DocumentTask.preprocessing());
            String raw = Files.readString(target, StandardCharsets.UTF_8);
            String processed = DocumentPreprocessor.processContent(raw);
            if (processed == null || processed.isBlank()) {
                throw new IllegalStateException("预处理后内容为空");
            }
            writeAtomically(target, processed);

            stage = DocumentStatus.VECTORIZING;
            tasks.put(filename, DocumentTask.vectorizing());
            List<Document> chunks = documentLoader.parseMarkdown(new FileSystemResource(target));
            if (chunks.isEmpty()) {
                throw new IllegalStateException("文档未解析出有效的内容切片");
            }
            // 清理该文档的旧切片，避免重新入库后残留孤儿切片
            vectorStore.delete(filenameFilter(filename));
            vectorStore.add(withStableIds(filename, chunks));
            tasks.remove(filename);
            log.info("知识库文档处理完成: filename={}, chunks={}", filename, chunks.size());
        } catch (Exception e) {
            String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            DocumentStatus failedStatus = stage == DocumentStatus.VECTORIZING
                    ? DocumentStatus.VECTORIZE_FAILED
                    : DocumentStatus.PREPROCESS_FAILED;
            tasks.put(filename, DocumentTask.failed(failedStatus, message));
            log.error("知识库文档处理失败: filename={}, status={}, error={}", filename, failedStatus, message, e);
        }
    }

    /**
     * 提交后台任务；同一文档已在处理中时拒绝，避免重复排队
     */
    private void submitTask(String filename) {
        tasks.compute(filename, (key, existing) -> {
            if (existing != null && existing.running()) {
                throw new IllegalArgumentException("文档「" + filename + "」正在处理中，请稍后再试");
            }
            return DocumentTask.preprocessing();
        });
        try {
            knowledgeDocumentExecutor.execute(() -> runPipeline(filename));
        } catch (TaskRejectedException e) {
            tasks.remove(filename);
            throw new IllegalArgumentException("当前有较多文档正在处理，请稍后再试；文档「" + filename
                    + "」已保存，稍后可在列表中点击「重新入库」");
        }
    }

    /**
     * 将切片 id 替换为稳定 id（文件名 + 内容 MD5），保证重复入库幂等
     */
    private List<Document> withStableIds(String filename, List<Document> chunks) {
        List<Document> documents = new ArrayList<>(chunks.size());
        for (Document chunk : chunks) {
            documents.add(Document.builder()
                    .id(KnowledgeAppDocumentLoader.stableDocumentId(filename, chunk.getText()))
                    .text(chunk.getText())
                    .metadata(chunk.getMetadata())
                    .build());
        }
        return documents;
    }

    /**
     * 枚举知识库目录及其语雀同步子目录下的 Markdown 文档
     */
    private List<DocumentFile> listDocumentFiles() {
        Path dir = requireDocumentDirectory();
        List<DocumentFile> files = new ArrayList<>();
        collectMarkdownFiles(dir, files);
        Path yuqueSyncDir = dir.resolve(YUQUE_SYNC_DIR);
        if (Files.isDirectory(yuqueSyncDir)) {
            collectMarkdownFiles(yuqueSyncDir, files);
        }
        return files;
    }

    /**
     * 收集目录下的 Markdown 文件信息
     */
    private void collectMarkdownFiles(Path dir, List<DocumentFile> target) {
        try (Stream<Path> stream = Files.list(dir)) {
            List<Path> paths = stream.filter(Files::isRegularFile).filter(this::isMarkdownFile).toList();
            for (Path path : paths) {
                String filename = path.getFileName().toString();
                target.add(new DocumentFile(filename, KnowledgeAppDocumentLoader.extractTopic(filename),
                        Files.size(path), toLocalDateTime(Files.getLastModifiedTime(path).toInstant())));
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("知识库文档目录读取失败：" + e.getMessage());
        }
    }

    /**
     * 统计指定文档在向量库中的切片数量
     */
    private Map<String, Integer> countChunks(List<String> filenames) {
        if (filenames.isEmpty()) {
            return Map.of();
        }
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("metadata." + METADATA_FILENAME).in(filenames)),
                Aggregation.group("metadata." + METADATA_FILENAME).count().as("count"));
        Map<String, Integer> counts = new HashMap<>();
        for (org.bson.Document document : mongoTemplate
                .aggregate(aggregation, collectionName, org.bson.Document.class).getMappedResults()) {
            Object key = document.get("_id");
            Object count = document.get("count");
            if (key != null && count instanceof Number number) {
                counts.put(String.valueOf(key), number.intValue());
            }
        }
        return counts;
    }

    /**
     * 构造按文件名匹配切片元数据的过滤表达式
     */
    private Filter.Expression filenameFilter(String filename) {
        return new Filter.Expression(Filter.ExpressionType.EQ,
                new Filter.Key(METADATA_FILENAME), new Filter.Value(filename));
    }

    /**
     * 解析文档状态：进行中或失败的任务记录优先，其余由切片数量推导
     */
    private DocumentStatus resolveStatus(DocumentTask task, int chunkCount) {
        if (task != null) {
            return task.status();
        }
        return chunkCount > 0 ? DocumentStatus.COMPLETED : DocumentStatus.NOT_INDEXED;
    }

    private String resolveErrorMessage(DocumentTask task) {
        return task == null ? null : task.errorMessage();
    }

    private Comparator<DocumentFile> resolveComparator(String sort) {
        String normalized = sort == null ? "" : sort.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "time_asc" -> Comparator.comparing(DocumentFile::updatedAt);
            case "name_asc" -> Comparator.comparing(DocumentFile::filename, String.CASE_INSENSITIVE_ORDER);
            default -> Comparator.comparing(DocumentFile::updatedAt).reversed();
        };
    }

    /**
     * 校验上传文件名：拒绝路径字符、控制字符、超长名称与非 Markdown 后缀
     */
    private String sanitizeFilename(String originalFilename) {
        String filename = originalFilename == null ? "" : originalFilename.trim();
        if (filename.isEmpty() || filename.length() <= MARKDOWN_SUFFIX.length()) {
            throw new IllegalArgumentException("文档名称为空，请重新选择文件");
        }
        if (filename.contains("/") || filename.contains("\\") || filename.contains("..") || filename.contains(":")) {
            throw new IllegalArgumentException("文档名称不能包含路径字符");
        }
        if (filename.chars().anyMatch(ch -> ch < 0x20 || ch == 0x7F)) {
            throw new IllegalArgumentException("文档名称包含非法字符");
        }
        if (filename.endsWith(".") || filename.endsWith(" ")) {
            throw new IllegalArgumentException("文档名称不能以点或空格结尾");
        }
        if (filename.length() > MAX_FILENAME_LENGTH) {
            throw new IllegalArgumentException("文档名称过长，请控制在 " + MAX_FILENAME_LENGTH + " 个字符以内");
        }
        if (!filename.toLowerCase(Locale.ROOT).endsWith(MARKDOWN_SUFFIX)) {
            throw new IllegalArgumentException("仅支持 .md 格式的 Markdown 文档");
        }
        return filename;
    }

    /**
     * 校验上传内容：大小、UTF-8 编码、非空与文本格式
     */
    private String decodeMarkdownContent(byte[] bytes) {
        if (bytes.length == 0) {
            throw new IllegalArgumentException("文档内容为空");
        }
        if (bytes.length > MAX_UPLOAD_BYTES) {
            throw new IllegalArgumentException("文档大小超过限制，单个文档最大 5MB");
        }
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        String content;
        try {
            content = decoder.decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException e) {
            throw new IllegalArgumentException("文档不是有效的 UTF-8 文本，请另存为 UTF-8 编码的 Markdown 文档后重试");
        }
        if (!content.isEmpty() && content.charAt(0) == '\uFEFF') {
            content = content.substring(1);
        }
        if (content.contains("\u0000")) {
            throw new IllegalArgumentException("文档内容不是文本格式，请上传 Markdown 文本文件");
        }
        if (content.isBlank()) {
            throw new IllegalArgumentException("文档内容为空");
        }
        return content;
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException("读取上传文件失败：" + e.getMessage());
        }
    }

    /**
     * 原子写入：先写临时文件再替换目标，避免读取方看到写入中的内容
     */
    private void writeAtomically(Path target, String content) {
        String suffix = ".tmp-" + UUID.randomUUID().toString().substring(0, 8);
        Path temp = target.resolveSibling(target.getFileName() + suffix);
        try {
            Files.writeString(temp, content, StandardCharsets.UTF_8);
            try {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("文档写入失败：" + e.getMessage() + describeWriteFailure(e));
        } finally {
            if (Files.exists(temp)) {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException e) {
                    log.debug("临时文件清理失败: {}", temp, e);
                }
            }
        }
    }

    /**
     * 写入失败的补充提示：目录不可写通常是打包成 jar 部署运行导致
     */
    private String describeWriteFailure(IOException e) {
        return e instanceof AccessDeniedException
                ? "（若为打包成 jar 部署运行，知识库目录不可写，暂不支持在线管理文档）"
                : "";
    }

    private void deleteFile(Path target, String filename) {
        try {
            Files.delete(target);
        } catch (IOException e) {
            throw new IllegalArgumentException("文档「" + filename + "」文件删除失败：" + e.getMessage());
        }
    }

    /**
     * 解析文档绝对路径：先在知识库目录查找，再在语雀同步子目录查找，并确认结果仍位于对应目录内
     */
    private Path resolveDocumentFile(String filename) {
        Path dir = requireDocumentDirectory();
        Path target = resolveInside(dir, filename);
        if (Files.isRegularFile(target)) {
            return target;
        }
        Path yuqueSyncDir = dir.resolve(YUQUE_SYNC_DIR);
        if (Files.isDirectory(yuqueSyncDir)) {
            Path nested = resolveInside(yuqueSyncDir, filename);
            if (Files.isRegularFile(nested)) {
                return nested;
            }
        }
        // 未找到时返回知识库目录下的路径，由调用方给出友好提示
        return target;
    }

    /**
     * 在指定目录内拼接文件名，并确认结果未越出该目录
     */
    private Path resolveInside(Path dir, String filename) {
        Path target = dir.resolve(filename).normalize();
        Path parent = target.getParent();
        if (parent == null || !parent.toAbsolutePath().normalize().equals(dir.toAbsolutePath().normalize())) {
            throw new IllegalArgumentException("文档名称非法");
        }
        return target;
    }

    private Path requireDocumentDirectory() {
        Path dir = documentLoader.resolveDocumentDirectory();
        if (dir == null) {
            throw new IllegalArgumentException("知识库文档目录不可用（打包成 jar 部署运行时不支持在线管理文档）");
        }
        return dir;
    }

    /**
     * 判断文档是否有正在执行的任务
     */
    private boolean isRunning(String filename) {
        DocumentTask task = tasks.get(filename);
        return task != null && task.running();
    }

    private void requireNotRunning(String filename) {
        if (isRunning(filename)) {
            throw new IllegalArgumentException("文档「" + filename + "」正在处理中，请稍后再试");
        }
    }

    private void requireAdmin(String username) {
        if (!userService.isAdmin(username)) {
            throw new IllegalArgumentException("无权限访问知识库文档管理");
        }
    }

    private boolean isMarkdownFile(Path path) {
        return path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(MARKDOWN_SUFFIX);
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    /**
     * 磁盘上的 Markdown 文档信息
     */
    private record DocumentFile(String filename, String name, long size, LocalDateTime updatedAt) {
    }

    /**
     * 文档处理任务：进行中（预处理/向量化）或失败
     */
    private record DocumentTask(DocumentStatus status, String errorMessage) {

        static DocumentTask preprocessing() {
            return new DocumentTask(DocumentStatus.PREPROCESSING, null);
        }

        static DocumentTask vectorizing() {
            return new DocumentTask(DocumentStatus.VECTORIZING, null);
        }

        static DocumentTask failed(DocumentStatus status, String errorMessage) {
            return new DocumentTask(status, errorMessage);
        }

        boolean running() {
            return status == DocumentStatus.PREPROCESSING || status == DocumentStatus.VECTORIZING;
        }
    }
}
