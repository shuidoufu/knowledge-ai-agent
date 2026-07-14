package com.example.aiagent.controller;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 图片代理接口
 * 后端下载原始图片并返回，** 携带完整浏览器请求头绕过防盗链 **
 * 前端 Markdown 中的图片 URL 可通过此接口代理加载。
 */
@Slf4j
@RestController
@RequestMapping("/image-proxy")
public class ImageProxyController {

    private static final int TIMEOUT_MILLIS = 15000;

    /**
     * 代理下载图片
     * @param url 原始图片 URL
     * @return 图片字节流
     */
    @GetMapping(produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> proxyImage(@RequestParam("url") String url) {
        if (url == null || url.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            HttpResponse response = HttpRequest.get(url)
                    .setFollowRedirects(true)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                            "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Referer", "https://www.bing.com/")
                    .header("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
                    .timeout(TIMEOUT_MILLIS)
                    .execute();

            if (response.getStatus() != 200) {
                log.warn("Image proxy failed: {} status {}", url, response.getStatus());
                return ResponseEntity.status(response.getStatus()).build();
            }

            byte[] body = response.bodyBytes();
            if (body == null || body.length == 0) {
                return ResponseEntity.noContent().build();
            }

            // 根据响应头判断图片类型
            String contentType = response.header("Content-Type");
            MediaType mediaType = MediaType.IMAGE_JPEG;
            if (contentType != null) {
                try {
                    mediaType = MediaType.parseMediaType(contentType);
                } catch (Exception e) {
                    // 默认 JPEG
                }
            }

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .body(body);

        } catch (Exception e) {
            log.warn("Image proxy error: {} - {}", url, e.getMessage());
            return ResponseEntity.status(502).build();
        }
    }
}
