package com.example.aiagent.service;

import com.example.aiagent.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * 认证服务：签发与校验 JWT
 */
@Service
public class AuthService {

    private final JwtProperties jwtProperties;
    private SecretKey secretKey;

    public AuthService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(Jwts.SIG.HS256.key().build().getEncoded());
    }

    /**
     * 签发 JWT，subject 存用户名
     */
    public String createToken(String username) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date(now))
                .expiration(new Date(now + jwtProperties.getExpirationSeconds() * 1000))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 解析并校验 JWT，返回 subject（用户名）；非法或过期返回 null
     */
    public String parseUsername(String token) {
        if (token == null || token.isBlank()) return null;
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从请求头取当前登录用户名
     *
     * @param request HTTP 请求
     * @return 用户名；未登录或 token 非法时返回 null
     */
    public String getCurrentUsername(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        return parseUsername(parseBearerToken(request.getHeader("Authorization")));
    }

    /**
     * 提取 Bearer token 原文
     */
    private static String parseBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring(7).trim();
    }
}
