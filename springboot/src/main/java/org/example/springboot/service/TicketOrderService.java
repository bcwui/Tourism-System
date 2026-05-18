package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TicketOrderService {

    private static final Logger logger = LoggerFactory.getLogger(TicketOrderService.class);

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
    private EmailService emailService;

    /**
     * 创建订单
     */
    @Transactional
    public TicketOrder createOrder(TicketOrder order) {
        // 获取当前登录用户
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("用户未登录");
        }
        order.setUserId(currentUser.getId());
        
        // 检查门票是否存在且可预订
        Ticket ticket = ticketMapper.selectById(order.getTicketId());
        if (ticket == null) {
            throw new ServiceException("门票不存在");
        }
        if (ticket.getStatus() != 1) {
            throw new ServiceException("该门票暂不可预订");
        }
        
        // 检查库存是否足够
        if (ticket.getStock() < order.getQuantity()) {
            throw new ServiceException("门票库存不足");
        }
        
        // 生成订单号
        order.setOrderNo(generateOrderNo());
        
        // 设置订单状态为待支付
        order.setStatus(0);
        
        // 计算订单总金额
        order.setTotalAmount(ticket.getDiscountPrice() != null ? 
                ticket.getDiscountPrice().multiply(java.math.BigDecimal.valueOf(order.getQuantity())) : 
                ticket.getPrice().multiply(java.math.BigDecimal.valueOf(order.getQuantity())));
        
        // 保存订单
        ticketOrderMapper.insert(order);
        
        // 扣减库存
        ticketService.updateTicketStock(order.getTicketId(), order.getQuantity());
        
        // 发送订单创建消息 - 使用try-catch确保异常不会影响订单创建
        try {
            sendOrderMessage(order, "create");
        } catch (Exception e) {
            logger.error("发送订单创建消息失败，但订单创建成功。订单号：{}，错误：{}", order.getOrderNo(), e.getMessage(), e);
            // 不重新抛出异常，确保订单创建成功
        }
        
        return order;
    }
    
    /**
     * 支付订单
     */
    @Transactional
    public void payOrder(Long orderId, String paymentMethod) {
        // 获取订单信息
        TicketOrder order = ticketOrderMapper.selectById(orderId);
        if (order == null) {
            throw new ServiceException("订单不存在");
        }
        
        // 检查订单状态
        if (order.getStatus() != 0) {
            throw new ServiceException("订单状态不正确，无法支付");
        }
        
        // 更新订单状态为已支付
        order.setStatus(1);
        order.setPaymentMethod(paymentMethod);
        order.setPaymentTime(LocalDateTime.now());
        
        // 先更新订单状态到数据库
        ticketOrderMapper.updateById(order);
        
        // 发送订单支付消息 - 使用try-catch确保异常不会影响订单状态更新
        try {
            sendOrderMessage(order, "pay");
        } catch (Exception e) {
            logger.error("发送订单支付消息失败，但订单状态已更新成功。订单号：{}，错误：{}", order.getOrderNo(), e.getMessage(), e);
            // 不重新抛出异常，确保订单状态更新成功
        }
    }
    
    /**
     * 取消订单
     */
    @Transactional
    public void cancelOrder(Long orderId) {
        // 获取当前登录用户
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("用户未登录");
        }
        
        // 获取订单信息
        TicketOrder order = ticketOrderMapper.selectById(orderId);
        if (order == null) {
            throw new ServiceException("订单不存在");
        }
        
        // 验证订单所属
        if (!order.getUserId().equals(currentUser.getId()) && !"ADMIN".equals(currentUser.getRoleCode())) {
            throw new ServiceException("无权操作此订单");
        }
        
        // 检查订单状态，只有待支付的订单可以取消
        if (order.getStatus() != 0) {
            throw new ServiceException("只有待支付的订单可以取消");
        }
        
        // 更新订单状态为已取消
        order.setStatus(2);
        ticketOrderMapper.updateById(order);
        
        // 恢复门票库存
        ticketService.restoreTicketStock(order.getTicketId(), order.getQuantity());
        
        // 发送订单取消消息 - 使用try-catch确保异常不会影响订单取消
        try {
            sendOrderMessage(order, "cancel");
        } catch (Exception e) {
            logger.error("发送订单取消消息失败，但订单取消成功。订单号：{}，错误：{}", order.getOrderNo(), e.getMessage(), e);
            // 不重新抛出异常，确保订单取消成功
        }
    }
    
    /**
     * 退款订单
     */
    @Transactional
    public void refundOrder(Long orderId) {
        // 获取当前登录用户
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("用户未登录");
        }
        
        // 获取订单信息
        TicketOrder order = ticketOrderMapper.selectById(orderId);
        if (order == null) {
            throw new ServiceException("订单不存在");
        }
        
        // 只有管理员可以执行退款操作
        if (!"ADMIN".equals(currentUser.getRoleCode())) {
            throw new ServiceException("无权执行退款操作");
        }
        
        // 检查订单状态，只有已支付的订单可以退款
        if (order.getStatus() != 1) {
            throw new ServiceException("只有已支付的订单可以退款");
        }
        
        // 更新订单状态为已退款
        order.setStatus(3);
        ticketOrderMapper.updateById(order);
        
        // 恢复门票库存
        ticketService.restoreTicketStock(order.getTicketId(), order.getQuantity());
        
        // 发送订单退款消息 - 使用try-catch确保异常不会影响订单退款
        try {
            sendOrderMessage(order, "refund");
        } catch (Exception e) {
            logger.error("发送订单退款消息失败，但订单退款成功。订单号：{}，错误：{}", order.getOrderNo(), e.getMessage(), e);
            // 不重新抛出异常，确保订单退款成功
        }
    }
    
    /**
     * 完成订单
     */
    @Transactional
    public void completeOrder(Long orderId) {
        // 获取当前登录用户
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("用户未登录");
        }
        
        // 获取订单信息
        TicketOrder order = ticketOrderMapper.selectById(orderId);
        if (order == null) {
            throw new ServiceException("订单不存在");
        }
        
        // 只有管理员可以执行完成订单操作
        if (!"ADMIN".equals(currentUser.getRoleCode())) {
            throw new ServiceException("无权执行此操作");
        }
        
        // 检查订单状态，只有已支付的订单可以标记为完成
        if (order.getStatus() != 1) {
            throw new ServiceException("只有已支付的订单可以标记为已完成");
        }
        
        // 更新订单状态为已完成
        order.setStatus(4);
        ticketOrderMapper.updateById(order);
        
        // 发送订单完成消息 - 使用try-catch确保异常不会影响订单完成
        try {
            sendOrderMessage(order, "complete");
        } catch (Exception e) {
            logger.error("发送订单完成消息失败，但订单完成成功。订单号：{}，错误：{}", order.getOrderNo(), e.getMessage(), e);
            // 不重新抛出异常，确保订单完成成功
        }
    }
    
    /**
     * 获取用户订单列表
     */
    public Page<TicketOrder> getUserOrders(Long userId, Integer status, Integer currentPage, Integer size) {
        LambdaQueryWrapper<TicketOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TicketOrder::getUserId, userId);
        
        if (status != null) {
            queryWrapper.eq(TicketOrder::getStatus, status);
        }
        
        queryWrapper.orderByDesc(TicketOrder::getCreateTime);
        
        Page<TicketOrder> page = ticketOrderMapper.selectPage(new Page<>(currentPage, size), queryWrapper);
        
        // 填充订单关联信息
        for (TicketOrder order : page.getRecords()) {
            fillOrderDetails(order);
        }
        
        return page;
    }
    
    /**
     * 管理员获取所有订单列表
     */
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
        
        // 填充订单关联信息
        for (TicketOrder order : page.getRecords()) {
            fillOrderDetails(order);
        }
        
        return page;
    }
    
    /**
     * 获取订单详情
     */
    public TicketOrder getOrderDetail(Long orderId) {
        TicketOrder order = ticketOrderMapper.selectById(orderId);
        if (order == null) {
            throw new ServiceException("订单不存在");
        }
        
        // 填充订单关联信息
        fillOrderDetails(order);
        
        return order;
    }
    
    /**
     * 填充订单详情（关联信息）
     */
    private void fillOrderDetails(TicketOrder order) {
        // 填充门票信息
        Ticket ticket = ticketMapper.selectById(order.getTicketId());
        if (ticket != null) {
            order.setTicketName(ticket.getTicketName());
            
            // 填充景点信息 - 修复：使用正确的字段名
            ScenicSpot scenicSpot = scenicSpotMapper.selectById(ticket.getScenicId());
            if (scenicSpot != null) {
                order.setScenicName(scenicSpot.getName());
            }
        }
        
        // 填充用户信息
        User user = userMapper.selectById(order.getUserId());
        if (user != null) {
            order.setUsername(user.getUsername());
        }
    }
    
    /**
     * 生成订单号
     */
    private String generateOrderNo() {
        // 生成格式: 年月日时分秒 + 4位随机数
        String dateStr = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String randomStr = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 4);
        return dateStr + randomStr;
    }

    /**
     * 根据订单号查询订单
     */
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

    /**
     * 删除订单
     * 只有已完成(4)、已退款(3)或已取消(2)的订单可以删除
     */
    @Transactional
    public void deleteOrder(Long orderId) {
        // 获取当前登录用户
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("用户未登录");
        }
        
        // 获取订单信息
        TicketOrder order = ticketOrderMapper.selectById(orderId);
        if (order == null) {
            throw new ServiceException("订单不存在");
        }
        
        // 验证订单所属或管理员权限
        boolean isAdmin = "ADMIN".equals(currentUser.getRoleCode());
        if (!order.getUserId().equals(currentUser.getId()) && !isAdmin) {
            throw new ServiceException("无权操作此订单");
        }
        
        // 检查订单状态，只有已完成、已退款或已取消的订单可以删除
        if (order.getStatus() != 2 && order.getStatus() != 3 && order.getStatus() != 4) {
            throw new ServiceException("只有已完成、已退款或已取消的订单可以删除");
        }
        
        // 删除订单
        ticketOrderMapper.deleteById(orderId);
    }

    /**
     * 发送订单消息 - 改为同步日志记录
     */
    private void sendOrderMessage(TicketOrder order, String eventType) {
        try {
            // 记录订单事件日志
            logger.info("订单事件：{}，订单号：{}，状态：{}", eventType, order.getOrderNo(), order.getStatus());
            
            // 暂时完全禁用邮件发送，只记录日志
            switch (eventType) {
                case "create":
                    logger.info("订单创建通知：订单号 {} 已创建", order.getOrderNo());
                    break;
                case "pay":
                    logger.info("订单支付通知：订单号 {} 已支付", order.getOrderNo());
                    break;
                case "cancel":
                    logger.info("订单取消通知：订单号 {} 已取消", order.getOrderNo());
                    break;
                case "refund":
                    logger.info("订单退款通知：订单号 {} 已退款", order.getOrderNo());
                    break;
                case "complete":
                    logger.info("订单完成通知：订单号 {} 已完成", order.getOrderNo());
                    break;
            }
            
        } catch (Exception e) {
            logger.error("发送订单消息失败，但订单处理继续：{}", e.getMessage(), e);
        }
    }
    
    /**
     * 发送订单通知邮件 - 暂时禁用
     */
    private void sendOrderNotificationEmail(TicketOrder order, String subject, String content) {
        // 暂时完全禁用邮件发送
        logger.info("邮件通知（已禁用）：订单号 {}，主题：{}，内容：{}", order.getOrderNo(), subject, content);
    }

    /**
     * 获取用户订单统计信息
     */
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
                case 0:
                    stats.put("pending", (Integer) stats.get("pending") + 1);
                    break;
                case 1:
                    stats.put("paid", (Integer) stats.get("paid") + 1);
                    break;
                case 2:
                    stats.put("cancelled", (Integer) stats.get("cancelled") + 1);
                    break;
                case 3:
                    stats.put("refunded", (Integer) stats.get("refunded") + 1);
                    break;
                case 4:
                    stats.put("completed", (Integer) stats.get("completed") + 1);
                    break;
            }
        }
        
        return stats;
    }

    /**
     * 获取订单状态文本
     */
    private String getOrderStatusText(Integer status) {
        switch (status) {
            case 0: return "待支付";
            case 1: return "已支付";
            case 2: return "已取消";
            case 3: return "已退款";
            case 4: return "已完成";
            default: return "未知状态";
        }
    }
}