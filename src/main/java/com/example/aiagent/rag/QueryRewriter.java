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
        String prompt = """
                判断以下用户问题是否需要搜索知识库来回答。

                需要搜索：涉及具体知识、技术概念、用户笔记/收藏、需要查阅资料的问题。
                不需要搜索：日常问候、闲聊、感谢、情感表达、询问AI自身情况等。

                如果需要搜索，把问题改写为适合向量检索的形式（提取关键词、去除口语）。
                如果不需要搜索，rewrittenQuery 填空字符串 ""。

                只输出纯 JSON，不要 markdown 代码块，不要多余文字：
                {"needsRetrieval": true, "rewrittenQuery": "改写后的查询"}
                （不需要搜索时输出：{"needsRetrieval": false, "rewrittenQuery": ""}）

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
