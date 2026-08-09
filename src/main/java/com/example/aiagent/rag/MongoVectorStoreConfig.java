package com.example.aiagent.rag;

import cn.hutool.crypto.digest.DigestUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * 知识库向量数据库配置（初始化 MongoDB 向量数据库 Bean）
 * 使用自研 MongoVectorStore（文档+向量存 MongoDB 集合，应用层余弦相似度检索），
 * 兼容 MongoDB 社区版（不依赖 Atlas Search 的 $vectorSearch）
 * 通过 conditionProperty.ai.bean-type=mongoVectorStore 条件加载，与内存向量库（KnowledgeAppVectorStoreConfig）互斥切换
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "conditionProperty.ai", name = "bean-type", havingValue = "mongoVectorStore")
public class MongoVectorStoreConfig {

    /**
     * 向量集合名
     */
    @Value("${spring.ai.vectorstore.mongodb.collection-name:vector_store}")
    private String collectionName;

    /**
     * embedding 字段路径
     */
    @Value("${spring.ai.vectorstore.mongodb.path-name:embedding}")
    private String pathName;

    @Resource
    private KnowledgeAppDocumentLoader knowledgeAppDocumentLoader;

    /**
     * 创建 MongoDB 向量数据库 Bean
     * 与内存向量库 Bean 同名且均标注 @Primary，由 conditionProperty.ai.bean-type 控制互斥加载，避免 Bean 冲突
     *
     * @param mongoTemplate  MongoDB 操作模板
     * @param embeddingModel 向量化模型（千问 DashScope）
     * @return MongoDB 向量数据库
     */
    @Bean
    @Primary
    public VectorStore knowledgeVectorStore(MongoTemplate mongoTemplate,
                                            @Qualifier("dashscopeEmbeddingModel") EmbeddingModel embeddingModel) {
        // 自研实现：存储结构（_id/content/metadata/embedding）与官方 MongoDBAtlasVectorStore 一致，
        // 检索在应用层计算余弦相似度，兼容 MongoDB 社区版（Atlas Search 为 Enterprise/Atlas 专属特性）
        MongoVectorStore vectorStore = new MongoVectorStore(mongoTemplate, embeddingModel,
                collectionName, pathName, new DashScopeBatchingStrategy());
        // 幂等增量加载本地知识库文档
        loadDocumentsIncrementally(vectorStore, mongoTemplate);
        return vectorStore;
    }

    /**
     * 幂等增量加载知识库文档
     * 为每个文档切片生成稳定 documentId（文件名 + 内容MD5），已存在的切片跳过，
     * 避免重启时重复调用 embedding API 和产生重复数据；文档内容变更时自动增量补入
     *
     * @param vectorStore   MongoDB 向量数据库
     * @param mongoTemplate MongoDB 操作模板（用于查询已存在的文档 id）
     */
    private void loadDocumentsIncrementally(VectorStore vectorStore, MongoTemplate mongoTemplate) {
        List<Document> documentList = knowledgeAppDocumentLoader.loadMarkdowns();
        // 为每个切片生成稳定 documentId：文件名#内容MD5（内容不变则 id 不变，重启时跳过）
        // Document 不可变，需通过 builder 重建带稳定 id 的切片
        for (int i = 0; i < documentList.size(); i++) {
            Document document = documentList.get(i);
            String filename = (String) document.getMetadata().getOrDefault("filename", "unknown");
            String stableId = filename + "#" + DigestUtil.md5Hex(document.getText());
            documentList.set(i, Document.builder()
                    .id(stableId)
                    .text(document.getText())
                    .metadata(document.getMetadata())
                    .build());
        }
        // 查询集合中已存在的文档 id
        Set<String> existingIds = StreamSupport.stream(mongoTemplate.getCollection(collectionName)
                        .distinct("_id", String.class).spliterator(), false)
                .collect(Collectors.toSet());
        // 过滤出新增切片
        List<Document> newDocuments = documentList.stream()
                .filter(document -> !existingIds.contains(document.getId()))
                .toList();
        if (!newDocuments.isEmpty()) {
            vectorStore.add(newDocuments);
        }
        log.info("MongoDB知识库文档加载完成: 总数={}, 新增={}, 跳过={}",
                documentList.size(), newDocuments.size(), documentList.size() - newDocuments.size());
    }

    /**
     * 按条数分批的向量化分批策略
     * DashScope embedding API 单次调用最多 25 条文本，而官方默认的 TokenCountBatchingStrategy
     * 按 token 数分批（切片 token 较少时单批可能超过 25 条），因此按条数每批 20 条分批，保证兼容
     */
    private static class DashScopeBatchingStrategy implements BatchingStrategy {

        /**
         * 每批最大条数（DashScope 上限 25，留 20% 余量）
         */
        private static final int MAX_BATCH_SIZE = 20;

        @Override
        public List<List<Document>> batch(List<Document> documents) {
            List<List<Document>> batches = new ArrayList<>();
            for (int i = 0; i < documents.size(); i += MAX_BATCH_SIZE) {
                batches.add(documents.subList(i, Math.min(i + MAX_BATCH_SIZE, documents.size())));
            }
            return batches;
        }
    }

}
