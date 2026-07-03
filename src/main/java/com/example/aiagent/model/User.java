package com.example.aiagent.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "users")
@Data
public class User {

    @Id
    private String id;                  // MongoDB 主键

    @Indexed(unique = true)
    private String userId;              // 业务用户ID (UUID)

    @Indexed(unique = true)
    private String username;            // 登录账号

    private String password;            // BCrypt 加密密码

    private LocalDateTime createdAt;    // 注册时间

    private LocalDateTime updatedAt;    // 更新时间

    private LocalDateTime lastLoginAt;  // 最后登录时间

}