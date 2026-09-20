package com.example.aiagent.repository;

import com.example.aiagent.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByUsername(String username);

    List<User> findByUserIdIn(Collection<String> userIds);

    boolean existsByUsername(String username);

    long countByRole(Integer role);

    /**
     * 按用户名模糊匹配分页查询，正则由调用方转义后传入
     *
     * @param regex    用户名匹配正则
     * @param pageable 分页参数
     * @return 分页结果
     */
    @Query("{ 'username': { $regex: ?0, $options: 'i' } }")
    Page<User> searchByUsername(String regex, Pageable pageable);
}
