package com.example.aiagent.app;

import com.example.aiagent.advisor.MyLoggerAdvisor;
import com.example.aiagent.chatmemory.FileBasedChatMemory;
import com.example.aiagent.chatmemory.MongoChatMemory;
import com.example.aiagent.rag.LoveAppRagCustomAdvisorFactory;
import com.example.aiagent.rag.QueryRewriter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@Component
@Slf4j
public class LoveApp {

    private final ChatClient chatClient;

    // MongoChatMemory,构造器注入，因为@Resource属于属性注入，晚于构造器
    public LoveApp(ChatModel dashScopeChatModel, MongoChatMemory mongoChatMemory,
                   @Value("${love-advisor.system-prompt}") String SYSTEM_PROMPT) {
        // 初始化基于文件的对话记忆
        String fileDir = System.getProperty("user.dir") + "\\chat_memory";
        ChatMemory chatMemory = new FileBasedChatMemory(fileDir);

        chatClient = ChatClient.builder(dashScopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        // 基于内存
                        // new MessageChatMemoryAdvisor(chatMemory),
                        // 基于MongoDB
                        new MessageChatMemoryAdvisor(mongoChatMemory),
                        // 自定义日志拦截器
                        new MyLoggerAdvisor()
                ).build();

    }

    public String doChat(String message, String chatId){
        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }


    public Flux<String> doChatByStream(String message, String chatId){
        return chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .stream()
                .content();
    }

    record LoveReport(String title, List<String> suggestions) {

    }

    @Value("${love-advisor.system-prompt}")
    private String SYSTEM_PROMPT;

    /**
     * AI 报告功能（结构化输出）
     * @param message
     * @param chatId
     * @return
     */
    public LoveReport doChatWithReport(String message, String chatId){
        LoveReport response = chatClient
                .prompt()
                .system(SYSTEM_PROMPT + "每次对话后都要生成恋爱结果，标题为{用户名}的恋爱报告，内容为建议列表")
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .call()
                .entity(LoveReport.class);
        log.info("LoveReport: {}", response);
        return response;
    }

    // AI 恋爱知识库问答功能
    @Resource
    private VectorStore pgVectorVectorStore;

    // 查询重写器
    @Resource
    private QueryRewriter queryRewriter;

    /**
     * 用 RAG 向量知识库进行对话
     * @param message
     * @param chatId
     * @return
     */
    public String doChatWithRag(String message, String chatId){
        // 查询重写
        String rewriteMessage = queryRewriter.doQueryRewrite(message);

        ChatResponse chatResponse = chatClient
                .prompt()
                // 使用改写后的查询
                .user(rewriteMessage)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                // 应用 RAG 知识库问答
                // .advisors(new QuestionAnswerAdvisor(vectorStore))
                // 应用 RAG 检索增强服务（基于 PGVector 向量存储）
                // .advisors(new QuestionAnswerAdvisor(pgVectorVectorStore))
                .advisors(
                        LoveAppRagCustomAdvisorFactory.createLoveAppRagCustomAdvisorFactory(
                                pgVectorVectorStore, "已婚"
                        )
                )
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    @Resource
    private ToolCallback[] allTools;
    /**
     * AI 恋爱报告功能（支持调用工具）
     */

    public String doChatWithTool(String message, String chatId){
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                // 开启日志
                .advisors(new MyLoggerAdvisor())
                .tools(allTools)
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    @Resource
    private ToolCallbackProvider toolCallbackProvider;
    /**
     * AI 恋爱报告功能（调用MCP服务）
     */

    public String doChatWithMcp(String message, String chatId){
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                // 开启日志
                .advisors(new MyLoggerAdvisor())
                .tools(toolCallbackProvider)
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }
}
