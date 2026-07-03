package com.example.aiagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置（密钥、过期时间等）
 */
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /** 签名密钥，生产环境请使用环境变量或密钥管理 */
    private String secret = "ai-agent-default-secret-change-in-production";

    /** 过期时间（秒），默认 7 天 */
    private long expirationSeconds = 7 * 24 * 3600;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    public void setExpirationSeconds(long expirationSeconds) {
        this.expirationSeconds = expirationSeconds;
    }
}
