package org.example.springboot.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    /** 限流key前缀 */
    String prefix() default "rate:limit:";
    /** 限流标识，支持SpEL表达式 */
    String key() default "";
    /** 时间窗口（秒） */
    int window() default 60;
    /** 时间窗口内最大请求数 */
    int maxRequests() default 20;
    /** 超出限制提示信息 */
    String message() default "请求过于频繁，请稍后再试";
}
