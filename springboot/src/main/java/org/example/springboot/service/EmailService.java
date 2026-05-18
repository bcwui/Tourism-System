package org.example.springboot.service;

import jakarta.annotation.Resource;
import org.example.springboot.DTO.EmailMessageDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 邮件服务类
 */
@Service
public class EmailService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    
    private static final ConcurrentHashMap<String, String> EMAIL_CODE_MAP = new ConcurrentHashMap<>();
    
    @Resource
    private JavaMailSender javaMailSender;
    
    @Value("${user.fromEmail}")
    private String fromEmail;
    
    /**
     * 发送验证码邮件（同步）
     * 
     * @param email 收件人邮箱
     * @return 验证码
     */
    public String sendVerificationCodeAsync(String email) {
        String code = generateVerificationCode();
        
        // 存储验证码
        EMAIL_CODE_MAP.put(email, code);
        
        // 创建邮件消息DTO
        EmailMessageDTO emailMessage = EmailMessageDTO.createVerifyCodeEmail(email, code);
        
        // 直接同步发送邮件
        sendEmail(emailMessage);
        
        logger.info("发送验证码邮件：{}，验证码：{}", email, code);
        
        return code;
    }
    
    /**
     * 发送重置密码邮件（同步）
     * 
     * @param email 收件人邮箱
     * @return 验证码
     */
    public String sendResetPasswordEmailAsync(String email) {
        String code = generateVerificationCode();
        
        // 存储验证码
        EMAIL_CODE_MAP.put(email, code);
        
        // 创建邮件消息DTO
        EmailMessageDTO emailMessage = EmailMessageDTO.createResetPasswordEmail(email, code);
        
        // 直接同步发送邮件
        sendEmail(emailMessage);
        
        logger.info("发送密码重置邮件：{}，验证码：{}", email, code);
        
        return code;
    }
    
    /**
     * 发送通知邮件（同步）
     * 
     * @param email   收件人邮箱
     * @param subject 邮件主题
     * @param content 邮件内容
     */
    public void sendNotificationEmailAsync(String email, String subject, String content) {
        // 创建邮件消息DTO
        EmailMessageDTO emailMessage = EmailMessageDTO.createNotificationEmail(email, subject, content);
        
        // 直接同步发送邮件
        sendEmail(emailMessage);
        
        logger.info("发送通知邮件：{}，主题：{}", email, subject);
    }
    
    /**
     * 发送邮件（同步）
     * 
     * @param emailMessage 邮件消息对象
     */
    public void sendEmail(EmailMessageDTO emailMessage) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(emailMessage.getTo());
            message.setSubject(emailMessage.getSubject());
            message.setText(emailMessage.getContent());
            
            javaMailSender.send(message);
            
            logger.info("邮件发送成功：{}", emailMessage.getTo());
        } catch (Exception e) {
            logger.error("邮件发送失败：{}", e.getMessage(), e);
            throw new RuntimeException("邮件发送失败：" + e.getMessage());
        }
    }
    
    /**
     * 验证验证码
     * 
     * @param email 邮箱
     * @param code  验证码
     * @return 是否验证成功
     */
    public boolean verifyCode(String email, String code) {
        String storedCode = EMAIL_CODE_MAP.get(email);
        
        if (storedCode != null && storedCode.equals(code)) {
            // 验证成功后移除验证码
            EMAIL_CODE_MAP.remove(email);
            return true;
        }
        
        return false;
    }
    
    /**
     * 生成6位随机验证码
     * 
     * @return 验证码
     */
    private String generateVerificationCode() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(1000000));
    }
}