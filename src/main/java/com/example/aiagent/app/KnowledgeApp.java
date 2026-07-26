package com.example.aiagent.app;

import cn.hutool.json.JSONUtil;
import com.example.aiagent.advisor.DocCaptureAdvisor;
import com.example.aiagent.advisor.MyLoggerAdvisor;
import com.example.aiagent.chatmemory.MongoChatMemory;
import com.example.aiagent.model.ChatMessages;
import com.example.aiagent.rag.KnowledgeAppRagCustomAdvisorFactory;
import com.example.aiagent.rag.QueryRewriter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@Component
@Slf4j
public class KnowledgeApp {

    private final ChatClient chatClient;
    // 构造函数注入系统提示词
    private final String SYSTEM_PROMPT;
    @Resource
    private ToolCallback[] allTools;
    // 知识库向量存储（基于内存的RAG）
    @Resource
    private VectorStore knowledgeVectorStore;
    // 查询重写器
    @Resource
    private QueryRewriter queryRewriter;
    // MongoDB 操作（用于持久化 RAG 引用）
    @Resource
    private MongoTemplate mongoTemplate;
    // MCP服务工具调用
    @Resource
    private ToolCallbackProvider toolCallbackProvider;


    // MongoChatMemory,构造器注入，因为@Resource属于属性注入，晚于构造器
    public KnowledgeApp(@Qualifier("openAiChatModel") ChatModel chatModel, MongoChatMemory mongoChatMemory, @Value("${knowledge-agent.system-prompt}") String SYSTEM_PROMPT) {
        // 注入提示词
        this.SYSTEM_PROMPT = SYSTEM_PROMPT;

        // // 初始化基于文件的对话记忆
        // String fileDir = System.getProperty("user.dir") + "\\chat_memory";
        // ChatMemory chatMemory = new FileBasedChatMemory(fileDir);

        chatClient = ChatClient.builder(chatModel)
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

    public String doChat(String message, String chatId) {
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

    /**
     * 通过流式方式处理聊天请求 （无RAG）
     *
     * @param message 用户输入的消息内容
     * @param chatId  聊天会话的唯一标识符
     * @return 返回一个Flux流，包含流式返回的聊天响应内容
     */
    public Flux<String> doChatByStream(String message, String chatId) {
        return chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                // .advisors(new QuestionAnswerAdvisor(knowledgeVectorStore))
                .tools(allTools)
                .stream()
                .content();
    }

    /**
     * 带引用标注的 RAG 流式对话
     * AI 回复中会用 [1]、[2] 标注引用来源
     * 流结束后追加引用切片信息，前端可解析展示
     * 通过 LLM 智能判断是否需要检索知识库
     */
    public Flux<String> doChatByStreamWithRag(String message, String chatId) {
        // 智能分析：判断是否需要检索 + 查询改写（一次LLM调用完成）
        QueryRewriter.QueryAnalysis analysis = queryRewriter.analyze(message);
        log.info("查询分析: needsRetrieval={}, rewrittenQuery={}", analysis.needsRetrieval(), analysis.rewrittenQuery());

        // 不需要检索知识库：直接走普通流式对话
        if (!analysis.needsRetrieval()) {
            log.info("智能判断为无需检索知识库: message={}", message);
            return doChatByStream(message, chatId);
        }

        // 需要检索：使用改写后的查询进行 RAG 流式对话
        String rewriteMessage = analysis.rewrittenQuery();
        if (rewriteMessage == null || rewriteMessage.isBlank()) {
            rewriteMessage = message;
        }

        // 文档捕获 Advisor
        DocCaptureAdvisor docCaptureAdvisor = new DocCaptureAdvisor();

        // 引用标注指令：追加到系统提示词中
        String citationInstruction = "\n\n【引用规范】当你引用知识库中的内容时，"
                + "请在引用内容的结尾处标注来源编号，格式为 [1]、[2] 等。"
                + "例如：根据你整理的笔记，知识管理的核心在于持续积累与分类[1]。"
                + "注意：只有问题涉及知识库相关内容时才引用，日常问候或无关问题无需引用。";

        Flux<String> contentFlux = chatClient
                .prompt()
                .user(rewriteMessage)
                .system(s -> s.text(SYSTEM_PROMPT + citationInstruction))
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .advisors(new QuestionAnswerAdvisor(knowledgeVectorStore), docCaptureAdvisor)
                .tools(allTools)
                .stream()
                .content();

        // 文本流结束后，追加引用切片信息并持久化到 MongoDB
        return contentFlux.concatWith(Flux.defer(() -> {
            List<Document> docs = docCaptureAdvisor.getRetrievedDocuments();
            if (docs != null && !docs.isEmpty()) {
                List<Map<String, Object>> refs = new ArrayList<>();
                for (int i = 0; i < docs.size(); i++) {
                    Document doc = docs.get(i);
                    refs.add(Map.of(
                            "index", i + 1,
                            "content", doc.getText(),
                            "metadata", doc.getMetadata()
                    ));
                }
                // 持久化 references 到 MongoDB（ChatMessages 的最后一条 assistant 消息）
                try {
                    Query query = new Query(Criteria.where("conversationId").is(chatId));
                    ChatMessages chatDoc = mongoTemplate.findOne(query, ChatMessages.class, "chat_memory");
                    if (chatDoc != null && chatDoc.getMessages() != null && !chatDoc.getMessages().isEmpty()) {
                        // 最后一条消息是 AI 的回复
                        ChatMessages.MessageDocument lastMsg = chatDoc.getMessages().get(chatDoc.getMessages().size() - 1);
                        lastMsg.setReferences(refs);
                        mongoTemplate.save(chatDoc, "chat_memory");
                        log.info("RAG 引用已持久化到 MongoDB: chatId={}, refs={}", chatId, refs.size());
                    }
                } catch (Exception e) {
                    log.error("持久化 RAG 引用失败", e);
                }

                String refsJson = JSONUtil.toJsonStr(refs);
                return Flux.just("\n\n<!--RAG_REFS-->" + refsJson);
            }
            log.info("RAG 未检索到相关文档");
            return Flux.empty();
        }));
    }

    /**
     * KnowledgeReport 是一个记录知识总结的记录类(Record)
     * 用于存储知识回答的标题和相关摘要列表
     *
     * @param title       报告的标题，类型为String
     * @param suggestions 知识摘要列表，类型为List<String>
     */
    record KnowledgeReport(String title, List<String> suggestions) {

    }

    /**
     * AI 知识总结功能（结构化输出）
     *
     * @param message
     * @param chatId
     * @return
     */
    public KnowledgeReport doChatWithReport(String message, String chatId) {
        KnowledgeReport response = chatClient
                .prompt()
                .system(SYSTEM_PROMPT + "每次对话后都要生成知识总结，标题为{用户名}的知识报告，内容为要点列表")
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .call()
                .entity(KnowledgeReport.class);
        log.info("KnowledgeReport: {}", response);
        return response;
    }

    /**
     * 用 RAG 向量知识库进行对话 （暂不使用该RAG）
     *
     * @param message
     * @param chatId
     * @return
     */
    // @Autowired
    // private VectorStore pgVectorVectorStore;
    public String doChatWithRag(String message, String chatId) {
        // 查询分析（重写）
        QueryRewriter.QueryAnalysis analysis = queryRewriter.analyze(message);
        String rewriteMessage = (analysis.rewrittenQuery() != null && !analysis.rewrittenQuery().isBlank())
                ? analysis.rewrittenQuery() : message;

        ChatResponse chatResponse = chatClient
                .prompt()
                // 使用改写后的查询
                .user(rewriteMessage)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                // 应用 RAG 知识库问答（基于 本地内存 向量数据库）
                // .advisors(new QuestionAnswerAdvisor(knowledgeVectorStore))
                // 应用 RAG 检索增强服务（基于 PGVector 向量存储）
                // .advisors(new QuestionAnswerAdvisor(pgVectorVectorStore))
                // 带有标签过滤的 RAG 增强服务
                .advisors(
                        KnowledgeAppRagCustomAdvisorFactory.createKnowledgeRagAdvisor(
                                knowledgeVectorStore, "知识库"
                        )
                )
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    /**
     * AI 工具对话功能（支持调用工具）
     */
    public String doChatWithTool(String message, String chatId) {
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

    /**
     * AI 对话功能（调用MCP服务）
     */
    public String doChatWithMcp(String message, String chatId) {
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
