package com.example.aiagent.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.advisor.api.*;
import org.springframework.ai.chat.model.MessageAggregator;
import reactor.core.publisher.Flux;

import java.util.Map;

@Slf4j
public class MyLoggerAdvisor implements CallAroundAdvisor, StreamAroundAdvisor {

    public String getName() {
        return this.getClass().getSimpleName();
    }

    public int getOrder() {
        return 0;
    }

    /**
     * 在处理请求前进行预处理的方法
     *
     * @param request 包含用户请求信息的AdvisedRequest对象
     * @return 返回处理后的AdvisedRequest对象
     */
    private AdvisedRequest before(AdvisedRequest request) {
        // 记录用户请求的日志信息
        log.info("AI request: {}", request.userText());
        // 记录 RAG 向量检索的上下文（包含检索到的文档内容）
        // Map<String, Object> context = request.adviseContext();
        // Object docs = context.get("qa_retrieved_documents");
        // if (docs != null) {
        //     log.info(">>> RAG 向量检索命中! 检索到 {} 篇文档 <<<", ((java.util.List<?>) docs).size());
        //     log.info("RAG context: {}", context);
        // } else {
        //     log.info(">>> 未命中 RAG 向量检索 <<<");
        // }
        // 返回原始请求对象（此处可根据需要添加预处理逻辑）
        return request;
    }

    private void observeAfter(AdvisedResponse advisedResponse) {
        log.info("AI response: {}", advisedResponse.response().getResult().getOutput().getText());
    }

    public AdvisedResponse aroundCall(AdvisedRequest advisedRequest, CallAroundAdvisorChain chain) {
        advisedRequest = this.before(advisedRequest);
        AdvisedResponse advisedResponse = chain.nextAroundCall(advisedRequest);
        this.observeAfter(advisedResponse);
        return advisedResponse;
    }

    public Flux<AdvisedResponse> aroundStream(AdvisedRequest advisedRequest, StreamAroundAdvisorChain chain) {
        advisedRequest = this.before(advisedRequest);
        Flux<AdvisedResponse> advisedResponses = chain.nextAroundStream(advisedRequest);
        return (new MessageAggregator()).aggregateAdvisedResponse(advisedResponses, this::observeAfter);
    }
}

