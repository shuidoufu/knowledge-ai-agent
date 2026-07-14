package com.example.aiagent.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 静态资源文件服务配置
 * 将 /files/** 的请求映射到本地 tmp/ 目录，使 AI 生成的图片和下载的文件
 * 可通过 HTTP 访问，以便在前端展示或在 PDF 中引用。
 * 
 * 映射规则：
 *   /api/files/download/filename  → {user.dir}/tmp/download/filename
 *   /api/files/pdf/filename       → {user.dir}/tmp/pdf/filename
 *   /api/files/file/filename      → {user.dir}/tmp/file/filename
 */
@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String tmpDir = System.getProperty("user.dir") + "/tmp/";
        // 将 file: 协议路径中的反斜杠替换为正斜杠，兼容 Windows
        String resourceLocation = "file:" + tmpDir.replace("\\", "/");

        registry.addResourceHandler("/files/**")
                .addResourceLocations(resourceLocation);
    }
}
