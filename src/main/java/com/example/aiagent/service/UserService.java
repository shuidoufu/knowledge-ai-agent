package com.example.aiagent.service;

import cn.hutool.crypto.digest.BCrypt;
import com.example.aiagent.model.User;
import com.example.aiagent.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * 用户服务：注册、登录校验、修改密码，基于 MongoDB
 */
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * 注册新用户，返回保存后的用户
     */
    public User register(String username, String rawPassword) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(rawPassword)) {
            throw new IllegalArgumentException("用户名和密码不能为空");
        }
        String normalizedUsername = username.trim();
        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new IllegalArgumentException("用户名已存在");
        }
        User user = new User();
        user.setUserId(UUID.randomUUID().toString());
        user.setUsername(normalizedUsername);
        user.setPassword(BCrypt.hashpw(rawPassword, BCrypt.gensalt()));
        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        return userRepository.save(user);
    }

    /**
     * 登录校验：用户名密码正确则返回用户对象，否则返回 null
     */
    public User validateAndGet(String username, String rawPassword) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(rawPassword)) {
            return null;
        }
        Optional<User> optionalUser = userRepository.findByUsername(username.trim());
        if (optionalUser.isEmpty()) {
            return null;
        }
        User user = optionalUser.get();
        if (!BCrypt.checkpw(rawPassword, user.getPassword())) {
            return null;
        }
        user.setLastLoginAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        return user;
    }

    /**
     * 修改当前用户密码
     */
    public void changePassword(String username, String oldPassword, String newPassword) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(oldPassword) || !StringUtils.hasText(newPassword)) {
            throw new IllegalArgumentException("用户名和密码不能为空");
        }
        User user = userRepository.findByUsername(username.trim())
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        if (!BCrypt.checkpw(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("原密码错误");
        }
        user.setPassword(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }
}
