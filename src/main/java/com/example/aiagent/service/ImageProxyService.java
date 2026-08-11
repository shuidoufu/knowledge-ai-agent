package com.example.aiagent.service;

import cn.hutool.http.HttpRequest;
import com.example.aiagent.constant.HotlinkImageConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URL;

/**
 * 图片代理服务：后端下载图片字节流（携带完整浏览器请求头，绕过站点防盗链）
 * 供 ImageProxyController（前端展示代理）与 PDFGenerationTool（PDF 插图下载）共用
 */
@Slf4j
@Service
public class ImageProxyService {

    /** 下载超时（毫秒） */
    private static final int TIMEOUT_MILLIS = 15000;

    /**
     * 图片下载结果
     *
     * @param bytes  图片字节流
     * @param format 图片格式（jpeg/png/gif/webp/bmp/unknown，按文件魔数识别）
     */
    public record ImageFetchResult(byte[] bytes, String format) {
    }

    /**
     * 下载图片：多策略 Referer 重试（Bing → 无 → 图片域名自身），携带完整浏览器请求头
     *
     * @param url 图片 URL
     * @return 下载结果，全部失败返回 null
     */
    public ImageFetchResult fetch(String url) {
        String[] referers = {
                "https://www.bing.com/",
                "",
                extractDomain(url)
        };
        for (String referer : referers) {
            ImageFetchResult result = tryDownload(url, referer);
            if (result != null) {
                return result;
            }
        }
        log.warn("图片下载失败（多策略均被拒）: {}", url);
        return null;
    }

    /**
     * 下载图片：单次尝试（Bing Referer），供前端展示路径使用（避免多策略重试的叠加延迟）
     *
     * @param url 图片 URL
     * @return 下载结果，失败返回 null
     */
    public ImageFetchResult fetchOnce(String url) {
        return tryDownload(url, "https://www.bing.com/");
    }

    /**
     * 用指定 Referer 尝试下载图片
     * 命中防盗链黑名单的 URL 直接拒绝（与搜索过滤共用 HotlinkImageConfig）
     *
     * @param url     图片 URL
     * @param referer Referer 头（空字符串表示不携带）
     * @return 下载结果，失败返回 null
     */
    private ImageFetchResult tryDownload(String url, String referer) {
        if (HotlinkImageConfig.isBlockedUrl(url)) {
            log.warn("图片 URL 命中防盗链黑名单，拒绝下载: {}", url);
            return null;
        }
        try {
            HttpRequest req = HttpRequest.get(url)
                    .setFollowRedirects(true)
                    // 完整浏览器请求头（sec-* 头是主流防盗链校验的关键特征）
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                            "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .header("Sec-Ch-Ua", "\"Not_A Brand\";v=\"8\", \"Chromium\";v=\"120\", \"Google Chrome\";v=\"120\"")
                    .header("Sec-Ch-Ua-Mobile", "?0")
                    .header("Sec-Ch-Ua-Platform", "\"Windows\"")
                    .header("Sec-Fetch-Dest", "image")
                    .header("Sec-Fetch-Mode", "no-cors")
                    .header("Sec-Fetch-Site", "cross-site")
                    .timeout(TIMEOUT_MILLIS);
            if (referer != null && !referer.isBlank()) {
                req.header("Referer", referer);
            }

            byte[] bytes = req.execute().bodyBytes();
            if (bytes == null || bytes.length == 0) {
                return null;
            }
            String format = detectImageFormat(bytes);
            if ("unknown".equals(format)) {
                log.warn("下载内容不是有效图片（可能是防盗链页面）: {}", url);
                return null;
            }
            log.info("图片下载成功: {} ({} bytes, {})", url, bytes.length, format);
            return new ImageFetchResult(bytes, format);
        } catch (Exception e) {
            log.warn("图片下载异常: {} - {}", url, e.getMessage());
            return null;
        }
    }

    /**
     * 通过文件魔数识别图片格式
     *
     * @param header 图片字节流
     * @return 格式（jpeg/png/gif/webp/bmp/svg/avif/unknown）
     */
    public String detectImageFormat(byte[] header) {
        if (header.length < 4) return "unknown";
        // JPEG: FF D8 FF
        if (header[0] == (byte) 0xFF && header[1] == (byte) 0xD8 && header[2] == (byte) 0xFF) return "jpeg";
        // PNG: 89 50 4E 47
        if (header[0] == (byte) 0x89 && header[1] == (byte) 0x50 && header[2] == (byte) 0x4E && header[3] == (byte) 0x47) return "png";
        // GIF: 47 49 46 38
        if (header[0] == (byte) 0x47 && header[1] == (byte) 0x49 && header[2] == (byte) 0x46 && header[3] == (byte) 0x38) return "gif";
        // WEBP: 52 49 46 46 (RIFF)
        if (header[0] == (byte) 0x52 && header[1] == (byte) 0x49 && header[2] == (byte) 0x46 && header[3] == (byte) 0x46) return "webp";
        // BMP: 42 4D
        if (header[0] == (byte) 0x42 && header[1] == (byte) 0x4D) return "bmp";
        // SVG: 前导空白/BOM 后以 <svg 或 <?xml 开头
        int i = 0;
        while (i < header.length && (header[i] == ' ' || header[i] == '\t' || header[i] == '\n' || header[i] == '\r'
                || (header[i] & 0xFF) == 0xEF || (header[i] & 0xFF) == 0xBB || (header[i] & 0xFF) == 0xBF)) {
            i++;
        }
        if (i + 4 <= header.length && (startsWithAscii(header, i, "<svg") || startsWithAscii(header, i, "<?xml"))) {
            return "svg";
        }
        // AVIF: ISO-BMFF 容器（offset 4 为 ftyp，brand 为 avif/avis）
        if (header.length >= 12 && header[4] == 'f' && header[5] == 't' && header[6] == 'y' && header[7] == 'p') {
            String brand = new String(header, 8, 4, java.nio.charset.StandardCharsets.US_ASCII);
            if ("avif".equals(brand) || "avis".equals(brand)) {
                return "avif";
            }
        }
        return "unknown";
    }

    /**
     * 判断字节流从指定位置开始是否与给定 ASCII 字符串匹配
     *
     * @param bytes 字节流
     * @param from  起始位置
     * @param ascii 目标字符串
     * @return 是否匹配
     */
    private boolean startsWithAscii(byte[] bytes, int from, String ascii) {
        if (from + ascii.length() > bytes.length) {
            return false;
        }
        for (int k = 0; k < ascii.length(); k++) {
            if (bytes[from + k] != (byte) ascii.charAt(k)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 从 URL 提取域名作为备选 Referer
     *
     * @param url 图片 URL
     * @return 域名（如 https://example.com/），解析失败返回空字符串
     */
    private String extractDomain(String url) {
        try {
            URL u = new URL(url);
            return u.getProtocol() + "://" + u.getHost() + "/";
        } catch (Exception e) {
            return "";
        }
    }

}
