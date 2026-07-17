package org.example.springboot.consumer;

import org.example.springboot.DTO.EmailMessageDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import static org.example.springboot.config.RabbitMQConfig.EMAIL_QUEUE;

@Component
@ConditionalOnProperty(name = "mq.consumer.email", havingValue = "true")
public class EmailMessageConsumer {

    private static final Logger logger = LoggerFactory.getLogger(EmailMessageConsumer.class);

    @Autowired(required = false)
    private JavaMailSender javaMailSender;

    @Value("${user.fromEmail:}")
    private String fromEmail;

    @RabbitListener(queues = EMAIL_QUEUE)
    public void handleEmailMessage(EmailMessageDTO emailMessage) {
        if (javaMailSender == null) {
            logger.warn("JavaMailSender 未配置，忽略邮件消息: type={}, to={}", emailMessage.getType(), emailMessage.getTo());
            return;
        }
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
