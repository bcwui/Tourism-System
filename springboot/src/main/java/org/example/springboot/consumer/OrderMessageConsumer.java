package org.example.springboot.consumer;

import org.example.springboot.DTO.OrderMessageDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import static org.example.springboot.config.RabbitMQConfig.ORDER_QUEUE;

@Component
@ConditionalOnProperty(name = "mq.consumer.order", havingValue = "true")
public class OrderMessageConsumer {

    private static final Logger logger = LoggerFactory.getLogger(OrderMessageConsumer.class);

    @RabbitListener(queues = ORDER_QUEUE)
    public void handleOrderMessage(OrderMessageDTO message) {
        logger.info("收到订单消息: eventType={}, orderNo={}, status={}",
                message.getEventType(), message.getOrderNo(), message.getStatus());

        switch (message.getEventType()) {
            case "ORDER_CREATED":
                logger.info("订单创建通知 - 订单号: {}, 金额: {}, 用户: {}",
                        message.getOrderNo(), message.getAmount(), message.getUsername());
                break;
            case "ORDER_PAID":
                logger.info("订单支付通知 - 订单号: {}, 支付时间: {}",
                        message.getOrderNo(), message.getPaymentTime());
                break;
            case "ORDER_CANCELLED":
                logger.info("订单取消通知 - 订单号: {}", message.getOrderNo());
                break;
            case "ORDER_REFUNDED":
                logger.info("订单退款通知 - 订单号: {}, 金额: {}",
                        message.getOrderNo(), message.getAmount());
                break;
            case "ORDER_COMPLETED":
                logger.info("订单完成通知 - 订单号: {}", message.getOrderNo());
                break;
        }
    }
}
