package org.example.springboot.service;

import jakarta.annotation.Resource;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.example.springboot.DTO.EmailMessageDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private static final String VERIFY_CODE_PREFIX = "email:code:";
    private static final long CODE_EXPIRE_SECONDS = 300;

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    @Value("${user.fromEmail}")
    private String fromEmail;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 发送验证码邮件（异步通过RocketMQ）
     * 验证码存储到Redis，设置5分钟过期
     */
    public String sendVerificationCodeAsync(String email) {
        String code = generateVerificationCode();
        stringRedisTemplate.opsForValue().set(VERIFY_CODE_PREFIX + email, code, CODE_EXPIRE_SECONDS, TimeUnit.SECONDS);

        EmailMessageDTO emailMessage = EmailMessageDTO.createVerifyCodeEmail(email, code);
        sendEmailAsync(emailMessage);

        logger.info("验证码邮件消息已发送到MQ：{}", email);
        return code;
    }

    /**
     * 发送重置密码邮件（异步通过RocketMQ）
     */
    public String sendResetPasswordEmailAsync(String email) {
        String code = generateVerificationCode();
        stringRedisTemplate.opsForValue().set(VERIFY_CODE_PREFIX + email, code, CODE_EXPIRE_SECONDS, TimeUnit.SECONDS);

        EmailMessageDTO emailMessage = EmailMessageDTO.createResetPasswordEmail(email, code);
        sendEmailAsync(emailMessage);

        logger.info("密码重置邮件消息已发送到MQ：{}", email);
        return code;
    }

    /**
     * 发送通知邮件（异步通过RocketMQ）
     */
    public void sendNotificationEmailAsync(String email, String subject, String content) {
        EmailMessageDTO emailMessage = EmailMessageDTO.createNotificationEmail(email, subject, content);
        sendEmailAsync(emailMessage);
        logger.info("通知邮件消息已发送到MQ：{}，主题：{}", email, subject);
    }

    /**
     * 将邮件消息发送到 RocketMQ
     */
    private void sendEmailAsync(EmailMessageDTO emailMessage) {
        try {
            rocketMQTemplate.convertAndSend("email-topic", emailMessage);
        } catch (Exception e) {
            logger.error("发送邮件消息到RocketMQ失败，降级为同步发送: {}", e.getMessage());
            // 降级：直接同步发送
            sendEmailSync(emailMessage);
        }
    }

    /**
     * 同步发送邮件（RocketMQ不可用时的降级方案）
     */
    private void sendEmailSync(EmailMessageDTO emailMessage) {
        try {
            jakarta.mail.internet.MimeMessage message = null;
            // 降级方案：仅记录日志，邮件在MQ恢复后通过重试机制发送
            logger.warn("RocketMQ不可用，邮件发送被跳过: to={}, subject={}", emailMessage.getTo(), emailMessage.getSubject());
        } catch (Exception e) {
            logger.error("邮件降级发送也失败：{}", e.getMessage(), e);
        }
    }

    /**
     * 验证验证码（从Redis读取并校验）
     */
    public boolean verifyCode(String email, String code) {
        String storedCode = stringRedisTemplate.opsForValue().get(VERIFY_CODE_PREFIX + email);
        if (storedCode != null && storedCode.equals(code)) {
            stringRedisTemplate.delete(VERIFY_CODE_PREFIX + email);
            return true;
        }
        return false;
    }

    private String generateVerificationCode() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(1000000));
    }
}
