package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.example.springboot.DTO.OrderMessageDTO;
import org.example.springboot.entity.ScenicSpot;
import org.example.springboot.entity.Ticket;
import org.example.springboot.entity.TicketOrder;
import org.example.springboot.entity.User;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.ScenicSpotMapper;
import org.example.springboot.mapper.TicketMapper;
import org.example.springboot.mapper.TicketOrderMapper;
import org.example.springboot.mapper.UserMapper;
import org.example.springboot.util.JwtTokenUtils;
import org.example.springboot.util.RedisLockUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TicketOrderService {

    private static final Logger logger = LoggerFactory.getLogger(TicketOrderService.class);
    private static final String STOCK_LOCK_PREFIX = "lock:ticket:stock:";

    @Resource
    private TicketOrderMapper ticketOrderMapper;
    @Resource
    private TicketMapper ticketMapper;
    @Resource
    private ScenicSpotMapper scenicSpotMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private TicketService ticketService;
    @Resource
    private RedisLockUtil redisLockUtil;
    @Resource
    private RocketMQTemplate rocketMQTemplate;

    @Transactional
    public TicketOrder createOrder(TicketOrder order) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("用户未登录");
        }
        order.setUserId(currentUser.getId());

        Ticket ticket = ticketMapper.selectById(order.getTicketId());
        if (ticket == null) {
            throw new ServiceException("门票不存在");
        }
        if (ticket.getStatus() != 1) {
            throw new ServiceException("该门票暂不可预订");
        }

        // 分布式锁防超卖
        String lockKey = STOCK_LOCK_PREFIX + order.getTicketId();
        String lockValue = redisLockUtil.tryLock(lockKey, 10);
        if (lockValue == null) {
            throw new ServiceException("系统繁忙，请稍后重试");
        }

        try {
            // 原子扣库存，affected=0 表示库存不足
            if (!ticketService.deductStock(order.getTicketId(), order.getQuantity())) {
                throw new ServiceException("门票库存不足");
            }

            order.setOrderNo(generateOrderNo());
            order.setStatus(0);
            order.setTotalAmount(ticket.getDiscountPrice() != null
                    ? ticket.getDiscountPrice().multiply(BigDecimal.valueOf(order.getQuantity()))
                    : ticket.getPrice().multiply(BigDecimal.valueOf(order.getQuantity())));

            ticketOrderMapper.insert(order);
        } finally {
            redisLockUtil.releaseLock(lockKey, lockValue);
        }

        // 发送异步订单消息到 RocketMQ
        try {
            OrderMessageDTO msg = buildOrderMessage(order, ticket, currentUser, "ORDER_CREATED");
            rocketMQTemplate.convertAndSend("order-topic", msg);
        } catch (Exception e) {
            logger.error("发送订单创建消息失败，订单号：{}，错误：{}", order.getOrderNo(), e.getMessage(), e);
        }

        return order;
    }

    @Transactional
    public void payOrder(Long orderId, String paymentMethod) {
        TicketOrder order = ticketOrderMapper.selectById(orderId);
        if (order == null) {
            throw new ServiceException("订单不存在");
        }
        if (order.getStatus() != 0) {
            throw new ServiceException("订单状态不正确，无法支付");
        }

        order.setStatus(1);
        order.setPaymentMethod(paymentMethod);
        order.setPaymentTime(LocalDateTime.now());
        ticketOrderMapper.updateById(order);

        try {
            Ticket ticket = ticketMapper.selectById(order.getTicketId());
            User user = userMapper.selectById(order.getUserId());
            OrderMessageDTO msg = buildOrderMessage(order, ticket, user, "ORDER_PAID");
            rocketMQTemplate.convertAndSend("order-topic", msg);
        } catch (Exception e) {
            logger.error("发送订单支付消息失败，订单号：{}，错误：{}", order.getOrderNo(), e.getMessage(), e);
        }
    }

    @Transactional
    public void cancelOrder(Long orderId) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("用户未登录");
        }

        TicketOrder order = ticketOrderMapper.selectById(orderId);
        if (order == null) {
            throw new ServiceException("订单不存在");
        }
        if (!order.getUserId().equals(currentUser.getId()) && !"ADMIN".equals(currentUser.getRoleCode())) {
            throw new ServiceException("无权操作此订单");
        }
        if (order.getStatus() != 0) {
            throw new ServiceException("只有待支付的订单可以取消");
        }

        order.setStatus(2);
        ticketOrderMapper.updateById(order);

        // 原子恢复库存
        ticketService.restoreStock(order.getTicketId(), order.getQuantity());

        try {
            Ticket ticket = ticketMapper.selectById(order.getTicketId());
            OrderMessageDTO msg = buildOrderMessage(order, ticket, currentUser, "ORDER_CANCELLED");
            rocketMQTemplate.convertAndSend("order-topic", msg);
        } catch (Exception e) {
            logger.error("发送订单取消消息失败，订单号：{}，错误：{}", order.getOrderNo(), e.getMessage(), e);
        }
    }

    @Transactional
    public void refundOrder(Long orderId) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("用户未登录");
        }
        if (!"ADMIN".equals(currentUser.getRoleCode())) {
            throw new ServiceException("无权执行退款操作");
        }

        TicketOrder order = ticketOrderMapper.selectById(orderId);
        if (order == null) {
            throw new ServiceException("订单不存在");
        }
        if (order.getStatus() != 1) {
            throw new ServiceException("只有已支付的订单可以退款");
        }

        order.setStatus(3);
        ticketOrderMapper.updateById(order);
        ticketService.restoreStock(order.getTicketId(), order.getQuantity());

        try {
            Ticket ticket = ticketMapper.selectById(order.getTicketId());
            User user = userMapper.selectById(order.getUserId());
            OrderMessageDTO msg = buildOrderMessage(order, ticket, user, "ORDER_REFUNDED");
            rocketMQTemplate.convertAndSend("order-topic", msg);
        } catch (Exception e) {
            logger.error("发送订单退款消息失败，订单号：{}，错误：{}", order.getOrderNo(), e.getMessage(), e);
        }
    }

    @Transactional
    public void completeOrder(Long orderId) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("用户未登录");
        }
        if (!"ADMIN".equals(currentUser.getRoleCode())) {
            throw new ServiceException("无权执行此操作");
        }

        TicketOrder order = ticketOrderMapper.selectById(orderId);
        if (order == null) {
            throw new ServiceException("订单不存在");
        }
        if (order.getStatus() != 1) {
            throw new ServiceException("只有已支付的订单可以标记为已完成");
        }

        order.setStatus(4);
        ticketOrderMapper.updateById(order);

        try {
            Ticket ticket = ticketMapper.selectById(order.getTicketId());
            User user = userMapper.selectById(order.getUserId());
            OrderMessageDTO msg = buildOrderMessage(order, ticket, user, "ORDER_COMPLETED");
            rocketMQTemplate.convertAndSend("order-topic", msg);
        } catch (Exception e) {
            logger.error("发送订单完成消息失败，订单号：{}，错误：{}", order.getOrderNo(), e.getMessage(), e);
        }
    }

    public Page<TicketOrder> getUserOrders(Long userId, Integer status, Integer currentPage, Integer size) {
        LambdaQueryWrapper<TicketOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TicketOrder::getUserId, userId);
        if (status != null) {
            queryWrapper.eq(TicketOrder::getStatus, status);
        }
        queryWrapper.orderByDesc(TicketOrder::getCreateTime);
        Page<TicketOrder> page = ticketOrderMapper.selectPage(new Page<>(currentPage, size), queryWrapper);
        for (TicketOrder order : page.getRecords()) {
            fillOrderDetails(order);
        }
        return page;
    }

    public Page<TicketOrder> getAllOrders(String orderNo, String visitorName, String visitorPhone, Integer status,
                                         Integer currentPage, Integer size) {
        LambdaQueryWrapper<TicketOrder> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(orderNo)) {
            queryWrapper.like(TicketOrder::getOrderNo, orderNo);
        }
        if (StringUtils.isNotBlank(visitorName)) {
            queryWrapper.like(TicketOrder::getVisitorName, visitorName);
        }
        if (StringUtils.isNotBlank(visitorPhone)) {
            queryWrapper.like(TicketOrder::getVisitorPhone, visitorPhone);
        }
        if (status != null) {
            queryWrapper.eq(TicketOrder::getStatus, status);
        }
        queryWrapper.orderByDesc(TicketOrder::getCreateTime);
        Page<TicketOrder> page = ticketOrderMapper.selectPage(new Page<>(currentPage, size), queryWrapper);
        for (TicketOrder order : page.getRecords()) {
            fillOrderDetails(order);
        }
        return page;
    }

    public TicketOrder getOrderDetail(Long orderId) {
        TicketOrder order = ticketOrderMapper.selectById(orderId);
        if (order == null) {
            throw new ServiceException("订单不存在");
        }
        fillOrderDetails(order);
        return order;
    }

    private void fillOrderDetails(TicketOrder order) {
        Ticket ticket = ticketMapper.selectById(order.getTicketId());
        if (ticket != null) {
            order.setTicketName(ticket.getTicketName());
            ScenicSpot scenicSpot = scenicSpotMapper.selectById(ticket.getScenicId());
            if (scenicSpot != null) {
                order.setScenicName(scenicSpot.getName());
            }
        }
        User user = userMapper.selectById(order.getUserId());
        if (user != null) {
            order.setUsername(user.getUsername());
        }
    }

    private String generateOrderNo() {
        String dateStr = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String randomStr = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 4);
        return dateStr + randomStr;
    }

    public TicketOrder getOrderByOrderNo(String orderNo) {
        if (StringUtils.isBlank(orderNo)) {
            return null;
        }
        LambdaQueryWrapper<TicketOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TicketOrder::getOrderNo, orderNo);
        TicketOrder order = ticketOrderMapper.selectOne(queryWrapper);
        if (order != null) {
            fillOrderDetails(order);
        }
        return order;
    }

    @Transactional
    public void deleteOrder(Long orderId) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("用户未登录");
        }
        TicketOrder order = ticketOrderMapper.selectById(orderId);
        if (order == null) {
            throw new ServiceException("订单不存在");
        }
        boolean isAdmin = "ADMIN".equals(currentUser.getRoleCode());
        if (!order.getUserId().equals(currentUser.getId()) && !isAdmin) {
            throw new ServiceException("无权操作此订单");
        }
        if (order.getStatus() != 2 && order.getStatus() != 3 && order.getStatus() != 4) {
            throw new ServiceException("只有已完成、已退款或已取消的订单可以删除");
        }
        ticketOrderMapper.deleteById(orderId);
    }

    public Map<String, Object> getUserOrderStats(Long userId) {
        LambdaQueryWrapper<TicketOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TicketOrder::getUserId, userId);
        List<TicketOrder> allOrders = ticketOrderMapper.selectList(queryWrapper);

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", allOrders.size());
        stats.put("pending", 0);
        stats.put("paid", 0);
        stats.put("cancelled", 0);
        stats.put("refunded", 0);
        stats.put("completed", 0);

        for (TicketOrder order : allOrders) {
            switch (order.getStatus()) {
                case 0 -> stats.put("pending", (Integer) stats.get("pending") + 1);
                case 1 -> stats.put("paid", (Integer) stats.get("paid") + 1);
                case 2 -> stats.put("cancelled", (Integer) stats.get("cancelled") + 1);
                case 3 -> stats.put("refunded", (Integer) stats.get("refunded") + 1);
                case 4 -> stats.put("completed", (Integer) stats.get("completed") + 1);
            }
        }
        return stats;
    }

    private OrderMessageDTO buildOrderMessage(TicketOrder order, Ticket ticket, User user, String eventType) {
        OrderMessageDTO msg = new OrderMessageDTO();
        msg.setOrderId(order.getId());
        msg.setOrderNo(order.getOrderNo());
        msg.setUserId(user.getId());
        msg.setUsername(user.getUsername());
        msg.setUserEmail(user.getEmail());
        msg.setUserPhone(user.getPhone());
        msg.setStatus(order.getStatus());
        msg.setAmount(order.getTotalAmount());
        msg.setOrderType("ticket");
        msg.setProductId(ticket != null ? ticket.getId() : null);
        msg.setProductName(ticket != null ? ticket.getTicketName() : null);
        msg.setQuantity(order.getQuantity());
        msg.setCreateTime(order.getCreateTime());
        msg.setPaymentTime(order.getPaymentTime());
        msg.setEventType(eventType);
        return msg;
    }
}
