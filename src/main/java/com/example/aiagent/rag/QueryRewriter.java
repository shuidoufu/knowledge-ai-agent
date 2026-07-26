package com.example.aiagent.rag;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * 查询分析器：智能判断是否需要知识库检索，并对查询进行重写
 */
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
                你是一个查询分析器。你的任务有两步：

                第一步：判断用户的问题是否需要搜索知识库。
                需要搜索的场景：问题涉及具体知识、技术概念、用户笔记/收藏中的内容、
                需要查阅资料才能回答的问题。
                不需要搜索的场景：日常问候、闲聊、感谢、简单聊天、情感表达、
                询问AI自身情况等无需查阅资料的对话。

                第二步：如果需要搜索，将用户的问题改写成更适合向量检索的形式
                （提取核心关键词、去除口语化表达）；如果不需要搜索，改写结果留空。

                请严格按以下JSON格式输出（不要包含markdown标记）：
                {"needsRetrieval": true/false, "rewrittenQuery": "改写后的查询或空字符串"}

                用户问题：""" + message;

        return chatClient.prompt()
                .user(prompt)
                .call()
                .entity(QueryAnalysis.class);
    }

    /**
     * 查询分析结果
     */
    public record QueryAnalysis(
            @JsonProperty("needsRetrieval") boolean needsRetrieval,
            @JsonProperty("rewrittenQuery") String rewrittenQuery
    ) {}

}
