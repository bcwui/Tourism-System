package org.example.springboot.consumer;

import jakarta.annotation.Resource;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.example.springboot.DTO.EmailMessageDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@RocketMQMessageListener(
    topic = "email-topic",
    consumerGroup = "tourism-email-consumer-group"
)
public class EmailMessageConsumer implements RocketMQListener<EmailMessageDTO> {

    private static final Logger logger = LoggerFactory.getLogger(EmailMessageConsumer.class);

    @Resource
    private JavaMailSender javaMailSender;

    @Value("${user.fromEmail}")
    private String fromEmail;

    @Override
    public void onMessage(EmailMessageDTO emailMessage) {
        logger.info("收到邮件发送消息: type={}, to={}", emailMessage.getType(), emailMessage.getTo());
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(emailMessage.getTo());
            message.setSubject(emailMessage.getSubject());
            message.setText(emailMessage.getContent());
            javaMailSender.send(message);
            logger.info("邮件异步发送成功: {}", emailMessage.getTo());
        } catch (Exception e) {
            logger.error("邮件异步发送失败: to={}, error={}", emailMessage.getTo(), e.getMessage(), e);
        }
    }
}
