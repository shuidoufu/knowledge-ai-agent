package com.example.aiagent.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 知识库文档详情（磁盘当前内容 + 实时分块结果）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class KnowledgeDocumentDetailDTO {

    /** 文件名 */
    private String filename;

    /** 显示名称 */
    private String name;

    /** 文件大小（字节） */
    private long size;

    /** 最后修改时间 */
    private LocalDateTime updatedAt;

    /** 处理状态 */
    private DocumentStatus status;

    /** 失败原因（仅失败状态有值） */
    private String errorMessage;

    /** 已入库的切片数量 */
    private int chunkCount;

    /** 磁盘当前内容（已完成状态为预处理结果） */
    private String content;

    /** 分块结果 */
    private List<Chunk> chunks;

    /**
     * 文档切片
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Chunk {

        /** 切片序号（从 1 开始） */
        private int index;

        /** 切片标题（Markdown 最近一级标题，可能为空） */
        private String title;

        /** 切片正文 */
        private String text;

        /** 正文字符数 */
        private int length;
    }
}
