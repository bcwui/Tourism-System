package org.example.springboot.util;

import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Redis分布式锁工具类
 */
@Component
public class RedisLockUtil {

    private static final Logger logger = LoggerFactory.getLogger(RedisLockUtil.class);

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 默认锁过期时间（秒）
     */
    private static final long DEFAULT_EXPIRE = 30;

    /**
     * 尝试获取锁
     *
     * @param lockKey 锁的键
     * @param value   锁的值，用于释放锁时校验身份，一般使用UUID
     * @param expire  锁的过期时间（秒）
     * @return 是否获取成功
     */
    public boolean tryLock(String lockKey, String value, long expire) {
        try {
            // 使用setIfAbsent方法（相当于Redis的SETNX命令）尝试获取锁
            Boolean result = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, value, expire, TimeUnit.SECONDS);
            // 返回加锁结果
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            logger.error("获取Redis锁异常: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 尝试获取锁（使用默认过期时间）
     *
     * @param lockKey 锁的键
     * @param value   锁的值
     * @return 是否获取成功
     */
    public boolean tryLock(String lockKey, String value) {
        return tryLock(lockKey, value, DEFAULT_EXPIRE);
    }

    /**
     * 尝试获取锁，并自动生成锁的值
     *
     * @param lockKey 锁的键
     * @param expire  锁的过期时间（秒）
     * @return 锁的值，获取失败返回null
     */
    public String tryLock(String lockKey, long expire) {
        // 生成UUID作为锁的值
        String value = UUID.randomUUID().toString();
        // 尝试获取锁
        boolean success = tryLock(lockKey, value, expire);
        // 返回锁的值或null
        return success ? value : null;
    }

    /**
     * 尝试获取锁，使用默认过期时间，并自动生成锁的值
     *
     * @param lockKey 锁的键
     * @return 锁的值，获取失败返回null
     */
    public String tryLock(String lockKey) {
        return tryLock(lockKey, DEFAULT_EXPIRE);
    }

    /**
     * 释放锁
     *
     * @param lockKey 锁的键
     * @param value   锁的值
     * @return 是否释放成功
     */
    public boolean releaseLock(String lockKey, String value) {
        try {
            // 获取锁的当前值
            String currentValue = stringRedisTemplate.opsForValue().get(lockKey);
            // 检查当前值是否与传入值相同
            if (value.equals(currentValue)) {
                // 相同则删除锁
                return Boolean.TRUE.equals(stringRedisTemplate.delete(lockKey));
            }
            // 不同则表示锁已过期或被其他线程获取
            return false;
        } catch (Exception e) {
            logger.error("释放Redis锁异常: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 执行带锁的操作
     *
     * @param lockKey     锁的键
     * @param expire      锁的过期时间（秒）
     * @param lockHandler 获取锁后要执行的操作
     * @return 操作是否执行成功
     */
    public boolean executeWithLock(String lockKey, long expire, Runnable lockHandler) {
        // 获取锁
        String lockValue = tryLock(lockKey, expire);
        // 判断是否获取成功
        if (lockValue != null) {
            try {
                // 执行操作
                lockHandler.run();
                return true;
            } finally {
                // 释放锁
                releaseLock(lockKey, lockValue);
            }
        }
        return false;
    }

    /**
     * 执行带锁的操作，使用默认过期时间
     *
     * @param lockKey     锁的键
     * @param lockHandler 获取锁后要执行的操作
     * @return 操作是否执行成功
     */
    public boolean executeWithLock(String lockKey, Runnable lockHandler) {
        return executeWithLock(lockKey, DEFAULT_EXPIRE, lockHandler);
    }
} 