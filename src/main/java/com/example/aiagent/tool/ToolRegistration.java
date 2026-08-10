package com.example.aiagent.tool;

import com.example.aiagent.service.ImageProxyService;
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

    @Resource
    private ImageProxyService imageProxyService;

    @Bean
    public ToolCallback[] allTools() {
        FileOperationTool fileOperationTool = new FileOperationTool();

        WebScrapingTool webScrapingTool = new WebScrapingTool();
        ResourceDownloadTool resourceDownloadTool = new ResourceDownloadTool();
        PDFGenerationTool pdfGenerationTool = new PDFGenerationTool(imageProxyService);
        WebSearchTool webSearchTool = new WebSearchTool(baiduApiKey);
        ImageSearchTool imageSearchTool = new ImageSearchTool();
        TerminateTool terminateTool = new TerminateTool();

        // 先构建基础工具数组
        ToolCallback[] baseTools = ToolCallbacks.from(
                fileOperationTool, webScrapingTool, resourceDownloadTool,
                pdfGenerationTool, webSearchTool, imageSearchTool, terminateTool
        );

        // 手动创建工作流引擎（避免循环依赖）
        WorkflowEngine workflowEngine = new WorkflowEngine(baseTools);
        WorkflowTool workflowTool = new WorkflowTool(workflowEngine);

        // 合并工作流工具到最终数组
        ToolCallback[] workflowTools = ToolCallbacks.from(workflowTool);
        ToolCallback[] allTools = new ToolCallback[baseTools.length + workflowTools.length];
        System.arraycopy(baseTools, 0, allTools, 0, baseTools.length);
        System.arraycopy(workflowTools, 0, allTools, baseTools.length, workflowTools.length);
        return allTools;
    }

    // AI调用MCP服务
    @Resource
    private ToolCallbackProvider toolCallbackProvider;
}
