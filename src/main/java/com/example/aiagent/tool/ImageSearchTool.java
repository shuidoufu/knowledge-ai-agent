package com.example.aiagent.tool;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.example.aiagent.constant.HotlinkImageConfig;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 图片搜索工具
 * 为 AI Agent 提供联网图片搜索能力，支持根据关键词搜索网络图片资源。
 * 搜索到的图片可以直接在对话中通过 Markdown 语法展示，或插入到 PDF 文档中。
 * 使用 Bing Image Search 作为搜索源，无需额外 API Key。
 * 
 * 注意：Bing 有反爬机制，可能返回 302 重定向或验证页面。
 * 本工具通过手动处理重定向链和模拟浏览器请求来规避。
 */
@Slf4j
public class ImageSearchTool {

    /** Bing 图片搜索地址 */
    private static final String BING_IMAGE_SEARCH_URL = "https://www.bing.com/images/search";

    /** 请求超时时间 */
    private static final int TIMEOUT_MILLIS = 20000;

    /** 浏览器 User-Agent */
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    /** 最多跟随重定向次数 */
    private static final int MAX_REDIRECTS = 5;

    /**
     * 搜索图片
     * 根据关键词从 Bing Image Search 搜索图片，返回图片 URL、标题和来源信息。
     * 
     * @param query 搜索关键词
     * @param count 返回结果数量（1-20，默认 10）
     * @return 格式化的图片搜索结果列表
     */
    @Tool(description = "Search images on the web. Returns image URLs, titles and source pages.")
    public String searchImages(
            @ToolParam(description = "Search query for images (e.g. 'cat', 'beach sunset', 'city skyline')") String query,
            @ToolParam(description = "Number of image results to return (1-20, default 10)") int count) {

        if (StrUtil.isBlank(query)) {
            return "Error: Search query cannot be empty";
        }
        if (count < 1) count = 1;
        if (count > 20) count = 20;

        try {
            List<ImageResult> results = searchBingImages(query, count);

            if (results.isEmpty()) {
                return "No image results found for: " + query;
            }

            // 格式化结果
            StringBuilder sb = new StringBuilder();
            sb.append("Image search results for \"").append(query).append("\":\n\n");

            int limit = Math.min(count, results.size());
            for (int i = 0; i < limit; i++) {
                ImageResult img = results.get(i);
                sb.append(i + 1).append(". ").append(img.title).append("\n");
                sb.append("   URL: ").append(img.imageUrl).append("\n");
                if (StrUtil.isNotBlank(img.sourceUrl)) {
                    sb.append("   Source: ").append(img.sourceUrl).append("\n");
                }
                sb.append("\n");
            }

            return sb.toString();

        } catch (Exception e) {
            log.error("Image search failed: query={}", query, e);
            return "Image search failed: " + e.getMessage();
        }
    }

    /**
     * 在 Bing Images 中搜索图片
     * 先关闭自动重定向以获取初始响应，遇到 302 则手动跟随到最终页。
     * 从 a.iusc[m] JSON 属性中提取原始高清图片 URL 和标题。
     */
    private List<ImageResult> searchBingImages(String query, int count) throws Exception {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        int perPage = Math.min(count + 10, 35);
        String url = BING_IMAGE_SEARCH_URL + "?q=" + encodedQuery + "&count=" + perPage + "&mkt=en-US&form=HDRSC3";

        String finalHtml = fetchWithRedirectHandling(url);
        if (StrUtil.isBlank(finalHtml)) {
            return new ArrayList<>();
        }

        Document doc = Jsoup.parse(finalHtml, url);
        Set<ImageResult> resultSet = new LinkedHashSet<>();

        // 方式一（主要）：提取 a.iusc[m] JSON 中的原始图片 URL
        // m 属性内容：{"murl":"原图URL","t":"标题","purl":"来源页URL",...}
        Elements iuscLinks = doc.select("a.iusc[m]");
        for (Element link : iuscLinks) {
            if (resultSet.size() >= count) break;
            String mJson = link.attr("m");
            if (StrUtil.isBlank(mJson)) continue;

            try {
                // 从 JSON 中提取 murl（原始来源 URL，不带 Bing 加的白边）
                String murl = extractJsonValue(mJson, "murl");
                if (StrUtil.isBlank(murl) || murl.startsWith("data:") || HotlinkImageConfig.isBlockedUrl(murl)) continue;

                // 获取标题
                String title = extractJsonValue(mJson, "t");
                if (StrUtil.isBlank(title)) title = "(image)";

                // 获取来源页 URL
                String purl = extractJsonValue(mJson, "purl");

                // 去重
                String finalUrl = murl;
                if (resultSet.stream().noneMatch(r -> r.imageUrl.equals(finalUrl))) {
                    resultSet.add(new ImageResult(title, murl, purl));
                }
            } catch (Exception e) {
                log.debug("Failed to parse iusc JSON", e);
            }
        }

        // 方式二（备选）：提取 img.mimg 缩略图（当 iusc 方式无效时）
        if (resultSet.size() < count) {
            Elements imgs = doc.select("img.mimg");
            for (Element img : imgs) {
                if (resultSet.size() >= count) break;
                String src = img.attr("src");
                if (StrUtil.isBlank(src) || src.startsWith("data:") || HotlinkImageConfig.isBlockedUrl(src)) continue;
                if (src.startsWith("//")) src = "https:" + src;

                // 过滤小图标
                String width = img.attr("width");
                if (StrUtil.isNotBlank(width)) {
                    try {
                        if (Integer.parseInt(width) < 100) continue;
                    } catch (NumberFormatException ignored) {}
                }

                // 去重
                String finalSrc = src;
                if (resultSet.stream().anyMatch(r -> r.imageUrl.equals(finalSrc))) continue;

                String title = img.attr("alt");
                if (StrUtil.isBlank(title)) title = "(image)";

                resultSet.add(new ImageResult(title, upscaleBingUrl(src), ""));
            }
        }

        List<ImageResult> results = new ArrayList<>(resultSet);
        log.info("Bing image search found {} results for: {}", results.size(), query);
        return results;
    }

