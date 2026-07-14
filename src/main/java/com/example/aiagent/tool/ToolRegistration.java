package com.example.aiagent.tool;

import jakarta.annotation.Resource;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.ToolCallbacks;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 集中的工具注册类
 */
@Configuration
public class ToolRegistration {

    @Value("${baidu.api-key:}")
    private String baiduApiKey;

    @Bean
    public ToolCallback[] allTools() {
        FileOperationTool fileOperationTool = new FileOperationTool();

        WebScrapingTool webScrapingTool = new WebScrapingTool();
        ResourceDownloadTool resourceDownloadTool = new ResourceDownloadTool();
        PDFGenerationTool pdfGenerationTool = new PDFGenerationTool();
        WebSearchTool webSearchTool = new WebSearchTool(baiduApiKey);
        TerminateTool terminateTool = new TerminateTool();
        return ToolCallbacks.from(
                fileOperationTool,
                webScrapingTool,
                resourceDownloadTool,
                pdfGenerationTool,
                webSearchTool,
                terminateTool
        );
    }

    // AI调用MCP服务
    @Resource
    private ToolCallbackProvider toolCallbackProvider;
}
