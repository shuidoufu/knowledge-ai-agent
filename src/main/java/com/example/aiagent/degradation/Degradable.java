package com.example.aiagent.degradation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 统一降级注解 @Degradable，标注在 @Tool 方法上，当工具调用异常时自动执行降级策略
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Degradable {

    FallbackStrategy strategy() default FallbackStrategy.NOTIFY_USER;

    int maxRetries() default 3;

    String fallback() default "";
}
