package com.example.aiagent.degradation;

/**
 * 降级策略
 */
public enum FallbackStrategy {
    SKIP,           // 跳过此工具，返回空结果
    RETRY,          // 重试指定次数
    USE_CACHE,      // 返回上次缓存的成功结果
    NOTIFY_USER,    // 返回用户提示信息
    USE_ALTERNATIVE // 调用备选方法
}
