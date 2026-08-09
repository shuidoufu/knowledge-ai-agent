package com.example.aiagent.config;


import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.example.aiagent.advisor.MyLoggerAdvisor;
import com.example.aiagent.chatmemory.MongoChatMemory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI配置类，用于配置ChatClient相关的Bean
 */
@Configuration
@ConditionalOnProperty(prefix = "conditionProperty.ai", name = "bean-type", havingValue = "chatClientConfig")
public class MyChatClientConfig {

    /**
     * 创建并配置ChatClient Bean
     *
     * @param chatModel       聊天模型，通过@Qualifier指定为"openAiChatModel"
     * @param myLoggerAdvisor 日志记录拦截器，用于记录聊天交互日志
     * @param SYSTEM_PROMPT   系统提示信息，通过@Value注解从配置文件中获取
     * @return 配置好的ChatClient实例
     */
    @Bean
    public ChatClient chatClient(@Qualifier("openAiChatModel") ChatModel chatModel, MyLoggerAdvisor myLoggerAdvisor, MongoChatMemory mongoChatMemory, @Value("${knowledge-agent.system-prompt}") String SYSTEM_PROMPT) {

        // 使用建造者模式创建ChatClient实例
        // 设置聊天模型和默认的拦截器
        return ChatClient.builder(chatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(myLoggerAdvisor, new MessageChatMemoryAdvisor(mongoChatMemory))
                .build();

    }

    /**
     * 创建并配置DashScopeChatModel Bean 千问模型
     * @param dashscopeChatModel
     * @param myLoggerAdvisor
     * @param mongoChatMemory
     * @param SYSTEM_PROMPT
     * @return
     */
    @Bean
    public ChatClient chatClientQwen(@Qualifier("dashscopeChatModel") DashScopeChatModel dashscopeChatModel, MyLoggerAdvisor myLoggerAdvisor, MongoChatMemory mongoChatMemory, @Value("${knowledge-agent.system-prompt}") String SYSTEM_PROMPT) {

        // 使用建造者模式创建ChatClient实例
        // 设置聊天模型和默认的拦截器
        return ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)  // 设置系统提示词
                .defaultAdvisors(myLoggerAdvisor, new MessageChatMemoryAdvisor(mongoChatMemory))  // 添加日志和记忆拦截器
                .build();  // 构建并返回ChatClient实例

    }
}
