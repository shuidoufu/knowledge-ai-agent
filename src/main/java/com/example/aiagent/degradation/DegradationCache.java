package com.example.aiagent.degradation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 降级缓存：记录每个工具最近一次成功调用的结果
 */
@Slf4j
@Component
public class DegradationCache {

    private final Map<String, Object> cache = new ConcurrentHashMap<>();

    public void put(String toolMethodKey, Object result) {
        cache.put(toolMethodKey, result);
        log.debug("降级缓存已更新: key={}", toolMethodKey);
    }

    public Object get(String toolMethodKey) {
        return cache.get(toolMethodKey);
    }
}
