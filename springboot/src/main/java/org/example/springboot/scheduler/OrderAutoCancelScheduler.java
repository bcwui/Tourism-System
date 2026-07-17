package org.example.springboot.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.example.springboot.entity.TicketOrder;
import org.example.springboot.mapper.TicketOrderMapper;
import org.example.springboot.service.TicketOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class OrderAutoCancelScheduler {

    private static final Logger logger = LoggerFactory.getLogger(OrderAutoCancelScheduler.class);

    @Resource
    private TicketOrderMapper ticketOrderMapper;

    @Resource
    private TicketOrderService ticketOrderService;

    @Scheduled(fixedRate = 30000)
    public void autoCancelExpiredOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(15);
        LambdaQueryWrapper<TicketOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TicketOrder::getStatus, 0)
                .lt(TicketOrder::getCreateTime, deadline);
        List<TicketOrder> expiredOrders = ticketOrderMapper.selectList(queryWrapper);

        if (expiredOrders.isEmpty()) {
            return;
        }

        logger.info("发现{}个过期未支付订单，开始自动取消", expiredOrders.size());
        for (TicketOrder order : expiredOrders) {
            try {
                ticketOrderService.cancelOrderSystem(order);
            } catch (Exception e) {
                logger.error("自动取消订单失败，订单号：{}，错误：{}", order.getOrderNo(), e.getMessage(), e);
            }
        }
    }
}
