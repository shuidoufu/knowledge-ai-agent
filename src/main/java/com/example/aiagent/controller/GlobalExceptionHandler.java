package com.example.aiagent.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;

/**
 * 认证与请求参数异常统一返回 JSON（如登录失败、未登录、无权限、上传超出大小限制）
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleAuthException(IllegalArgumentException e) {
        String msg = e.getMessage();
        int code = resolveStatusCode(msg);
        return ResponseEntity.status(code).body(Map.of(
                "code", code,
                "message", msg != null ? msg : "请求参数错误"
        ));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSizeException(MaxUploadSizeExceededException e) {
        int code = HttpStatus.PAYLOAD_TOO_LARGE.value();
        return ResponseEntity.status(code).body(Map.of(
                "code", code,
                "message", "上传文件超过大小限制，单个文档最大 5MB"
        ));
    }

    /**
     * 根据异常信息判定响应状态码：未登录 401、无权限 403、其余 400
     */
    private int resolveStatusCode(String msg) {
        if (msg != null && msg.contains("未登录")) {
            return HttpStatus.UNAUTHORIZED.value();
        }
        if (msg != null && msg.contains("无权限")) {
            return HttpStatus.FORBIDDEN.value();
        }
        return HttpStatus.BAD_REQUEST.value();
    }
}
