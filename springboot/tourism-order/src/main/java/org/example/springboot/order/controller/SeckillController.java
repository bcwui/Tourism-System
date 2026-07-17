package org.example.springboot.order.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.example.springboot.DTO.SeckillMessageDTO;
import org.example.springboot.annotation.RateLimit;
import org.example.springboot.common.Result;
import org.example.springboot.entity.SeckillActivity;
import org.example.springboot.entity.User;
import org.example.springboot.service.SeckillService;
import org.example.springboot.util.JwtTokenUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "秒杀接口")
@RestController
@RequestMapping("/seckill")
public class SeckillController {

    @Resource
    private SeckillService seckillService;

    @Operation(summary = "列出所有进行中的秒杀活动")
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> listSeckillActivities() {
        return Result.success(seckillService.listActiveSeckills());
    }

    @Operation(summary = "查询Redis实时库存")
    @GetMapping("/stock/{activityId}")
    public Result<Long> getStock(@PathVariable Long activityId) {
        return Result.success(seckillService.getSeckillStock(activityId));
    }

    @Operation(summary = "查询门票秒杀活动")
    @GetMapping("/activity/{ticketId}")
    public Result<Map<String, Object>> getSeckillActivity(@PathVariable Long ticketId) {
        SeckillActivity activity = seckillService.getActiveSeckill(ticketId);
        if (activity == null) {
            return Result.success(null);
        }
        // 查门票以获取名称、类型、原价
        org.example.springboot.entity.Ticket ticket = seckillService.getTicketById(activity.getTicketId());
        long redisStock = seckillService.getSeckillStock(activity.getId());
        Map<String, Object> data = new HashMap<>();
        data.put("activityId", activity.getId());
        data.put("ticketId", activity.getTicketId());
        data.put("ticketName", ticket != null ? ticket.getTicketName() : "");
        data.put("ticketType", ticket != null ? ticket.getTicketType() : "");
        data.put("originalPrice", ticket != null ? ticket.getPrice() : null);
        data.put("seckillPrice", activity.getSeckillPrice());
        data.put("seckillStock", redisStock);
        data.put("limitPerUser", activity.getLimitPerUser());
        data.put("startTime", activity.getStartTime());
        data.put("endTime", activity.getEndTime());
        return Result.success(data);
    }

    @RateLimit(prefix = "rate:limit:", window = 10, maxRequests = 5, message = "秒杀请求过于频繁")
    @Operation(summary = "执行秒杀")
    @PostMapping("/{activityId}")
    public Result<String> executeSeckill(@PathVariable Long activityId, @RequestBody SeckillMessageDTO request) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            return Result.error("用户未登录");
        }
        seckillService.executeSeckill(activityId, currentUser, request);
        return Result.success("秒杀成功，订单生成中");
    }

    @Operation(summary = "预热秒杀库存（管理端调用）")
    @PostMapping("/preload/{activityId}")
    public Result<String> preloadStock(@PathVariable Long activityId) {
        seckillService.preloadStock(activityId);
        return Result.success("库存预热完成");
    }

    // ==================== 管理端 CRUD ====================

    @Operation(summary = "管理端：分页查看所有秒杀活动")
    @GetMapping("/admin/list")
    public Result<?> adminList(@RequestParam(defaultValue = "1") Integer currentPage,
                               @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(seckillService.listAllActivities(currentPage, size));
    }

    @Operation(summary = "管理端：查看单个秒杀活动详情")
    @GetMapping("/admin/{id}")
    public Result<Map<String, Object>> adminDetail(@PathVariable Long id) {
        SeckillActivity activity = seckillService.getActiveSeckill(id);
        if (activity == null) {
            // 查 DB 兜底（可能已过期但管理员需要看）
            activity = seckillService.getActivityById(id);
        }
        if (activity == null) {
            return Result.error("秒杀活动不存在");
        }
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("id", activity.getId());
        data.put("ticketId", activity.getTicketId());
        data.put("seckillPrice", activity.getSeckillPrice());
        data.put("seckillStock", activity.getSeckillStock());
        data.put("limitPerUser", activity.getLimitPerUser());
        data.put("startTime", activity.getStartTime());
        data.put("endTime", activity.getEndTime());
        data.put("status", activity.getStatus());
        data.put("createTime", activity.getCreateTime());
        data.put("redisStock", seckillService.getSeckillStock(activity.getId()));
        org.example.springboot.entity.Ticket ticket = seckillService.getTicketById(activity.getTicketId());
        if (ticket != null) {
            data.put("ticketName", ticket.getTicketName());
            data.put("originalPrice", ticket.getPrice());
        }
        return Result.success(data);
    }

    @Operation(summary = "创建秒杀活动")
    @PostMapping
    public Result<SeckillActivity> create(@RequestBody SeckillActivity activity) {
        seckillService.createActivity(activity);
        return Result.success(activity);
    }

    @Operation(summary = "更新秒杀活动")
    @PutMapping("/{id}")
    public Result<String> update(@PathVariable Long id, @RequestBody SeckillActivity activity) {
        activity.setId(id);
        seckillService.updateActivity(activity);
        return Result.success("更新成功");
    }

    @Operation(summary = "删除秒杀活动")
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        seckillService.deleteActivity(id);
        return Result.success("删除成功");
    }
}
