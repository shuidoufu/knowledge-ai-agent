package com.example.aiagent.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * 工作流工具：将预设流程暴露给 AI 调用
 */
@Slf4j
public class WorkflowTool {

    private final WorkflowEngine workflowEngine;

    public WorkflowTool(WorkflowEngine workflowEngine) {
        this.workflowEngine = workflowEngine;
    }

    @Tool(description = "按预设工作流执行多步骤任务。当用户请求包含多个固定步骤（如搜索资料/图片后生成PDF等）时，" +
            "必须使用本工具完成，不要分别调用 searchImages、generatePDF 等单个工具。" +
            "可用工作流：pdf_report（搜索→抓取→生成PDF）、image_album（搜图→生成PDF）")
    public String executeWorkflow(
            @ToolParam(description = "工作流名称，可选值：pdf_report, image_album") String workflowName,
            @ToolParam(description = "用户的需求描述，作为流程的输入参数") String query) {
        log.info("调用工作流: workflowName={}, query={}", workflowName, query);
        return workflowEngine.execute(workflowName, query);
    }
}
