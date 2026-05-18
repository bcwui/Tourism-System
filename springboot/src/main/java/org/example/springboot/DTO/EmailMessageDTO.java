package org.example.springboot.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 邮件消息DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailMessageDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 消息类型：验证码、通知等
     */
    private String type;
    
    /**
     * 收件人邮箱
     */
    private String to;
    
    /**
     * 邮件主题
     */
    private String subject;
    
    /**
     * 邮件内容
     */
    private String content;
    
    /**
     * 其他参数，如验证码等
     */
    private String extraData;
    
    /**
     * 创建验证码邮件
     */
    public static EmailMessageDTO createVerifyCodeEmail(String to, String code) {
        EmailMessageDTO email = new EmailMessageDTO();
        email.setType("verifyCode");
        email.setTo(to);
        email.setSubject("验证码邮件");
        email.setContent("您的验证码为：" + code + "，有效期为5分钟，请勿泄露给他人。");
        email.setExtraData(code);
        return email;
    }
    
    /**
     * 创建密码重置邮件
     */
    public static EmailMessageDTO createResetPasswordEmail(String to, String code) {
        EmailMessageDTO email = new EmailMessageDTO();
        email.setType("resetPassword");
        email.setTo(to);
        email.setSubject("密码重置验证码");
        email.setContent("您的密码重置验证码为：" + code + "，有效期为5分钟，请勿泄露给他人。");
        email.setExtraData(code);
        return email;
    }
    
    /**
     * 创建通知邮件
     */
    public static EmailMessageDTO createNotificationEmail(String to, String subject, String content) {
        EmailMessageDTO email = new EmailMessageDTO();
        email.setType("notification");
        email.setTo(to);
        email.setSubject(subject);
        email.setContent(content);
        return email;
    }
} 