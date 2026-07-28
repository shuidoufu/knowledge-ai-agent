package com.example.aiagent.degradation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * 降级切面：拦截 @Degradable 标注的方法，按策略执行降级
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class DegradableAspect {

    private final DegradationCache degradationCache;

    @Around("@annotation(degradable)")
    public Object handleDegradation(ProceedingJoinPoint pjp, Degradable degradable) {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        String methodKey = buildMethodKey(signature, pjp.getArgs());

        return switch (degradable.strategy()) {
            case RETRY -> handleRetry(pjp, degradable, methodKey);
            case USE_CACHE -> handleCache(pjp, methodKey);
            case SKIP -> handleSkip(pjp);
            case USE_ALTERNATIVE -> handleAlternative(pjp, degradable);
            case NOTIFY_USER -> handleNotify(pjp, methodKey);
        };
    }

    private Object handleRetry(ProceedingJoinPoint pjp, Degradable degradable, String methodKey) {
        for (int i = 1; i <= degradable.maxRetries(); i++) {
            try {
                Object result = pjp.proceed();
                degradationCache.put(methodKey, result);
                return result;
            } catch (Throwable e) {
                log.warn("工具调用失败 (第{}次/共{}次): {} - {}", i, degradable.maxRetries(), methodKey, e.getMessage());
                if (i == degradable.maxRetries()) {
                    log.error("工具调用重试耗尽: {}", methodKey);
                }
            }
        }
        return "服务暂时不可用，请稍后重试";
    }

    private Object handleCache(ProceedingJoinPoint pjp, String methodKey) {
        try {
            Object result = pjp.proceed();
            degradationCache.put(methodKey, result);
            return result;
        } catch (Throwable e) {
            log.warn("工具调用失败，尝试返回缓存: {} - {}", methodKey, e.getMessage());
            Object cached = degradationCache.get(methodKey);
            if (cached != null) {
                return cached;
            }
            return "暂无缓存数据，服务暂时不可用";
        }
    }

    private Object handleSkip(ProceedingJoinPoint pjp) {
        try {
            return pjp.proceed();
        } catch (Throwable e) {
            log.warn("工具调用失败，已跳过: {}", e.getMessage());
            return null;
        }
    }

    private Object handleAlternative(ProceedingJoinPoint pjp, Degradable degradable) {
        try {
            Object result = pjp.proceed();
            String methodKey = buildMethodKey((MethodSignature) pjp.getSignature(), pjp.getArgs());
            degradationCache.put(methodKey, result);
            return result;
        } catch (Throwable e) {
            log.warn("主工具调用失败，尝试备选降级: {}", e.getMessage());
            if (!degradable.fallback().isBlank()) {
                return "主服务不可用，已切换至备选方案";
            }
            return "该功能暂时无法使用";
        }
    }

    private Object handleNotify(ProceedingJoinPoint pjp, String methodKey) {
        try {
            Object result = pjp.proceed();
            degradationCache.put(methodKey, result);
            return result;
        } catch (Throwable e) {
            log.warn("工具调用失败，已通知用户: {} - {}", methodKey, e.getMessage());
            return "该功能暂时不可用，请稍后重试";
        }
    }

    private String buildMethodKey(MethodSignature signature, Object[] args) {
        return signature.getDeclaringTypeName() + "." + signature.getName() + "(" + Arrays.toString(args) + ")";
    }
}
