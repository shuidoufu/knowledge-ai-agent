package com.example.aiagent.controller;

import com.example.aiagent.model.UserPageDTO;
import com.example.aiagent.service.AuthService;
import com.example.aiagent.service.UserManageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 用户管理接口（仅管理员可用，权限校验在服务层）
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/ai/user")
public class UserController {

    private final UserManageService userManageService;
    private final AuthService authService;

    /**
     * 分页查询用户
     * GET /api/ai/user/list
     */
    @GetMapping("/list")
    public UserPageDTO list(@RequestParam(required = false) String keyword,
                            @RequestParam(defaultValue = "1") int page,
                            @RequestParam(defaultValue = "10") int size,
                            @RequestParam(defaultValue = "time_desc") String sort,
                            HttpServletRequest request) {
        return userManageService.listUsers(currentUsername(request), keyword, page, size, sort);
    }

    /**
     * 批量调整用户角色
     * POST /api/ai/user/batch-role
     * Body: { "userIds": ["<用户ID>"], "role": 1 }
     */
    @PostMapping("/batch-role")
    public Map<String, Object> batchRole(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        return userManageService.batchUpdateRole(currentUsername(request), readUserIds(body), readRole(body));
    }

    /**
     * 取当前登录用户名，未登录直接抛出 401
     */
    private String currentUsername(HttpServletRequest request) {
        String username = authService.getCurrentUsername(request);
        if (username == null) {
            throw new IllegalArgumentException("未登录或登录已过期");
        }
        return username;
    }

    /**
     * 读取请求体中的用户ID列表，缺失或类型不符时返回 null 交由服务层校验
     */
    private List<String> readUserIds(Map<String, Object> body) {
        Object value = body == null ? null : body.get("userIds");
        if (!(value instanceof List<?> list)) {
            return null;
        }
        return list.stream().filter(item -> item instanceof String).map(String.class::cast).toList();
    }

    /**
     * 读取请求体中的角色取值，缺失或非整数时返回 null 交由服务层校验
     */
    private Integer readRole(Map<String, Object> body) {
        Object value = body == null ? null : body.get("role");
        return value instanceof Integer role ? role : null;
    }
}
