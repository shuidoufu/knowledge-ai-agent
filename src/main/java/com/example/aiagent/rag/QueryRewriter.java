package com.example.aiagent.rag;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * 查询分析器：智能判断是否需要知识库检索，并对查询进行重写
 */
@Slf4j
@Component
public class QueryRewriter {

    private final ChatClient chatClient;

    public QueryRewriter(@Qualifier("openAiChatModel") ChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel).build();
    }

    /**
     * 分析用户问题，判断是否需要检索知识库，并对查询进行重写
     *
     * @param message 用户原始输入
     * @return 查询分析结果
     */
    public QueryAnalysis analyze(String message) {
        // 判定采用"单一判据"：问题是否可能与用户个人笔记/收藏相关
        // 注意：user(String) 会走 PromptTemplate 模板渲染，prompt 中不能出现 {} 字面量
        String prompt = """
                判断以下用户问题是否需要检索用户的知识库（个人笔记/收藏）来回答。

                核心判断标准：这个问题是否可能与用户自己整理过的笔记/收藏内容相关？
                需要检索：涉及具体知识、技术概念，用户可能在笔记中整理过相关内容（例如"HashMap原理"、"RAG是什么"）。
                不需要检索：日常问候、闲聊、实时信息（天气、新闻等）、需要调用工具执行的任务（搜索图片、生成PDF、下载等），
                以及任何检索用户笔记也无法回答的问题。

                如果需要检索，把问题改写为适合向量检索的形式（提取关键词、去除口语）。
                改写必须保留用户的所有动作要求（搜索、生成、下载、整理等）和任务步骤，只做口语化精简，不要删减任何步骤。
                如果不需要检索，rewrittenQuery 为空字符串。

                只输出纯 JSON，不要 markdown 代码块，不要多余文字。
                JSON 包含两个字段：
                - needsRetrieval：布尔值，true 表示需要检索知识库，false 表示不需要
                - rewrittenQuery：字符串，改写后的查询；不需要检索时为空字符串

                用户问题：""" + message;

        try {
            return chatClient.prompt()
                    .user(prompt)
                    .call()
                    .entity(QueryAnalysis.class);
        } catch (Exception e) {
            log.warn("查询分析解析失败，默认走检索流程: {}", e.getMessage());
            return new QueryAnalysis(true, message);
        }
    }

    /**
     * 查询分析结果
     */
    public record QueryAnalysis(
            @JsonProperty("needsRetrieval") boolean needsRetrieval,
            @JsonProperty("rewrittenQuery") String rewrittenQuery
    ) {}

}
