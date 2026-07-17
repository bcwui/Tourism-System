package org.example.springboot.service;

import jakarta.annotation.Resource;
import org.example.springboot.DTO.SeckillMessageDTO;
import org.example.springboot.entity.SeckillActivity;
import org.example.springboot.entity.Ticket;
import org.example.springboot.entity.User;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.SeckillActivityMapper;
import org.example.springboot.mapper.TicketMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.example.springboot.config.RabbitMQConfig.SECKILL_EXCHANGE;
import static org.example.springboot.config.RabbitMQConfig.SECKILL_ORDER_ROUTING_KEY;

@Service
public class SeckillService {

    private static final Logger logger = LoggerFactory.getLogger(SeckillService.class);
    private static final String SECKILL_STOCK_PREFIX = "seckill:stock:";
    private static final String SECKILL_USERS_PREFIX = "seckill:users:";
    private static final String ORDER_LOCK_PREFIX = "lock:seckill:order:";

    @Resource
    private SeckillActivityMapper seckillActivityMapper;
    @Resource
    private TicketMapper ticketMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RabbitTemplate rabbitTemplate;

    /**
     * 列出所有进行中的秒杀活动，含门票信息
     */
    public List<Map<String, Object>> listActiveSeckills() {
        List<SeckillActivity> activities = seckillActivityMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SeckillActivity>()
                        .eq(SeckillActivity::getStatus, 1)
        );
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (SeckillActivity a : activities) {
            if (now.isBefore(a.getStartTime()) || now.isAfter(a.getEndTime())) {
                continue;
            }
            Ticket ticket = ticketMapper.selectById(a.getTicketId());
            if (ticket == null) continue;
            Map<String, Object> item = new HashMap<>();
            item.put("activityId", a.getId());
            item.put("ticketId", a.getTicketId());
            item.put("ticketName", ticket.getTicketName());
            item.put("ticketType", ticket.getTicketType());
            item.put("originalPrice", ticket.getPrice());
            item.put("seckillPrice", a.getSeckillPrice());
            item.put("seckillStock", getSeckillStock(a.getId()));
            item.put("limitPerUser", a.getLimitPerUser());
            item.put("startTime", a.getStartTime());
            item.put("endTime", a.getEndTime());
            result.add(item);
        }
        return result;
    }

    public SeckillActivity getActivityById(Long activityId) {
        return seckillActivityMapper.selectById(activityId);
    }

    public SeckillActivity getActiveSeckill(Long ticketId) {
        SeckillActivity activity = seckillActivityMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SeckillActivity>()
                        .eq(SeckillActivity::getTicketId, ticketId)
                        .eq(SeckillActivity::getStatus, 1)
        );
        if (activity == null) {
            return null;
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(activity.getStartTime()) || now.isAfter(activity.getEndTime())) {
            return null;
        }
        return activity;
    }

    public Ticket getTicketById(Long ticketId) {
        return ticketMapper.selectById(ticketId);
    }

    public long getSeckillStock(Long activityId) {
        String stockStr = stringRedisTemplate.opsForValue().get(SECKILL_STOCK_PREFIX + activityId);
        if (stockStr != null) {
            return Long.parseLong(stockStr);
        }
        // Redis 未预热，返回 DB 中的实际库存
        SeckillActivity activity = seckillActivityMapper.selectById(activityId);
        return activity != null ? activity.getSeckillStock() : 0;
    }

    /**
     * 预热秒杀库存到Redis（秒杀开始前调用）
     */
    public void preloadStock(Long activityId) {
        SeckillActivity activity = seckillActivityMapper.selectById(activityId);
        if (activity == null) {
            throw new ServiceException("秒杀活动不存在");
        }
        String stockKey = SECKILL_STOCK_PREFIX + activityId;
        stringRedisTemplate.opsForValue().set(stockKey, String.valueOf(activity.getSeckillStock()),
                java.time.Duration.between(LocalDateTime.now(), activity.getEndTime()).getSeconds(),
                TimeUnit.SECONDS);
        logger.info("秒杀库存预热完成: activityId={}, stock={}", activityId, activity.getSeckillStock());
    }

    /**
     * 执行秒杀
     */
    public boolean executeSeckill(Long activityId, User user, SeckillMessageDTO request) {
        // 1. 校验秒杀活动
        SeckillActivity activity = seckillActivityMapper.selectById(activityId);
        if (activity == null || activity.getStatus() != 1) {
            throw new ServiceException("秒杀活动不存在或已结束");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(activity.getStartTime())) {
            throw new ServiceException("秒杀尚未开始");
        }
        if (now.isAfter(activity.getEndTime())) {
            throw new ServiceException("秒杀已结束");
        }

        // 2. 校验门票
        Ticket ticket = ticketMapper.selectById(activity.getTicketId());
        if (ticket == null || ticket.getStatus() != 1) {
            throw new ServiceException("门票不可用");
        }

        // 3. 限购检查：每人限购
        String usersKey = SECKILL_USERS_PREFIX + activityId;
        Boolean alreadyBought = stringRedisTemplate.opsForSet().isMember(usersKey, user.getId().toString());
        if (Boolean.TRUE.equals(alreadyBought)) {
            throw new ServiceException("您已参与过本次秒杀");
        }
        Integer limitPerUser = activity.getLimitPerUser() != null ? activity.getLimitPerUser() : 1;
        int buyQty = request.getQuantity() != null ? request.getQuantity() : 1;
        if (buyQty > limitPerUser) {
            throw new ServiceException("每人限购" + limitPerUser + "张");
        }

        // 4. Redis原子扣库存（若未预热则用 SETNX 自动初始化，防并发覆写）
        String stockKey = SECKILL_STOCK_PREFIX + activityId;
        Boolean exists = stringRedisTemplate.hasKey(stockKey);
        if (Boolean.FALSE.equals(exists)) {
            long ttl = java.time.Duration.between(now, activity.getEndTime()).getSeconds();
            if (ttl > 0) {
                stringRedisTemplate.opsForValue().setIfAbsent(stockKey,
                        String.valueOf(activity.getSeckillStock()), ttl, TimeUnit.SECONDS);
            }
        }
        Long remaining = stringRedisTemplate.opsForValue().decrement(stockKey, buyQty);
        if (remaining == null || remaining < 0) {
            stringRedisTemplate.opsForValue().increment(stockKey, buyQty);
            throw new ServiceException("秒杀库存不足");
        }

        // 5. 标记用户已购买
        stringRedisTemplate.opsForSet().add(usersKey, user.getId().toString());

        // 6. 发送异步下单消息
        try {
            request.setActivityId(activityId);
            request.setUserId(user.getId());
            request.setTicketId(activity.getTicketId());
            rabbitTemplate.convertAndSend(SECKILL_EXCHANGE, SECKILL_ORDER_ROUTING_KEY, request);
            logger.info("秒杀下单消息已发送: userId={}, activityId={}, ticketId={}", user.getId(), activityId, activity.getTicketId());
        } catch (Exception e) {
            logger.error("秒杀下单消息发送失败，回滚库存: userId={}, activityId={}", user.getId(), activityId, e);
            stringRedisTemplate.opsForValue().increment(stockKey, buyQty);
            stringRedisTemplate.opsForSet().remove(usersKey, user.getId().toString());
            throw new ServiceException("系统繁忙，请稍后重试");
        }

        return true;
    }

    /**
     * 回滚秒杀（消息消费失败时）
     */
    public void rollbackSeckill(Long activityId, Long userId, int quantity) {
        stringRedisTemplate.opsForValue().increment(SECKILL_STOCK_PREFIX + activityId, quantity);
        stringRedisTemplate.opsForSet().remove(SECKILL_USERS_PREFIX + activityId, userId.toString());
        logger.info("秒杀已回滚: userId={}, activityId={}, quantity={}", userId, activityId, quantity);
    }

    /**
     * 管理端：列出所有秒杀活动（含门票信息，分页）
     */
    public com.baomidou.mybatisplus.extension.plugins.pagination.Page<Map<String, Object>> listAllActivities(
            Integer currentPage, Integer size) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<SeckillActivity> page =
                seckillActivityMapper.selectPage(
                        new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(currentPage, size),
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SeckillActivity>()
                                .orderByDesc(SeckillActivity::getCreateTime));
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Map<String, Object>> result =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(currentPage, size, page.getTotal());
        List<Map<String, Object>> records = new ArrayList<>();
        for (SeckillActivity a : page.getRecords()) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", a.getId());
            item.put("ticketId", a.getTicketId());
            item.put("seckillPrice", a.getSeckillPrice());
            item.put("seckillStock", a.getSeckillStock());
            item.put("limitPerUser", a.getLimitPerUser());
            item.put("startTime", a.getStartTime());
            item.put("endTime", a.getEndTime());
            item.put("status", a.getStatus());
            item.put("createTime", a.getCreateTime());
            item.put("redisStock", getSeckillStock(a.getId()));
            Ticket ticket = ticketMapper.selectById(a.getTicketId());
            if (ticket != null) {
                item.put("ticketName", ticket.getTicketName());
                item.put("originalPrice", ticket.getPrice());
            }
            records.add(item);
        }
        result.setRecords(records);
        return result;
    }

    // ==================== 管理端 CRUD ====================

    public void createActivity(SeckillActivity activity) {
        Ticket ticket = ticketMapper.selectById(activity.getTicketId());
        if (ticket == null) {
            throw new ServiceException("门票不存在");
        }
        activity.setCreateTime(LocalDateTime.now());
        seckillActivityMapper.insert(activity);
        logger.info("秒杀活动已创建: ticketId={}, seckillPrice={}", activity.getTicketId(), activity.getSeckillPrice());
    }

    public void updateActivity(SeckillActivity activity) {
        SeckillActivity existing = seckillActivityMapper.selectById(activity.getId());
        if (existing == null) {
            throw new ServiceException("秒杀活动不存在");
        }
        seckillActivityMapper.updateById(activity);
    }

    public void deleteActivity(Long activityId) {
        SeckillActivity activity = seckillActivityMapper.selectById(activityId);
        if (activity == null) {
            throw new ServiceException("秒杀活动不存在");
        }
        seckillActivityMapper.deleteById(activityId);
        // 清理 Redis 缓存
        stringRedisTemplate.delete(SECKILL_STOCK_PREFIX + activityId);
        stringRedisTemplate.delete(SECKILL_USERS_PREFIX + activityId);
        logger.info("秒杀活动已删除: activityId={}", activityId);
    }
}
