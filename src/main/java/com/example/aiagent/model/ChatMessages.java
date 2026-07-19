package com.example.aiagent.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import org.springframework.data.mongodb.core.index.Indexed;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 基于MongoDB实现将会话内容存储到MongoDB中
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Document("chat_memory")
public class ChatMessages {
    // 唯一标识，映射到 MongoDB 文档的 _id 字段
    @Id
    private String id;

    @Indexed(unique = true)
    private String conversationId;

    private String title;

    private List<MessageDocument> messages;

    private Instant createdAt;

    private Instant updatedAt;

    // 嵌套消息文档
    public static class MessageDocument {
        private String role;        // 角色：user, assistant, system
        private String content;     // 消息内容
        private Instant timestamp;  // 时间戳
        private List<Map<String, Object>> references; // RAG 引用切片

        // Getters & Setters
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }

        public Instant getTimestamp() { return timestamp; }
        public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

        public List<Map<String, Object>> getReferences() { return references; }
        public void setReferences(List<Map<String, Object>> references) { this.references = references; }
    }
}