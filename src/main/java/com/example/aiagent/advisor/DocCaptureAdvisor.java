package com.example.aiagent.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;

import org.springframework.ai.chat.client.advisor.api.*;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 文档捕获（引用标注）
 * 捕获 QuestionAnswerAdvisor 检索到的文档
 * 必须在 QuestionAnswerAdvisor 之后执行（order > 0）
 */
@Slf4j
public class DocCaptureAdvisor implements CallAroundAdvisor, StreamAroundAdvisor {

    private volatile List<Document> retrievedDocuments;

    public List<Document> getRetrievedDocuments() {
        return retrievedDocuments;
    }

    @Override
    public String getName() {
        return "DocCaptureAdvisor";
    }

    @Override
    public int getOrder() {
        return 1; // 在 QuestionAnswerAdvisor(order=0) 之后执行
    }

    @Override
    public AdvisedResponse aroundCall(AdvisedRequest advisedRequest, CallAroundAdvisorChain chain) {
        captureDocs(advisedRequest);
        return chain.nextAroundCall(advisedRequest);
    }

    @Override
    public Flux<AdvisedResponse> aroundStream(AdvisedRequest advisedRequest, StreamAroundAdvisorChain chain) {
        captureDocs(advisedRequest);
        return chain.nextAroundStream(advisedRequest);
    }

    @SuppressWarnings("unchecked")
    private void captureDocs(AdvisedRequest request) {
        Object docs = request.adviseContext().get("qa_retrieved_documents");
        if (docs instanceof List) {
            retrievedDocuments = (List<Document>) docs;
            log.info("DocCaptureAdvisor 捕获到 {} 篇检索文档", retrievedDocuments.size());
        }
    }
}