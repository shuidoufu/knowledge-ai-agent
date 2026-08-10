package com.example.aiagent.controller;

import com.example.aiagent.service.ImageProxyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
     * @param url 原始图片 URL
     * @return 图片字节流
     */
    @GetMapping(produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> proxyImage(@RequestParam("url") String url) {
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

        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(result.bytes());
    }

}
