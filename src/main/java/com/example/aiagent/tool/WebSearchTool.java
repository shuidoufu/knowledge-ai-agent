package com.example.aiagent.tool;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * 联网搜索工具
 * 使用百度智能云 AppBuilder 搜索 API，支持联网搜索并返回结构化结果。
 * 通过 baidu.api-key 配置 API 密钥（AppBuilder 应用的 API Key）。
 * 
 * API 文档：https://ai.baidu.com/ai-doc/AppBuilder/pmaxd1hvy
 */
public class WebSearchTool {

    private static final String SEARCH_API_URL = "https://qianfan.baidubce.com/v2/ai_search/web_search";
    private static final int DEFAULT_TIMEOUT = 20000;

    private final String apiKey;

    /**
     * @param apiKey 百度智能云 AppBuilder API Key（对应配置文件的 baidu.api-key）
     */
    public WebSearchTool(String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * 联网搜索（百度，默认 5 条结果）
     *
     * @param query 搜索关键词
     * @return 格式化的搜索结果列表
     */
    @Tool(description = "Search the web using Baidu AppBuilder search engine. Returns formatted results with title, link and snippet.")
    public String searchWeb(
            @ToolParam(description = "Search query keyword") String query) {
        return searchBaidu(query, 5);
    }

    /**
     * 联网搜索（指定条数）
     *
     * @param query  搜索关键词
     * @param count  返回结果数量（1-10）
     * @return 格式化的搜索结果
     */
    @Tool(description = "Search the web with specified result count. Returns formatted results with title, link and snippet.")
    public String searchWebAdvanced(
            @ToolParam(description = "Search query keyword") String query,
            @ToolParam(description = "Number of results to return (1-10)") int count) {

        if (count < 1) count = 1;
        if (count > 10) count = 10;

        return searchBaidu(query, count);
    }

    /**
     * 执行百度 AppBuilder 搜索
     */
    private String searchBaidu(String query, int count) {
        if (StrUtil.isBlank(apiKey)) {
            return "Baidu API key is not configured. Please set 'baidu.api-key' in application.yml.";
        }
        if (StrUtil.isBlank(query)) {
            return "Search query cannot be empty";
        }

        try {
            // 构造请求体
            JSONObject body = new JSONObject();

            // messages 参数
            JSONArray messages = new JSONArray();
            JSONObject userMsg = new JSONObject();
            userMsg.set("role", "user");
            userMsg.set("content", query.trim());
            messages.add(userMsg);
            body.set("messages", messages);

            // 搜索配置
            body.set("search_source", "baidu_search_v2");

            // 资源类型过滤
            JSONArray resourceFilter = new JSONArray();
            JSONObject webFilter = new JSONObject();
            webFilter.set("type", "web");
            webFilter.set("top_k", count);
            resourceFilter.add(webFilter);
            body.set("resource_type_filter", resourceFilter);

            // 发送 POST 请求
            HttpResponse response = HttpRequest.post(SEARCH_API_URL)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(DEFAULT_TIMEOUT)
                    .body(body.toString())
                    .execute();

            int statusCode = response.getStatus();
            String responseBody = response.body();

            if (StrUtil.isBlank(responseBody)) {
                return "Search API returned empty response.";
            }

            JSONObject json = JSONUtil.parseObj(responseBody);

            // 检查错误响应
            if (statusCode != 200 || json.containsKey("code")) {
                String errCode = json.getStr("code", String.valueOf(statusCode));
                String errMsg = json.getStr("message", "Unknown error");
                return "Search API error (" + errCode + "): " + errMsg;
            }

            // 提取搜索结果
            JSONArray references = json.getJSONArray("references");
            if (references == null || references.isEmpty()) {
                return "No search results found for: " + query;
            }

            // 格式化结果
            StringBuilder sb = new StringBuilder();
            sb.append("Search results for \"").append(query).append("\":\n\n");

            int limit = Math.min(count, references.size());
            for (int i = 0; i < limit; i++) {
                JSONObject item = references.getJSONObject(i);

                String title = item.getStr("title", "");
                String link = item.getStr("url", "");
                String snippet = item.getStr("content", "");

                if (StrUtil.isBlank(title)) continue;

                sb.append(i + 1).append(". ").append(title.trim()).append("\n");
                if (StrUtil.isNotBlank(link)) {
                    sb.append("   Link: ").append(link).append("\n");
                }
                if (StrUtil.isNotBlank(snippet)) {
                    String trimmed = snippet.trim();
                    if (trimmed.length() > 300) {
                        trimmed = trimmed.substring(0, 300) + "...";
                    }
                    sb.append("   ").append(trimmed).append("\n");
                }
                sb.append("\n");
            }

            return sb.toString();

        } catch (Exception e) {
            return "Search failed: " + e.getMessage();
        }
    }
}
