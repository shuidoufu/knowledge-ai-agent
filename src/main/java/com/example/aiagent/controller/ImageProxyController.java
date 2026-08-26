package com.example.aiagent.controller;

import com.example.aiagent.service.ImageProxyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 图片代理接口
 * 通过 ImageProxyService 下载原始图片并返回（携带完整浏览器请求头绕过防盗链）
 * 前端 Markdown 中的图片 URL 可通过此接口代理加载
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/image-proxy")
public class ImageProxyController {

    private final ImageProxyService imageProxyService;

    /**
     * 代理下载图片
     *
     * @param url      原始图片 URL
     * @param download 是否作为附件下载（移动端长按保存图片使用）
     * @return 图片字节流
     */
    @GetMapping(produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> proxyImage(
            @RequestParam("url") String url,
            @RequestParam(value = "download", defaultValue = "false") boolean download) {
        if (url == null || url.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        // 前端展示路径使用单次尝试，避免多策略重试的叠加延迟
        ImageProxyService.ImageFetchResult result = imageProxyService.fetchOnce(url);
        if (result == null) {
            log.warn("Image proxy failed: {}", url);
            return ResponseEntity.status(502).build();
        }

        // 根据图片格式设置响应类型
        MediaType mediaType = switch (result.format()) {
            case "png" -> MediaType.IMAGE_PNG;
            case "gif" -> MediaType.IMAGE_GIF;
            case "webp" -> MediaType.parseMediaType("image/webp");
            case "bmp" -> MediaType.parseMediaType("image/bmp");
            case "svg" -> MediaType.parseMediaType("image/svg+xml");
            case "avif" -> MediaType.parseMediaType("image/avif");
            default -> MediaType.IMAGE_JPEG;
        };

        ResponseEntity.BodyBuilder builder = ResponseEntity.ok().contentType(mediaType);
        if (download) {
            builder.header(HttpHeaders.CONTENT_DISPOSITION,
                    buildContentDisposition(buildFileName(url, result.format())));
        }
        return builder.body(result.bytes());
    }

    /**
     * 从原始图片 URL 提取文件名（含扩展名），非法字符替换为下划线；无法提取时用时间戳命名。
     */
    private String buildFileName(String url, String format) {
        // Spring 已对 query 解码一次，此处直接用 URL 的路径段（不二次解码，避免 %XX 被错误展开）
        String ext = format == null || format.isBlank() ? "jpg" : format;
        // 取路径最后一段作为文件名（去掉 query/fragment）
        String path = url;
        int q = path.indexOf('?');
        if (q >= 0) path = path.substring(0, q);
        String name = path.substring(path.lastIndexOf('/') + 1);
        if (name.isBlank() || !name.contains(".")) {
            name = "image_" + System.currentTimeMillis() + "." + ext;
        }
        // 清洗 Windows/URL 非法字符
        name = name.replaceAll("[\\\\/:*?\"<>|\\s]", "_");
        return name;
    }

    /**
     * 构造 Content-Disposition（ASCII 回退名 + RFC 5987 中文名）。
     */
    private String buildContentDisposition(String fileName) {
        String fallback = fileName.replaceAll("[^A-Za-z0-9._-]", "_");
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        return "attachment; filename=\"" + fallback + "\"; filename*=UTF-8''" + encoded;
    }

}
