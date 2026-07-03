package com.example.aiagent.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * 认证相关异常统一返回 JSON（如登录失败、未登录）
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleAuthException(IllegalArgumentException e) {
        String msg = e.getMessage();
        int code = (msg != null && msg.contains("未登录")) ? HttpStatus.UNAUTHORIZED.value() : HttpStatus.BAD_REQUEST.value();
        return ResponseEntity.status(code).body(Map.of(
                "code", code,
                "message", msg != null ? msg : "请求参数错误"
        ));
    }
}
