package com.example.aiagent.constant;

/**
 * 用户角色取值表（对应 users 集合的 role 字段，两位数字）
 * 0 普通用户 · 1 管理员
 */
public final class UserRole {

    /** 普通用户 */
    public static final int USER = 0;

    /** 管理员：可访问知识库文档管理 */
    public static final int ADMIN = 1;

    private UserRole() {
    }
}
