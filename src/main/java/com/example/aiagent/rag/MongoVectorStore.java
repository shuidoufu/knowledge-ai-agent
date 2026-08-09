package com.example.aiagent.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentMetadata;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingOptionsBuilder;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 基于 MongoDB 的自研向量数据库实现
 * 文档与向量持久化到 MongoDB 集合（结构：_id、content、metadata、embedding），
 * 检索时在应用层计算余弦相似度排序（不依赖 Atlas Search 的 $vectorSearch，兼容 MongoDB 社区版）
 */
@Slf4j
public class MongoVectorStore implements VectorStore {

    /** 文档 id 字段名（与集合存储结构一致） */
    public static final String ID_FIELD_NAME = "_id";

    /** 文档内容字段名 */
    public static final String CONTENT_FIELD_NAME = "content";

    /** 文档元数据字段名 */
    public static final String METADATA_FIELD_NAME = "metadata";

    /** 相似度得分字段名 */
    public static final String SCORE_FIELD_NAME = "score";

    private final MongoTemplate mongoTemplate;

    private final EmbeddingModel embeddingModel;

    /** 向量集合名 */
    private final String collectionName;

    /** embedding 字段路径 */
    private final String pathName;

    /** 向量化分批策略（兼容 DashScope 单次 25 条上限） */
    private final BatchingStrategy batchingStrategy;

    /**
     * 构造自研 MongoDB 向量数据库
     *
     * @param mongoTemplate    MongoDB 操作模板
     * @param embeddingModel   向量化模型
     * @param collectionName   向量集合名
     * @param pathName         embedding 字段路径
     * @param batchingStrategy 向量化分批策略
     */
    public MongoVectorStore(MongoTemplate mongoTemplate, EmbeddingModel embeddingModel,
                            String collectionName, String pathName, BatchingStrategy batchingStrategy) {
        this.mongoTemplate = mongoTemplate;
        this.embeddingModel = embeddingModel;
        this.collectionName = collectionName;
        this.pathName = pathName;
        this.batchingStrategy = batchingStrategy;
    }

