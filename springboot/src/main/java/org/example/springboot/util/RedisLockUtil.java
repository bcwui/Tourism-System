package org.example.springboot.util;

import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class RedisLockUtil {

    private static final Logger logger = LoggerFactory.getLogger(RedisLockUtil.class);
    private static final long DEFAULT_EXPIRE = 30;

    /**
     * Lua脚本：原子释放锁，只有当value匹配时才删除key
     */
    private static final String RELEASE_LOCK_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "  return redis.call('del', KEYS[1]) " +
            "else " +
            "  return 0 " +
            "end";

    private static final DefaultRedisScript<Long> RELEASE_LOCK_REDIS_SCRIPT =
            new DefaultRedisScript<>(RELEASE_LOCK_SCRIPT, Long.class);

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public boolean tryLock(String lockKey, String value, long expire) {
        try {
            Boolean result = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, value, expire, TimeUnit.SECONDS);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            logger.error("获取Redis锁异常: {}", e.getMessage(), e);
            return false;
        }
    }

    public boolean tryLock(String lockKey, String value) {
        return tryLock(lockKey, value, DEFAULT_EXPIRE);
    }

    public String tryLock(String lockKey, long expire) {
        String value = UUID.randomUUID().toString();
        boolean success = tryLock(lockKey, value, expire);
        return success ? value : null;
    }

    public String tryLock(String lockKey) {
        return tryLock(lockKey, DEFAULT_EXPIRE);
    }

    /**
     * 使用Lua脚本原子释放锁
     */
    public boolean releaseLock(String lockKey, String value) {
        try {
            Long result = stringRedisTemplate.execute(
                    RELEASE_LOCK_REDIS_SCRIPT,
                    Collections.singletonList(lockKey),
                    value
            );
            return Long.valueOf(1).equals(result);
        } catch (Exception e) {
            logger.error("释放Redis锁异常: {}", e.getMessage(), e);
            return false;
        }
    }

    public boolean executeWithLock(String lockKey, long expire, Runnable lockHandler) {
        String lockValue = tryLock(lockKey, expire);
        if (lockValue != null) {
            try {
                lockHandler.run();
                return true;
            } finally {
                releaseLock(lockKey, lockValue);
            }
        }
        return false;
    }

    public boolean executeWithLock(String lockKey, Runnable lockHandler) {
        return executeWithLock(lockKey, DEFAULT_EXPIRE, lockHandler);
    }
}
