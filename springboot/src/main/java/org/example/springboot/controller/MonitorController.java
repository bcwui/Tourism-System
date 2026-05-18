package org.example.springboot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.example.springboot.common.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/monitor")
@Tag(name = "系统监控", description = "系统监控相关接口")
@CrossOrigin(origins = "*", maxAge = 3600)
public class MonitorController {

    private static final Logger logger = LoggerFactory.getLogger(MonitorController.class);

    @GetMapping("/system")
    @Operation(summary = "获取系统状态")
    public Result<Map<String, Object>> getSystemStatus() {
        try {
            Map<String, Object> result = new HashMap<>();
            
            // 系统基本信息
            result.put("timestamp", System.currentTimeMillis());
            result.put("status", "running");
            result.put("javaVersion", System.getProperty("java.version"));
            result.put("osName", System.getProperty("os.name"));
            result.put("osVersion", System.getProperty("os.version"));
            
            // 内存信息
            Runtime runtime = Runtime.getRuntime();
            Map<String, Object> memory = new HashMap<>();
            memory.put("totalMemory", runtime.totalMemory());
            memory.put("freeMemory", runtime.freeMemory());
            memory.put("maxMemory", runtime.maxMemory());
            memory.put("usedMemory", runtime.totalMemory() - runtime.freeMemory());
            result.put("memory", memory);
            
            logger.info("系统状态查询成功");
            return Result.success(result);
        } catch (Exception e) {
            logger.error("获取系统状态失败：{}", e.getMessage(), e);
            return Result.error("获取系统状态失败：" + e.getMessage());
        }
    }

    @GetMapping("/health")
    @Operation(summary = "健康检查")
    public Result<String> healthCheck() {
        return Result.success("系统运行正常");
    }
}