package com.example.aiagent.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 知识库文档处理线程池：单线程串行执行，避免同一文档的预处理与向量化并发读写
 */
@Configuration
public class KnowledgeDocumentTaskConfig {

    /** 排队上限（超出后拒绝新任务） */
    private static final int QUEUE_CAPACITY = 20;

    /** 关闭时等待在途任务完成的最长秒数 */
    private static final int AWAIT_TERMINATION_SECONDS = 30;

    /**
     * 知识库文档处理线程池：单线程、有界队列，队列满时拒绝新任务
     *
     * @return 文档处理线程池
     */
    @Bean("knowledgeDocumentExecutor")
    public ThreadPoolTaskExecutor knowledgeDocumentExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(QUEUE_CAPACITY);
        executor.setThreadNamePrefix("knowledge-doc-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(AWAIT_TERMINATION_SECONDS);
        executor.initialize();
        return executor;
    }
}
