package com.example.aiagent.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户管理列表项（不含密码等敏感字段）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {

    /** 业务用户ID（UUID） */
    private String userId;

    /** 登录账号 */
    private String username;

    /** 角色（0 普通用户 / 1 管理员），取值见 UserRole */
    private Integer role;

    /** 注册时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 最后登录时间，从未登录为 null */
    private LocalDateTime lastLoginAt;

    /** 是否为当前登录账号，为真时前端禁止修改其权限 */
    private boolean self;
}
