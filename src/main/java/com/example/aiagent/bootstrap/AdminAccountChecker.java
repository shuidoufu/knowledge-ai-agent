package com.example.aiagent.bootstrap;

import com.example.aiagent.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * （手动维护管理员帐号类）
 * 启动时只读检查管理员账号数量，为 0 时提示手工维护 users 集合的 role 字段
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAccountChecker implements ApplicationRunner {

    /** 手工设置管理员角色的命令提示 */
    private static final String ADMIN_ROLE_UPDATE_STATEMENT =
            "db.users.updateOne({username:\"admin\"},{$set:{role:NumberInt(1)}})";

    private final UserService userService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            long adminCount = userService.countAdmins();
            if (adminCount == 0) {
                log.warn("当前没有管理员账号，知识库文档管理入口不可用；请手工维护 role 字段: {}", ADMIN_ROLE_UPDATE_STATEMENT);
            } else {
                log.info("管理员账号数量: {}", adminCount);
            }
        } catch (Exception e) {
            log.warn("管理员账号数量检查跳过: {}", e.getMessage());
        }
    }
}
