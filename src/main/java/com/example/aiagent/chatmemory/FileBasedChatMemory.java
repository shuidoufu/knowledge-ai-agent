package com.example.aiagent.chatmemory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于文件持久化的对话记忆 (使用 JSON 序列化)
 */
public class FileBasedChatMemory implements ChatMemory {

    private final String BASE_DIR;

    // Jackson ObjectMapper，用于 JSON 转换
    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        // 注册时间模块，处理 LocalDateTime 等时间类型
        objectMapper.registerModule(new JavaTimeModule());
        // 美化输出
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        // --- 新增配置开始 ---
        // 启用默认类型信息 (Non-Final)
        // 这会让 Jackson 在序列化 List<Message> 时，把每个元素的具体类名写入 JSON
        // 例如: ["java.util.ArrayList", [{"@class": "org.springframework.ai.chat.messages.UserMessage", ...}]]
        objectMapper.activateDefaultTyping(
                objectMapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL
        );
        // --- 新增配置结束 ---
    }

    public FileBasedChatMemory(String base_dir) {
        BASE_DIR = base_dir;
        File baseDir = new File(BASE_DIR);
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }
    }

    @Override
    public void add(String conversationId, Message message) {
        List<Message> current = get(conversationId, Integer.MAX_VALUE);
        current.add(message);
        saveConversation(conversationId, current);
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        List<Message> current = get(conversationId, Integer.MAX_VALUE);
        current.addAll(messages);
        saveConversation(conversationId, current);
    }

    @Override
    public List<Message> get(String conversationId, int lastN) {
        List<Message> all = getOrCreateConversation(conversationId);
        if (all == null) return new ArrayList<>();

        int fromIndex = Math.max(0, all.size() - lastN);
        return new ArrayList<>(all.subList(fromIndex, all.size()));
    }

    @Override
    public void clear(String conversationId) {
        File file = getConversationFile(conversationId);
        if (file.exists()) {
            file.delete();
        }
    }

    /**
     * 保存对话记录为 JSON 文件
     */
    private void saveConversation(String conversationId, List<Message> messages) {
        File file = getConversationFile(conversationId);
        try {
            // 将 List<Message> 转换为 JSON 字符串并写入文件
            objectMapper.writeValue(file, messages);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 读取 JSON 文件并转换为 List<Message>
     */
    private List<Message> getOrCreateConversation(String conversationId) {
        File file = getConversationFile(conversationId);
        if (!file.exists()) {
            return new ArrayList<>();
        }

        try {
            // 从 JSON 文件读取并转换为 List<Message>
            // 使用 TypeReference 处理泛型 List<Message>
            return objectMapper.readValue(file, new TypeReference<List<Message>>() {});
        } catch (IOException e) {
            System.err.println("Failed to load conversation from JSON: " + e.getMessage());
            e.printStackTrace();
            // 读取失败时删除文件，防止下次继续报错
            if (file.exists()) {
                file.delete();
            }
            return new ArrayList<>();
        }
    }

    private File getConversationFile(String conversationId) {
        return new File(BASE_DIR, conversationId + ".json");
    }
}
