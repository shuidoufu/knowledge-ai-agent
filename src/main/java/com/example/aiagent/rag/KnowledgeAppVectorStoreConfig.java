package com.example.aiagent.rag;

import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;

/**
 * 知识库向量数据库配置（初始化基于本地内存的向量数据库Bean）
 * 通过 conditionProperty.ai.bean-type=memoryVectorStore 条件加载，与 MongoDB 向量库（MongoVectorStoreConfig）互斥切换
 */
@Configuration
@ConditionalOnProperty(prefix = "conditionProperty.ai", name = "bean-type", havingValue = "memoryVectorStore")
public class KnowledgeAppVectorStoreConfig {

    @Resource
    private KnowledgeAppDocumentLoader knowledgeAppDocumentLoader;

//    @Resource
//    private MyKeywordEnricher myKeywordEnricher;

    @Bean
    @Primary
    public VectorStore knowledgeVectorStore(@Qualifier("dashscopeEmbeddingModel") EmbeddingModel embeddingModel) {
        SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(embeddingModel).build();
        // 加载文档
        List<Document> documentList = knowledgeAppDocumentLoader.loadMarkdowns();
//        // 自动补充关键元信息
//        List<Document> enrichDocuments= myKeywordEnricher.enrichDocument(documentList);
        simpleVectorStore.add(documentList);
//        simpleVectorStore.add(enrichDocuments);
        return simpleVectorStore;
    }

}
