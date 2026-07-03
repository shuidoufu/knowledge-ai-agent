package com.example.aiagent.chatmemory;

import com.example.aiagent.model.ChatMessages;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class MongoChatMemory implements ChatMemory {

    @Autowired
    private MongoTemplate mongoTemplate;

    private static final String COLLECTION_NAME = "chat_memory";

    /**
     * 添加消息到指定会话
     */
    @Override
    public void add(String conversationId, List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }

        // 查询现有文档
        Query query = new Query(Criteria.where("conversationId").is(conversationId));
        ChatMessages existingDoc = mongoTemplate.findOne(query, ChatMessages.class, COLLECTION_NAME);

        List<ChatMessages.MessageDocument> newMessages = messages.stream()
                .map(this::convertToDocument)
                .collect(Collectors.toList());

        if (existingDoc == null) {
            // 新建文档
            ChatMessages document = new ChatMessages();
            document.setConversationId(conversationId);
            document.setMessages(newMessages);
            document.setCreatedAt(Instant.now());
            document.setUpdatedAt(Instant.now());
            mongoTemplate.save(document, COLLECTION_NAME);
        } else {
            // 追加消息
            existingDoc.getMessages().addAll(newMessages);
            existingDoc.setUpdatedAt(Instant.now());
            mongoTemplate.save(existingDoc, COLLECTION_NAME);
        }
    }

    /**
     * 获取最近 N 条消息
     */
    @Override
    public List<Message> get(String conversationId, int lastN) {
        Query query = new Query(Criteria.where("conversationId").is(conversationId));
        ChatMessages document = mongoTemplate.findOne(query, ChatMessages.class, COLLECTION_NAME);

        if (document == null || document.getMessages() == null || document.getMessages().isEmpty()) {
            return new ArrayList<>();
        }

        List<ChatMessages.MessageDocument> allMessages = document.getMessages();

        // 获取最后 N 条
        int startIndex = Math.max(0, allMessages.size() - lastN);
        List<ChatMessages.MessageDocument> recentMessages = allMessages.subList(startIndex, allMessages.size());

        return recentMessages.stream()
                .map(this::convertToMessage)
                .filter(msg -> msg != null)
                .collect(Collectors.toList());
    }

    /**
     * 清空指定会话的所有消息
     */
    @Override
    public void clear(String conversationId) {
        Query query = new Query(Criteria.where("conversationId").is(conversationId));
        mongoTemplate.remove(query, ChatMessages.class, COLLECTION_NAME);
    }

    // ============ 辅助转换方法 ============

    /**
     * Message -> Document
     */
    private ChatMessages.MessageDocument convertToDocument(Message message) {
        ChatMessages.MessageDocument doc = new ChatMessages.MessageDocument();
        doc.setRole(getRoleString(message.getMessageType()));
        doc.setContent(getMessageContent(message));
        doc.setTimestamp(Instant.now());
        return doc;
    }

    /**
     * Document -> Message
     */
    private Message convertToMessage(ChatMessages.MessageDocument doc) {
        return switch (doc.getRole().toLowerCase()) {
            case "user" -> new UserMessage(doc.getContent());
            case "assistant" -> new AssistantMessage(doc.getContent());
            case "system" -> new SystemMessage(doc.getContent());
            case "tool" -> new ToolResponseMessage(List.of()); // 简化处理
            default -> null;
        };
    }

    /**
     * 获取角色字符串
     */
    private String getRoleString(MessageType messageType) {
        return switch (messageType) {
            case USER -> "user";
            case ASSISTANT -> "assistant";
            case SYSTEM -> "system";
            case TOOL -> "tool";
        };
    }

    /**
     * 提取消息内容
     */
    private String getMessageContent(Message message) {
        return switch (message) {
            case UserMessage um -> um.getText();
            case AssistantMessage am -> am.getText();
            case SystemMessage sm -> sm.getText();
            case ToolResponseMessage trm -> trm.getResponses().toString();
            default -> "";
        };
    }
}