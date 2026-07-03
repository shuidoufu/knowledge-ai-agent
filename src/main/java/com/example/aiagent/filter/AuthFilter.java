package com.example.aiagent.filter;

import com.example.aiagent.service.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 会话相关接口鉴权：访问 /api/ai 下的接口必须携带有效 JWT，否则返回 401
 */
public class AuthFilter extends OncePerRequestFilter {

    /** path 相对 context-path，即 /auth、/ai、/health 等 */
    private static final String AUTH_PATH = "/auth";
    private static final String AI_PATH = "/ai";

    private final AuthService authService;

    public AuthFilter(AuthService authService) {
        this.authService = authService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        String path = uri.startsWith(request.getContextPath())
                ? uri.substring(request.getContextPath().length()) : uri;
        if (path.isEmpty()) path = "/";
        // 放行：认证接口、健康检查、Swagger 等
        if (isAllowWithoutAuth(path)) {
            filterChain.doFilter(request, response);
            return;
        }
        // 保护 /ai 下的会话接口（path 相对 context，即 /ai/...）
        if (path.startsWith("/ai")) {
            String token = parseBearerToken(request.getHeader("Authorization"));
            String username = authService.parseUsername(token);
            if (username == null) {
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                response.getWriter().write("{\"code\":401,\"message\":\"未登录或登录已过期，请先登录\"}");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean isAllowWithoutAuth(String path) {
        return path.startsWith(AUTH_PATH)
                || path.startsWith("/health")
                || path.startsWith("/swagger")
                || path.startsWith("/v3/api-docs");
    }

    private static String parseBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) return null;
        return authorization.substring(7).trim();
    }
}