    /**
     * 从简单 JSON 文本中提取指定字段的值（不依赖 JSON 库，使用字符串搜索）
     */
    private String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\":\"";
        int start = json.indexOf(searchKey);
        if (start < 0) return "";
        start += searchKey.length();
        int end = start;
        // 处理转义字符
        StringBuilder value = new StringBuilder();
        while (end < json.length()) {
            char c = json.charAt(end);
            if (c == '\\') {
                end++;
                if (end < json.length()) {
                    value.append(json.charAt(end));
                    end++;
                }
            } else if (c == '"') {
                break;
            } else {
                value.append(c);
                end++;
            }
        }
        return value.toString().replace("\\/", "/").replace("\\u0026", "&");
    }

    /**
     * 发起 HTTP GET 请求并手动处理重定向链，最终返回页面 HTML。
     * 最多跟随 MAX_REDIRECTS 次重定向。
     */
    private String fetchWithRedirectHandling(String url) {
        String currentUrl = url;

        for (int i = 0; i <= MAX_REDIRECTS; i++) {
            HttpResponse response = HttpRequest.get(currentUrl)
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .setFollowRedirects(false)
                    .timeout(TIMEOUT_MILLIS)
                    .execute();

            int statusCode = response.getStatus();

            if (statusCode >= 300 && statusCode < 400) {
                String location = response.header("Location");
                if (StrUtil.isBlank(location)) {
                    log.warn("Redirect with no Location header at step {}", i);
                    return null;
                }
                if (location.startsWith("/")) {
                    location = "https://www.bing.com" + location;
                } else if (!location.startsWith("http://") && !location.startsWith("https://")) {
                    int lastSlash = currentUrl.lastIndexOf('/');
                    if (lastSlash > 8) {
                        location = currentUrl.substring(0, lastSlash + 1) + location;
                    }
                }
                log.info("Following redirect {}: {} -> {}", i + 1, statusCode, location);
                currentUrl = location;
                continue;
            }

            if (statusCode == 200) {
                return response.body();
            }

            log.warn("Unexpected status {} at step {}", statusCode, i);
            return null;
        }

        log.warn("Too many redirects, giving up");
        return null;
    }

    /**
     * 将 Bing CDN 缩略图 URL 提升为高清大图
     * Bing CDN（tse*.bing.net）的缩略图 URL 带有尺寸参数（w=148&h=180），
     * 将这些参数改为更大的值即可获取高清版本。
     * 
     * 示例：
     *   https://tse1-mm.cn.bing.net/th/id/OIP-C.xxx?w=148&h=180 → w=1200&h=900
     */
    /**
     * 将 Bing 图片 URL 提升为高清可访问的格式。
     * turl 格式：ts*.mm.bing.net/th?id=OIP.xxx&pid=15.1
     * 需要转换为：tse*-mm.cn.bing.net/th/id/OIP-C.xxx?w=1200
     */
    private String upscaleBingUrl(String url) {
        if (StrUtil.isBlank(url) || url.contains("data:")) {
            return url;
        }
        // 处理 turl 格式：th?id=OIP.xxx → th/id/OIP-C.xxx
        if (url.contains("th?id=OIP.")) {
            String hash = url.replaceAll(".*th\\?id=OIP\\.([^&]+).*", "$1");
            if (StrUtil.isNotBlank(hash) && !hash.equals(url)) {
                // 提取数字前缀：ts1 → 1
                String num = url.replaceAll(".*ts(\\d+)\\.mm\\..*", "$1");
                return "https://tse" + num + "-mm.cn.bing.net/th/id/OIP-C." + hash + "?w=1200";
            }
        }
        // 旧版 tse 格式：只保留宽度参数
        if (url.contains("bing.net")) {
            return url.replaceAll("[?&]w=\\d+", "?w=1200")
                    .replaceAll("[&][h]=\\d+", "")
                    .replaceAll("[&]rm=\\d+", "");
        }
        return url;
    }

    /**
     * 图片搜索结果内部类
     * equals/hashCode 基于 imageUrl 实现去重
     */
    private static class ImageResult {
        final String title;
        final String imageUrl;
        final String sourceUrl;

        ImageResult(String title, String imageUrl, String sourceUrl) {
            this.title = title != null ? title : "";
            this.imageUrl = imageUrl != null ? imageUrl : "";
            this.sourceUrl = sourceUrl != null ? sourceUrl : "";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ImageResult)) return false;
            ImageResult that = (ImageResult) o;
            return imageUrl.equals(that.imageUrl);
        }

        @Override
        public int hashCode() {
            return imageUrl.hashCode();
        }
    }
}