    /**
     * 添加文档：生成向量后按 _id 幂等写入 MongoDB（save 为 upsert 语义，重复 id 覆盖更新）
     *
     * @param documents 文档切片列表
     */
    @Override
    public void add(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return;
        }
        // 批量向量化（按分批策略拆批，保证与 DashScope 单次 25 条上限兼容）
        List<float[]> embeddings = this.embeddingModel.embed(documents,
                EmbeddingOptionsBuilder.builder().build(), this.batchingStrategy);
        for (int i = 0; i < documents.size(); i++) {
            Document document = documents.get(i);
            org.bson.Document mongoDocument = new org.bson.Document()
                    .append(ID_FIELD_NAME, document.getId())
                    .append(CONTENT_FIELD_NAME, document.getText())
                    .append(METADATA_FIELD_NAME, document.getMetadata())
                    .append(pathName, embeddings.get(i));
            this.mongoTemplate.save(mongoDocument, this.collectionName);
        }
    }

    /**
     * 按文档 id 列表删除
     *
     * @param idList 文档 id 列表
     */
    @Override
    public void delete(List<String> idList) {
        if (idList == null || idList.isEmpty()) {
            return;
        }
        Query query = new Query(Criteria.where(ID_FIELD_NAME).in(idList));
        this.mongoTemplate.remove(query, this.collectionName);
    }

    /**
     * 按过滤表达式删除（加载全量后应用层评估过滤条件，收集匹配的文档 id 批量删除）
     *
     * @param filterExpression 过滤表达式
     */
    @Override
    public void delete(Filter.Expression filterExpression) {
        if (filterExpression == null) {
            return;
        }
        List<String> matchedIds = loadAll().stream()
                .filter(mongoDocument -> evaluate(filterExpression,
                        mongoDocument.get(METADATA_FIELD_NAME, org.bson.Document.class)))
                .map(mongoDocument -> mongoDocument.getString(ID_FIELD_NAME))
                .toList();
        delete(matchedIds);
    }

    /**
     * 相似度检索：应用层加载全部文档向量，计算余弦相似度，按阈值过滤并取 topK
     * 当前知识库数据量小（数百切片），全量计算毫秒级完成；数据量增大后可升级为 $vectorSearch 或索引方案
     *
     * @param request 检索请求（含查询、topK、相似度阈值、过滤表达式）
     * @return 按相似度降序排列的文档列表
     */
    @Override
    public List<Document> similaritySearch(SearchRequest request) {
        // 查询向量化
        float[] queryEmbedding = this.embeddingModel.embed(request.getQuery());

        List<ScoredDocument> scoredDocuments = new ArrayList<>();
        for (org.bson.Document mongoDocument : loadAll()) {
            // 过滤表达式应用层评估（如 status 过滤）
            if (request.getFilterExpression() != null
                    && !evaluate(request.getFilterExpression(),
                            mongoDocument.get(METADATA_FIELD_NAME, org.bson.Document.class))) {
                continue;
            }
            List<?> embeddingList = mongoDocument.get(pathName, List.class);
            if (embeddingList == null) {
                continue;
            }
            float[] storedEmbedding = toFloatArray(embeddingList);
            double score = cosineSimilarity(queryEmbedding, storedEmbedding);
            // 相似度阈值过滤
            if (score < request.getSimilarityThreshold()) {
                continue;
            }
            scoredDocuments.add(new ScoredDocument(mongoDocument, score));
        }

        // 按相似度降序排序，取 topK
        return scoredDocuments.stream()
                .sorted(Comparator.comparingDouble(ScoredDocument::score).reversed())
                .limit(request.getTopK())
                .map(this::toDocument)
                .toList();
    }

    /**
     * 加载集合中全部文档（仅取检索所需字段）
     *
     * @return 文档列表
     */
    private List<org.bson.Document> loadAll() {
        Query query = new Query();
        query.fields().include(ID_FIELD_NAME, CONTENT_FIELD_NAME, METADATA_FIELD_NAME, pathName);
        return this.mongoTemplate.find(query, org.bson.Document.class, this.collectionName);
    }

    /**
     * 将存储的向量列表转换为 float 数组
     *
     * @param embeddingList 向量列表
     * @return float 数组
     */
    private float[] toFloatArray(List<?> embeddingList) {
        float[] result = new float[embeddingList.size()];
        for (int i = 0; i < embeddingList.size(); i++) {
            result[i] = ((Number) embeddingList.get(i)).floatValue();
        }
        return result;
    }

    /**
     * 计算两个向量的余弦相似度
     *
     * @param a 向量 a
     * @param b 向量 b
     * @return 余弦相似度（0~1）
     */
    private double cosineSimilarity(float[] a, float[] b) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * 将存储的 MongoDB 文档转换为 Spring AI Document（附带 score 与 distance 元数据，与官方实现一致）
     *
     * @param scoredDocument 带相似度得分的存储文档
     * @return Spring AI Document
     */
    private Document toDocument(ScoredDocument scoredDocument) {
        org.bson.Document mongoDocument = scoredDocument.mongoDocument();
        Map<String, Object> metadata = new HashMap<>(
                mongoDocument.get(METADATA_FIELD_NAME, org.bson.Document.class));
        metadata.put(DocumentMetadata.DISTANCE.value(), 1 - scoredDocument.score());
        return Document.builder()
                .id(mongoDocument.getString(ID_FIELD_NAME))
                .text(mongoDocument.getString(CONTENT_FIELD_NAME))
                .metadata(metadata)
                .score(scoredDocument.score())
                .build();
    }

    /**
     * 应用层评估过滤表达式（支持 AND、OR、NOT、EQ、NE、GT、GTE、LT、LTE、IN、NIN）
     * 字段取值来自文档 metadata
     *
     * @param expression 过滤表达式
     * @param metadata   文档元数据
     * @return 是否匹配
     */
    private boolean evaluate(Filter.Expression expression, Map<String, Object> metadata) {
        if (expression == null) {
            return true;
        }
        return switch (expression.type()) {
            case AND -> evaluate(asExpression(expression.left()), metadata)
                    && evaluate(asExpression(expression.right()), metadata);
            case OR -> evaluate(asExpression(expression.left()), metadata)
                    || evaluate(asExpression(expression.right()), metadata);
            case NOT -> !evaluate(asExpression(expression.left()), metadata);
            case EQ, NE, GT, GTE, LT, LTE -> {
                String key = ((Filter.Key) expression.left()).key();
                Object expected = ((Filter.Value) expression.right()).value();
                Object actual = metadata.get(key);
                int compare = compareValue(actual, expected);
                yield switch (expression.type()) {
                    case EQ -> compare == 0;
                    case NE -> compare != 0;
                    case GT -> compare > 0;
                    case GTE -> compare >= 0;
                    case LT -> compare < 0;
                    default -> compare <= 0;
                };
            }
            case IN, NIN -> {
                String key = ((Filter.Key) expression.left()).key();
                Object actual = metadata.get(key);
                List<?> expectedValues = (List<?>) ((Filter.Value) expression.right()).value();
                boolean contains = expectedValues.stream().anyMatch(value -> compareValue(actual, value) == 0);
                yield expression.type() == Filter.ExpressionType.IN ? contains : !contains;
            }
        };
    }

    /**
     * 解包操作数：Group 包装的表达式需取出内层表达式
     *
     * @param operand 操作数
     * @return 内层表达式
     */
    private Filter.Expression asExpression(Filter.Operand operand) {
        if (operand instanceof Filter.Group group) {
            return group.content();
        }
        return (Filter.Expression) operand;
    }

    /**
     * 比较字段实际值与期望值（数字按数值比较，其余按字符串比较）
     *
     * @param actual   实际值
     * @param expected 期望值
     * @return 比较结果（-1、0、1）
     */
    private int compareValue(Object actual, Object expected) {
        if (actual instanceof Number actualNumber && expected instanceof Number expectedNumber) {
            return Double.compare(actualNumber.doubleValue(), expectedNumber.doubleValue());
        }
        return Objects.equals(actual, expected) ? 0
                : String.valueOf(actual).compareTo(String.valueOf(expected));
    }

    /**
     * 带相似度得分的存储文档内部记录
     *
     * @param mongoDocument MongoDB 存储文档
     * @param score         余弦相似度得分
     */
    private record ScoredDocument(org.bson.Document mongoDocument, double score) {
    }

}
