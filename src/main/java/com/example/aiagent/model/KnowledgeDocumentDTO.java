package com.example.aiagent.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 知识库文档列表项
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class KnowledgeDocumentDTO {

    /** 文件名（含 .md 后缀，文档唯一标识） */
    private String filename;

    /** 显示名称（文件名去序号前缀与后缀） */
    private String name;

    /** 文件大小（字节） */
    private long size;

    /** 最后修改时间（上传的文档即上传时间） */
    private LocalDateTime updatedAt;

    /** 已入库的切片数量 */
    private int chunkCount;

    /** 处理状态 */
    private DocumentStatus status;

    /** 失败原因（仅失败状态有值） */
    private String errorMessage;
}
