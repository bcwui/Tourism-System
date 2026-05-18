package org.example.springboot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.example.springboot.common.Result;
import org.example.springboot.util.RedisLockUtil;
import org.example.springboot.util.RedisUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Redis测试控制器
 */
@Tag(name = "Redis测试接口")
@RestController
@RequestMapping("/redis")
public class RedisTestController {

    private static final Logger logger = LoggerFactory.getLogger(RedisTestController.class);

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private RedisLockUtil redisLockUtil;

    /**
     * 设置字符串缓存
     */
    @Operation(summary = "设置字符串缓存")
    @PostMapping("/string")
    public Result<String> setString(@RequestParam String key, @RequestParam String value, @RequestParam(defaultValue = "60") long expire) {
        boolean result = redisUtil.set(key, value, expire);
        if (result) {
            return Result.success("设置成功");
        } else {
            return Result.error("设置失败");
        }
    }

    /**
     * 获取字符串缓存
     */
    @Operation(summary = "获取字符串缓存")
    @GetMapping("/string")
    public Result<Object> getString(@RequestParam String key) {
        Object value = redisUtil.get(key);
        if (value != null) {
            return Result.success(value);
        } else {
            return Result.error("键不存在");
        }
    }

    /**
     * 设置哈希缓存
     */
    @Operation(summary = "设置哈希缓存")
    @PostMapping("/hash")
    public Result<String> setHash(@RequestParam String key, @RequestBody Map<String, Object> map, @RequestParam(defaultValue = "60") long expire) {
        boolean result = redisUtil.hmset(key, map, expire);
        if (result) {
            return Result.success("设置成功");
        } else {
            return Result.error("设置失败");
        }
    }

    /**
     * 获取哈希缓存
     */
    @Operation(summary = "获取哈希缓存")
    @GetMapping("/hash")
    public Result<Map<Object, Object>> getHash(@RequestParam String key) {
        Map<Object, Object> map = redisUtil.hmget(key);
        return Result.success(map);
    }

    /**
     * 测试分布式锁
     */
    @Operation(summary = "测试分布式锁")
    @GetMapping("/lock")
    public Result<Map<String, Object>> testLock(@RequestParam String key, @RequestParam(defaultValue = "30") long expire) {
        Map<String, Object> result = new HashMap<>();
        
        // 尝试获取锁
        String lockValue = redisLockUtil.tryLock(key, expire);
        
        if (lockValue != null) {
            try {
                // 模拟处理业务逻辑
                logger.info("获取锁成功，锁值：{}", lockValue);
                logger.info("正在处理业务逻辑...");
                TimeUnit.SECONDS.sleep(2); // 模拟耗时操作
                
                result.put("success", true);
                result.put("message", "获取锁成功并执行完任务");
                result.put("lockValue", lockValue);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                result.put("success", false);
                result.put("message", "执行任务时被中断");
            } finally {
                // 释放锁
                boolean released = redisLockUtil.releaseLock(key, lockValue);
                result.put("released", released);
            }
        } else {
            result.put("success", false);
            result.put("message", "获取锁失败，任务正在被其他线程执行");
        }
        
        return Result.success(result);
    }

    /**
     * 清除指定的缓存
     */
    @Operation(summary = "清除指定的缓存")
    @DeleteMapping
    public Result<String> delete(@RequestParam String key) {
        redisUtil.del(key);
        return Result.success("删除成功");
    }

    /**
     * 按前缀清除缓存
     */
    @Operation(summary = "按前缀清除缓存")
    @DeleteMapping("/prefix")
    public Result<String> deleteByPrefix(@RequestParam String prefix) {
        redisUtil.delByPrefix(prefix);
        return Result.success("删除成功");
    }
} 