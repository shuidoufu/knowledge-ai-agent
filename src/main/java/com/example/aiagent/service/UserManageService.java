package com.example.aiagent.service;

import com.example.aiagent.constant.UserRole;
import com.example.aiagent.model.User;
import com.example.aiagent.model.UserDTO;
import com.example.aiagent.model.UserPageDTO;
import com.example.aiagent.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 用户管理服务：管理员分页查询用户与批量调整用户角色，基于 MongoDB
 */
@Slf4j
@Service
public class UserManageService {

    /** 每页条数上限 */
    private static final int MAX_PAGE_SIZE = 50;

    /** 页码上限，限制分页偏移量避免超大页码溢出 */
    private static final int MAX_PAGE = 1000000;

    /** 单次批量修改的用户数量上限 */
    private static final int MAX_BATCH_SIZE = 200;

    /** 排序方式：注册时间倒序（默认） */
    private static final String SORT_TIME_DESC = "time_desc";

    /** 排序方式：注册时间正序 */
    private static final String SORT_TIME_ASC = "time_asc";

    /** 排序方式：用户名升序 */
    private static final String SORT_NAME_ASC = "name_asc";

    private final UserRepository userRepository;
    private final UserService userService;

    public UserManageService(UserRepository userRepository, UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService;
    }

    /**
     * 分页查询用户（仅管理员）
     *
     * @param username 当前登录用户名
     * @param keyword  用户名搜索关键字，可为空
     * @param page     页码，从 1 开始，超过页码上限时按上限处理
     * @param size     每页条数
     * @param sort     排序方式：time_desc（默认）｜time_asc｜name_asc
     * @return 分页结果，前端据此渲染列表与总数、当前页
     */
    public UserPageDTO listUsers(String username, String keyword, int page, int size, String sort) {
        User operator = requireAdminUser(username);
        int pageSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        int pageIndex = Math.max(1, Math.min(page, MAX_PAGE));
        Pageable pageable = PageRequest.of(pageIndex - 1, pageSize, resolveSort(sort));
        Page<User> userPage = StringUtils.hasText(keyword)
                ? userRepository.searchByUsername(Pattern.quote(keyword.trim()), pageable)
                : userRepository.findAll(pageable);
        List<UserDTO> items = userPage.getContent().stream()
                .map(user -> toDTO(user, operator.getUserId()))
                .toList();
        return new UserPageDTO(userPage.getTotalElements(), pageIndex, pageSize, userPage.getTotalPages(), items);
    }

    /**
     * 批量调整用户角色（仅管理员）
     *
     * @param username 当前登录用户名
     * @param userIds  目标用户ID列表
     * @param role     目标角色：0 普通用户 / 1 管理员
     * @return 变更结果：实际变更数量 updated、目标角色 role、提示信息 message
     */
    public Map<String, Object> batchUpdateRole(String username, List<String> userIds, Integer role) {
        User operator = requireAdminUser(username);
        int targetRole = resolveTargetRole(role);
        List<String> targets = normalizeUserIds(userIds);
        if (targets.contains(operator.getUserId())) {
            throw new IllegalArgumentException("不能修改自己的权限");
        }
        List<User> targetUsers = userRepository.findByUserIdIn(targets);
        if (targetUsers.size() != targets.size()) {
            throw new IllegalArgumentException("部分用户不存在，请刷新后重试");
        }
        List<User> changed = new ArrayList<>(targets.size());
        LocalDateTime now = LocalDateTime.now();
        for (User user : targetUsers) {
            if (Objects.equals(user.getRole(), targetRole)) {
                continue;
            }
            user.setRole(targetRole);
            user.setUpdatedAt(now);
            changed.add(user);
        }
        if (!changed.isEmpty()) {
            userRepository.saveAll(changed);
            rollbackIfNoAdminRemains(changed, targetRole);
        }
        log.info("批量调整用户角色: operator={}, role={}, requested={}, updated={}",
                operator.getUsername(), targetRole, targets.size(), changed.size());
        String message = targetRole == UserRole.ADMIN
                ? "已将 " + changed.size() + " 个用户设为管理员"
                : "已将 " + changed.size() + " 个用户设为普通用户";
        return Map.of("updated", changed.size(), "role", targetRole, "message", message);
    }

    /**
     * 复查管理员数量：本次变更后若已无管理员，则回滚本次角色变更并抛出异常
     */
    private void rollbackIfNoAdminRemains(List<User> changed, int targetRole) {
        if (targetRole != UserRole.USER || userService.countAdmins() > 0) {
            return;
        }
        for (User user : changed) {
            user.setRole(UserRole.ADMIN);
            userRepository.save(user);
        }
        throw new IllegalArgumentException("至少保留一个管理员账号");
    }

    /**
     * 取当前登录的管理员账号，账号不存在或不是管理员时抛出无权限异常（由全局异常处理器映射为 403）
     */
    private User requireAdminUser(String username) {
        User user = StringUtils.hasText(username)
                ? userRepository.findByUsername(username.trim()).orElse(null)
                : null;
        if (!UserService.isAdminUser(user)) {
            throw new IllegalArgumentException("无权限访问用户管理");
        }
        return user;
    }

    /**
     * 解析目标角色取值，非 0 / 1 时抛出参数异常
     */
    private int resolveTargetRole(Integer role) {
        if (role == null || (role != UserRole.USER && role != UserRole.ADMIN)) {
            throw new IllegalArgumentException("角色取值不合法");
        }
        return role;
    }

    /**
     * 校验并去重目标用户ID列表，空列表与超量均抛出参数异常
     */
    private List<String> normalizeUserIds(List<String> userIds) {
        Set<String> normalized = new LinkedHashSet<>();
        if (userIds != null) {
            for (String userId : userIds) {
                if (StringUtils.hasText(userId)) {
                    normalized.add(userId.trim());
                }
            }
        }
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("请选择要修改的用户");
        }
        if (normalized.size() > MAX_BATCH_SIZE) {
            throw new IllegalArgumentException("单次最多修改 " + MAX_BATCH_SIZE + " 个用户");
        }
        return new ArrayList<>(normalized);
    }

    /**
     * 解析排序方式，未知取值按注册时间倒序；主排序键外追加用户名二级排序键，保证同值记录翻页顺序稳定
     */
    private Sort resolveSort(String sort) {
        String normalized = sort == null ? "" : sort.trim();
        if (SORT_TIME_ASC.equals(normalized)) {
            return Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("username"));
        }
        if (SORT_NAME_ASC.equals(normalized)) {
            return Sort.by(Sort.Order.asc("username"), Sort.Order.asc("userId"));
        }
        return Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("username"));
    }

    /**
     * 转换为列表项，仅复制页面所需字段（不含密码）
     */
    private UserDTO toDTO(User user, String operatorUserId) {
        return new UserDTO(user.getUserId(), user.getUsername(), user.getRole(),
                user.getCreatedAt(), user.getUpdatedAt(), user.getLastLoginAt(),
                Objects.equals(operatorUserId, user.getUserId()));
    }
}
