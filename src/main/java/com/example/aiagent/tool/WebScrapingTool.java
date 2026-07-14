package com.example.aiagent.tool;

import cn.hutool.core.util.StrUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 网页内容抓取工具
 * 使用 Jsoup 抓取网页内容，支持多种输出格式：原始 HTML、纯文本、结构化摘要。
 * 与 WebSearchTool（搜索）、ResourceDownloadTool（下载）配合使用，完成搜索→抓取→保存的完整流程。
 */
public class WebScrapingTool {

    private static final int DEFAULT_TIMEOUT = 15000;
    private static final int MAX_CONTENT_LENGTH = 10000;

    /**
     * 抓取网页的原始 HTML 内容
     *
     * @param url 网页 URL
     * @return 网页 HTML 源码
     */
    @Tool(description = "Get the full HTML source of a web page. Returns raw HTML.")
    public String scrapeWebPage(
            @ToolParam(description = "URL of the web page to scrape") String url) {
        return scrapeUrl(url, "html", 0);
    }

    /**
     * 抓取网页的纯文本内容（去除 HTML 标签）
     *
     * @param url 网页 URL
     * @return 网页的纯文本内容
     */
    @Tool(description = "Extract clean text content from a web page (HTML tags removed). Best for AI reading.")
    public String scrapeWebPageText(
            @ToolParam(description = "URL of the web page to scrape") String url) {
        return scrapeUrl(url, "text", MAX_CONTENT_LENGTH);
    }

    /**
     * 抓取网页并提取结构化摘要（标题、描述、正文预览、链接等）
     *
     * @param url 网页 URL
     * @return 结构化网页摘要
     */
    @Tool(description = "Extract a structured summary from a web page: title, meta description, headings, links, and main text preview.")
    public String scrapeWebPageSummary(
            @ToolParam(description = "URL of the web page to scrape") String url) {
        return scrapeUrl(url, "summary", MAX_CONTENT_LENGTH);
    }

    /**
     * 抓取并提取网页中的指定元素
     *
     * @param url      网页 URL
     * @param selector CSS 选择器（如 "h1", "p", "article", ".content", "#main"）
     * @return 匹配元素的内容
     */
    @Tool(description = "Extract specific elements from a web page using a CSS selector. Examples: 'h1', 'p', 'article', '.content', '#main'")
    public String scrapeWebPageElements(
            @ToolParam(description = "URL of the web page to scrape") String url,
            @ToolParam(description = "CSS selector to target elements (e.g. 'h1', 'p', 'article', '.content')") String selector) {
        return scrapeUrl(url, "selector:" + selector, MAX_CONTENT_LENGTH);
    }

    /**
     * 统一抓取入口
     */
    private String scrapeUrl(String url, String mode, int maxLength) {
        if (StrUtil.isBlank(url)) {
            return "URL cannot be empty";
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return "Invalid URL: must start with http:// or https://";
        }

        try {
            Document doc = Jsoup.connect(url)
                    .timeout(DEFAULT_TIMEOUT)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .followRedirects(true)
                    .get();

            return switch (mode) {
                case "html" -> formatHtml(doc);
                case "text" -> formatText(doc, maxLength);
                case "summary" -> formatSummary(doc, maxLength);
                default -> {
                    if (mode.startsWith("selector:")) {
                        String selector = mode.substring(9);
                        yield formatSelector(doc, selector, maxLength);
                    }
                    yield "Unknown mode: " + mode;
                }
            };

        } catch (IOException e) {
            return "Error scraping " + url + ": " + e.getMessage();
        } catch (Exception e) {
            return "Unexpected error scraping " + url + ": " + e.getMessage();
        }
    }

    /** 格式化原始 HTML */
    private String formatHtml(Document doc) {
        StringBuilder sb = new StringBuilder();
        sb.append("Title: ").append(doc.title()).append("\n");
        sb.append("URL: ").append(doc.location()).append("\n");
        sb.append("Content length: ").append(doc.html().length()).append(" chars\n\n");
        sb.append(doc.html());
        return sb.toString();
    }

    /** 格式化纯文本 */
    private String formatText(Document doc, int maxLength) {
        String text = doc.body().text();
        text = text.replaceAll("\\s+", " ").trim();
        if (text.length() > maxLength) {
            text = text.substring(0, maxLength) + "...\n[Content truncated at " + maxLength + " chars]";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Title: ").append(doc.title()).append("\n");
        sb.append("URL: ").append(doc.location()).append("\n\n");
        sb.append(text);
        return sb.toString();
    }

    /** 格式化结构化摘要 */
    private String formatSummary(Document doc, int maxLength) {
        StringBuilder sb = new StringBuilder();
        sb.append("Title: ").append(doc.title()).append("\n");
        sb.append("URL: ").append(doc.location()).append("\n");

        // Meta description
        Element metaDesc = doc.selectFirst("meta[name=description]");
        if (metaDesc != null) {
            String desc = metaDesc.attr("content");
            if (StrUtil.isNotBlank(desc)) {
                sb.append("Description: ").append(desc).append("\n");
            }
        }

        // Meta keywords
        Element metaKeywords = doc.selectFirst("meta[name=keywords]");
        if (metaKeywords != null) {
            String kw = metaKeywords.attr("content");
            if (StrUtil.isNotBlank(kw)) {
                sb.append("Keywords: ").append(kw).append("\n");
            }
        }

        sb.append("\n--- Headings ---\n");
        Elements headings = doc.select("h1, h2, h3");
        for (Element h : headings) {
            sb.append(h.tagName()).append(": ").append(h.text()).append("\n");
        }

        sb.append("\n--- Links ---\n");
        Elements links = doc.select("a[href]");
        int linkCount = 0;
        for (Element a : links) {
            String href = a.attr("abs:href");
            String text = a.text();
            if (StrUtil.isNotBlank(text) && href.startsWith("http")) {
                sb.append("- ").append(text).append(" -> ").append(href).append("\n");
                linkCount++;
                if (linkCount >= 20) break;
            }
        }

        sb.append("\n--- Main Text ---\n");
        // 优先取 article 或 main 标签，否则取 body
        Element mainContent = doc.selectFirst("article, main, .content, #content, .post, .article");
        if (mainContent == null) {
            mainContent = doc.body();
        }
        String text = mainContent.text().replaceAll("\\s+", " ").trim();
        if (text.length() > maxLength) {
            text = text.substring(0, maxLength) + "...\n[Content truncated at " + maxLength + " chars]";
        }
        sb.append(text);

        return sb.toString();
    }

    /** 按 CSS 选择器提取元素 */
    private String formatSelector(Document doc, String selector, int maxLength) {
        Elements elements = doc.select(selector);
        if (elements.isEmpty()) {
            return "No elements found for selector: " + selector;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Found ").append(elements.size()).append(" element(s) for '").append(selector).append("':\n\n");

        int count = 0;
        for (Element el : elements) {
            String text = el.text().replaceAll("\\s+", " ").trim();
            if (StrUtil.isBlank(text)) continue;

            count++;
            // 如果是大段内容，截断
            if (text.length() > 500) {
                text = text.substring(0, 500) + "...";
            }
            sb.append(count).append(". ").append(text).append("\n\n");

            if (count >= 20) {
                sb.append("... and ").append(elements.size() - 20).append(" more elements\n");
                break;
            }
        }

        return sb.toString();
    }
}
