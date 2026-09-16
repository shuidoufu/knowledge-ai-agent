package com.example.aiagent.model;

/**
 * 知识库文档处理状态
 * DTO 字段类型的枚举类（与相关 KnowledgeDocument*DTO* 强绑定）
 */
public enum DocumentStatus {

    /** 预处理中 */
    PREPROCESSING,

    /** 向量化中 */
    VECTORIZING,

    /** 已完成（切片已入库） */
    COMPLETED,

    /** 未入库（磁盘有文档，向量库无对应切片） */
    NOT_INDEXED,

    /** 预处理阶段失败 */
    PREPROCESS_FAILED,

    /** 向量化阶段失败 */
    VECTORIZE_FAILED
}
