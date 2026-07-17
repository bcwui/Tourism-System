package org.example.springboot.annotation;

import java.lang.annotation.*;

/**
 * Redis缓存注解
 * 用于标记需要缓存结果的方法
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RedisCache {

    /**
     * 缓存键名前缀
     * 通常使用业务模块名称作为前缀，例如：user、order等
     */
    String prefix() default "";

    /**
     * 缓存键名
     * 支持SpEL表达式，例如：#id、#user.id等
     */
    String key() default "";

    /**
     * 缓存过期时间（秒）
     * 默认1天
     */
    long expire() default 86400;

    /**
     * 是否刷新缓存
     * 如果为true，会删除该键的缓存
     */
    boolean refresh() default false;
} 