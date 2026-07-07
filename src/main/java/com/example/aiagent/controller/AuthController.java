package com.example.aiagent.controller;

import com.example.aiagent.service.AuthService;
import com.example.aiagent.service.CaptchaService;
import com.example.aiagent.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 登录与鉴权接口
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    private final AuthService authService;
    private final CaptchaService captchaService;

    public AuthController(UserService userService, AuthService authService, CaptchaService captchaService) {
        this.userService = userService;
        this.authService = authService;
        this.captchaService = captchaService;
    }

    /**
     * 登录：用户名密码正确则返回 JWT
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body) {
        String username = body != null ? body.get("username") : null;
        String password = body != null ? body.get("password") : null;
        var user = userService.validateAndGet(username, password);
        if (user == null) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        String token = authService.createToken(user.getUsername());
        return Map.of(
                "token", token,
                "username", user.getUsername()
        );
    }

    /**
     * 校验当前 token 是否有效，返回当前用户名（用于前端恢复登录态）
     * GET /api/auth/me
     * Header: Authorization: Bearer <token>
     */
    @GetMapping("/me")
    public Map<String, Object> me(@RequestHeader(value = "Authorization", required = false) String authorization) {
        String token = parseBearerToken(authorization);
        String username = authService.parseUsername(token);
        if (username == null) {
            throw new IllegalArgumentException("未登录或登录已过期");
        }
        return Map.of("username", username);
    }

    /**
     * 登出：客户端清除 token 即可，此接口用于统一登出流程（可选调用）
     * POST /api/auth/logout
     */
    @PostMapping("/logout")
    public Map<String, Object> logout() {
        return Map.of("message", "ok");
    }

    /**
     * 获取图片验证码
     * GET /api/auth/captcha
     */
    @GetMapping("/captcha")
    public Map<String, String> captcha() {
        return captchaService.generate();
    }

    /**
     * 注册：创建新用户并直接返回 token
     * POST /api/auth/register
     * Body: { "username": "...", "password": "...", "captchaKey": "...", "captchaCode": "..." }
     */
    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, String> body) {
        String captchaKey = body != null ? body.get("captchaKey") : null;
        String captchaCode = body != null ? body.get("captchaCode") : null;
        if (!captchaService.validate(captchaKey, captchaCode)) {
            throw new IllegalArgumentException("验证码错误或已过期");
        }
        String username = body != null ? body.get("username") : null;
        String password = body != null ? body.get("password") : null;
        var user = userService.register(username, password);
        String token = authService.createToken(user.getUsername());
        return Map.of(
                "token", token,
                "username", user.getUsername()
        );
    }

    /**
     * 修改当前登录用户密码
     * POST /api/auth/change-password
     * Header: Authorization: Bearer <token>
     * Body: { "oldPassword": "...", "newPassword": "..." }
     */
    @PostMapping("/change-password")
    public Map<String, Object> changePassword(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, String> body
    ) {
        String token = parseBearerToken(authorization);
        String username = authService.parseUsername(token);
        if (username == null) {
            throw new IllegalArgumentException("未登录或登录已过期");
        }
        String oldPassword = body != null ? body.get("oldPassword") : null;
        String newPassword = body != null ? body.get("newPassword") : null;
        userService.changePassword(username, oldPassword, newPassword);
        return Map.of("message", "密码修改成功");
    }

    private static String parseBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) return null;
        return authorization.substring(7).trim();
    }
}
