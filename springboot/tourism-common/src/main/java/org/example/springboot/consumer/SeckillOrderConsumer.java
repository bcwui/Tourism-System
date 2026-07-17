package org.example.springboot.consumer;

import jakarta.annotation.Resource;
import org.example.springboot.DTO.SeckillMessageDTO;
import org.example.springboot.entity.SeckillActivity;
import org.example.springboot.entity.Ticket;
import org.example.springboot.entity.TicketOrder;
import org.example.springboot.entity.User;
import org.example.springboot.mapper.SeckillActivityMapper;
import org.example.springboot.mapper.TicketMapper;
import org.example.springboot.mapper.TicketOrderMapper;
import org.example.springboot.mapper.UserMapper;
import org.example.springboot.service.SeckillService;
import org.example.springboot.service.TicketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.example.springboot.config.RabbitMQConfig.SECKILL_ORDER_QUEUE;

@Component
@ConditionalOnProperty(name = "mq.consumer.seckill", havingValue = "true")
public class SeckillOrderConsumer {

    private static final Logger logger = LoggerFactory.getLogger(SeckillOrderConsumer.class);

    @Resource
    private TicketOrderMapper ticketOrderMapper;
    @Resource
    private TicketMapper ticketMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private TicketService ticketService;
    @Resource
    private SeckillActivityMapper seckillActivityMapper;
    @Resource
    private SeckillService seckillService;

    @RabbitListener(queues = SECKILL_ORDER_QUEUE)
    @Transactional
    public void handleSeckillOrder(SeckillMessageDTO msg) {
        logger.info("处理秒杀订单: userId={}, ticketId={}, activityId={}", msg.getUserId(), msg.getTicketId(), msg.getActivityId());
        try {
            SeckillActivity activity = seckillActivityMapper.selectById(msg.getActivityId());
            Ticket ticket = ticketMapper.selectById(msg.getTicketId());
            User user = userMapper.selectById(msg.getUserId());

            if (activity == null || ticket == null || user == null) {
                logger.error("秒杀订单数据异常: activity={}, ticket={}, user={}", msg.getActivityId(), msg.getTicketId(), msg.getUserId());
                seckillService.rollbackSeckill(msg.getActivityId(), msg.getUserId(), msg.getQuantity());
                return;
            }

            // DB层面原子扣减（最终保障）
            if (!ticketService.deductStock(msg.getTicketId(), msg.getQuantity())) {
                logger.error("秒杀订单创建失败，门票库存不足: ticketId={}", msg.getTicketId());
                seckillService.rollbackSeckill(msg.getActivityId(), msg.getUserId(), msg.getQuantity());
                return;
            }

            // 扣减秒杀活动库存
            int activityDeducted = seckillActivityMapper.deductStock(msg.getActivityId(), msg.getQuantity());
            if (activityDeducted <= 0) {
                logger.error("秒杀活动库存扣减失败: activityId={}", msg.getActivityId());
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                seckillService.rollbackSeckill(msg.getActivityId(), msg.getUserId(), msg.getQuantity());
                return;
            }

            // 创建订单
            TicketOrder order = new TicketOrder();
            order.setOrderNo(generateOrderNo());
            order.setUserId(msg.getUserId());
            order.setTicketId(msg.getTicketId());
            order.setQuantity(msg.getQuantity());
            order.setVisitorName(msg.getVisitorName());
            order.setVisitorPhone(msg.getVisitorPhone());
            order.setIdCard(msg.getIdCard());
            order.setVisitDate(msg.getVisitDate());
            order.setTotalAmount(activity.getSeckillPrice().multiply(BigDecimal.valueOf(msg.getQuantity())));
            order.setStatus(0); // 待支付
            ticketOrderMapper.insert(order);

            logger.info("秒杀订单创建成功: orderNo={}, userId={}, ticketId={}", order.getOrderNo(), msg.getUserId(), msg.getTicketId());
        } catch (Exception e) {
            logger.error("秒杀订单处理异常，回滚并丢弃消息: {}", e.getMessage(), e);
            // 标记事务回滚，确保 DB 扣减被撤销
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            // 恢复 Redis 库存和用户资格（仅一次，不重试避免重复 rollback）
            seckillService.rollbackSeckill(msg.getActivityId(), msg.getUserId(), msg.getQuantity());
            // 不抛出异常，消息ACK不再重试，防止 rollbackSeckill 被重复调用导致 Redis 库存膨胀
        }
    }

    private String generateOrderNo() {
        String dateStr = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String randomStr = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 4);
        return "SK" + dateStr + randomStr;
    }
}
