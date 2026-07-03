package com.example.aiagent.config;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MCP 后备配置——当 MCP 客户端被禁用时，
 * 提供一个空的 ToolCallbackProvider bean，
 * 避免因 MCP 未启用导致 @Resource 注入失败。
 */
@Configuration
public class McpFallbackConfig {

    @Bean
    @ConditionalOnMissingBean(ToolCallbackProvider.class)
    public ToolCallbackProvider toolCallbackProvider() {
        return ToolCallbackProvider.from();
    }
}
