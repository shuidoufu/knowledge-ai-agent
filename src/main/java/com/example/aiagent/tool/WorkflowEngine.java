package com.example.aiagent.tool;

import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 工作流引擎：按预设流程串行执行多个工具调用，支持 SPEL 表达式进行步骤间数据传递
 * 步骤参数用 {表达式} 占位符模板（SPEL 变量引用自动补 # 前缀），工具输出经 JSON 还原后作为步骤变量，
 * 通过注册的 SPEL 函数提取字段（toImages 提取图片URL、extractUrl 提取首个URL）
 */
@Slf4j
public class WorkflowEngine {

    private final ToolCallback[] allTools;
    private final ExpressionParser parser = new SpelExpressionParser();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 模板占位符匹配：{表达式} */
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([^{}]+)\\}");

    /** 匹配搜索结果文本中的图片 URL 行（如 "   URL: https://..."） */
    private static final Pattern IMAGE_URL_PATTERN = Pattern.compile("(?m)^\\s*URL:\\s*(\\S+)");

    /** 匹配文本中第一个 http(s) URL */
    private static final Pattern FIRST_URL_PATTERN = Pattern.compile("https?://\\S+");

    /** 流程定义：按名称索引 */
    private final Map<String, WorkflowDefinition> workflows = new LinkedHashMap<>();

    public WorkflowEngine(ToolCallback[] allTools) {
        this.allTools = allTools;
        // 注册预设流程
        workflows.put("pdf_report", WorkflowDefinition.builder()
                .name("生成含图片的 PDF 报告")
                .description("搜索资料 → 搜索图片 → 抓取详情 → 生成PDF")
                .steps(List.of(
                        new WorkflowStep("searchWeb", Map.of("query", "{query}"), "searchResult"),
                        new WorkflowStep("searchImages", Map.of("query", "{query}", "count", "10"), "imageUrls"),
                        new WorkflowStep("scrapeWebPageText", Map.of("url", "{extractUrl(#searchResult)}"), "webContent"),
                        new WorkflowStep("generatePDF",
                                Map.of("fileName", "{query}.pdf",
                                        "content", "{query} 调研报告\n\n{webContent}\n\n{toImages(#imageUrls)}"),
                                "pdfFile")
                ))
                .build());

        workflows.put("image_album", WorkflowDefinition.builder()
                .name("图片专辑")
                .description("搜索图片 → 生成PDF")
                .steps(List.of(
                        new WorkflowStep("searchImages", Map.of("query", "{query}", "count", "10"), "imageUrls"),
                        new WorkflowStep("generatePDF",
                                Map.of("fileName", "{query}.pdf",
                                        "content", "{query}\n\n{toImages(#imageUrls)}"),
                                "pdfFile")
                ))
                .build());
    }

    /**
     * 执行指定工作流
     *
     * @param workflowName 工作流名称
     * @param query        用户需求描述，作为流程的输入参数
     * @return 工作流执行结果
     */
    public String execute(String workflowName, String query) {
        WorkflowDefinition wf = workflows.get(workflowName);
        if (wf == null) {
            return "未知工作流：" + workflowName;
        }

        StandardEvaluationContext ctx = new StandardEvaluationContext();
        ctx.setVariable("query", query);
        registerFunctions(ctx);

        log.info("开始执行工作流 [{}]: {}", wf.name(), query);

        for (int i = 0; i < wf.steps().size(); i++) {
            WorkflowStep step = wf.steps().get(i);
            ToolCallback tool = findTool(step.toolName());
            if (tool == null) {
                log.warn("工作流 [{}] 步骤 {}: 工具 {} 未找到，跳过", wf.name(), i, step.toolName());
                continue;
            }

            // 解析每个参数表达式，构建 JSON 参数对象后调用工具（多参数工具需 JSON 对象形式）
            Map<String, Object> args = new LinkedHashMap<>();
            for (Map.Entry<String, String> entry : step.params().entrySet()) {
                args.put(entry.getKey(), resolveTemplate(entry.getValue(), ctx));
            }
            String output = normalizeToolOutput(tool.call(JSONUtil.toJsonStr(args)));
            ctx.setVariable(step.outputKey(), output);
            log.info("工作流 [{}] 步骤 {}/{}: {} 执行完成", wf.name(), i + 1, wf.steps().size(), step.toolName());
        }

        String lastKey = wf.steps().get(wf.steps().size() - 1).outputKey();
        return ctx.lookupVariable(lastKey) != null ? ctx.lookupVariable(lastKey).toString() : "执行完成";
    }

    public List<String> getWorkflowNames() {
        return List.copyOf(workflows.keySet());
    }

    /**
     * 还原工具输出：将工具返回的 JSON 字符串字面量还原为纯文本（仅处理首尾引号的字符串形态，JSON 对象/数组保持原样）
     *
     * @param rawOutput 工具原始返回值
     * @return 还原后的文本
     */
    private String normalizeToolOutput(String rawOutput) {
        if (rawOutput == null || !rawOutput.startsWith("\"")) {
            return rawOutput;
        }
        try {
            return objectMapper.readValue(rawOutput, String.class);
        } catch (Exception e) {
            log.warn("工具输出 JSON 字符串还原失败，使用原文: {}", e.getMessage());
            return rawOutput;
        }
    }

    /**
     * 注册 SPEL 辅助函数（toImages、extractUrl）
     *
     * @param ctx SPEL 上下文
     */
    private void registerFunctions(StandardEvaluationContext ctx) {
        try {
            ctx.registerFunction("toImages", WorkflowEngine.class.getMethod("toImages", String.class));
            ctx.registerFunction("extractUrl", WorkflowEngine.class.getMethod("extractUrl", String.class));
        } catch (NoSuchMethodException e) {
            log.warn("SPEL 函数注册失败", e);
        }
    }

    /**
     * 解析模板：替换所有 {表达式} 占位符为 SPEL 求值结果（自动补 # 前缀）
     *
     * @param template 模板文本
     * @param ctx      SPEL 上下文
     * @return 替换后的文本
     */
    private String resolveTemplate(String template, StandardEvaluationContext ctx) {
        if (template == null || !template.contains("{")) {
            return template;
        }
        Matcher m = PLACEHOLDER_PATTERN.matcher(template);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String expr = m.group(1);
            if (!expr.startsWith("#")) {
                expr = "#" + expr;
            }
            String value;
            try {
                value = parser.parseExpression(expr).getValue(ctx, String.class);
            } catch (Exception e) {
                log.warn("SPEL 解析失败: {}", expr, e);
                value = m.group(0);
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * SPEL 函数：从搜索结果文本中提取图片 URL，转为 Markdown 图片语法（供 PDF 渲染图片）
     *
     * @param searchText 搜索结果文本
     * @return Markdown 图片列表（如 ![图1](url)），无 URL 时返回原文
     */
    public static String toImages(String searchText) {
        Matcher m = IMAGE_URL_PATTERN.matcher(searchText);
        StringBuilder sb = new StringBuilder();
        int i = 1;
        while (m.find() && i <= 5) {
            sb.append("![图").append(i).append("](").append(m.group(1)).append(")\n");
            i++;
        }
        return sb.length() > 0 ? sb.toString().trim() : searchText;
    }

    /**
     * SPEL 函数：提取文本中第一个 http(s) URL
     *
     * @param text 文本
     * @return 第一个 URL，未找到时返回原文
     */
    public static String extractUrl(String text) {
        Matcher m = FIRST_URL_PATTERN.matcher(text);
        return m.find() ? m.group() : text;
    }

    private ToolCallback findTool(String name) {
        for (ToolCallback tc : allTools) {
            if (tc.getToolDefinition().name().equals(name)) {
                return tc;
            }
        }
        return null;
    }

    public record WorkflowDefinition(String name, String description, List<WorkflowStep> steps) {
        public static Builder builder() { return new Builder(); }
        public static class Builder {
            private String name; private String description; private List<WorkflowStep> steps;
            public Builder name(String name) { this.name = name; return this; }
            public Builder description(String description) { this.description = description; return this; }
            public Builder steps(List<WorkflowStep> steps) { this.steps = steps; return this; }
            public WorkflowDefinition build() { return new WorkflowDefinition(name, description, steps); }
        }
    }

    public record WorkflowStep(String toolName, Map<String, String> params, String outputKey) {}
}
