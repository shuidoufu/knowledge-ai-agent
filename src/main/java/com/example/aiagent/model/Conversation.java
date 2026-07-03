package com.example.aiagent.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDateTime;

@Document(collection = "conversations")
@Data
public class Conversation {

    @Id
    private String id;

    @Indexed(unique = true)
    private String conversationId;

    @Indexed
    private String userId;

    private String title;                   // 会话标题

    private Integer messageCount;           // 消息计数（避免频繁 count）

    private String modelName;               // 使用的模型

    private ConversationStatus status;      // active/archived/deleted

    private Instant createdAt;

    @Indexed                                // 排序用
    private Instant updatedAt;

    public enum ConversationStatus {
        ACTIVE, ARCHIVED, DELETED
    }
}