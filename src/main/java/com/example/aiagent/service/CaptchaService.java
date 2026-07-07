package com.example.aiagent.service;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 图片验证码服务：生成、存储、校验
 * 使用 Hutool CaptchaUtil 生成图片验证码，验证码存储在内存中，5 分钟过期
 */
@Slf4j
@Service
public class CaptchaService {

    /** 验证码存储: captchaKey -> { code, expireAt } */
    private final Map<String, CaptchaEntry> store = new ConcurrentHashMap<>();

    /** 验证码有效期（毫秒） */
    private static final long EXPIRE_MS = 5 * 60 * 1000;

    /**
     * 生成验证码
     *
     * @return Map 包含 captchaKey 和 base64 图片数据
     */
    public Map<String, String> generate() {
        // 清除已过期的验证码
        evictExpired();

        // 使用 Hutool 生成验证码：宽 120，高 40，4 位字符，50 条干扰线
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(120, 40, 4, 50);
        String code = captcha.getCode();
        String imageBase64 = Base64.getEncoder().encodeToString(captcha.getImageBytes());

        String key = UUID.randomUUID().toString().replace("-", "");
        store.put(key, new CaptchaEntry(code, System.currentTimeMillis() + EXPIRE_MS));

        log.info("captcha generated: key={}, code={}", key, code);
        return Map.of(
                "captchaKey", key,
                "captchaImage", "data:image/png;base64," + imageBase64
        );
    }

    /**
     * 校验验证码
     *
     * @param key  验证码 key
     * @param code 用户输入的验证码
     * @return true 如果验证码正确且未过期
     */
    public boolean validate(String key, String code) {
        if (key == null || code == null) {
            return false;
        }
        CaptchaEntry entry = store.remove(key);
        if (entry == null) {
            return false;
        }
        if (System.currentTimeMillis() > entry.expireAt) {
            log.warn("captcha expired: key={}", key);
            return false;
        }
        boolean match = entry.code.equalsIgnoreCase(code.trim());
        if (!match) {
            log.warn("captcha mismatch: key={}, expected={}, got={}", key, entry.code, code);
        }
        return match;
    }

    /** 清理过期验证码 */
    private void evictExpired() {
        long now = System.currentTimeMillis();
        store.entrySet().removeIf(e -> now > e.getValue().expireAt);
    }

    /** 验证码存储条目 */
    private record CaptchaEntry(String code, long expireAt) {}
}
