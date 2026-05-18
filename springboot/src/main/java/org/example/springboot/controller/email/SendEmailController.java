package org.example.springboot.controller.email;

import jakarta.annotation.Resource;
import org.example.springboot.common.Result;
import org.example.springboot.entity.User;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.service.EmailService;
import org.example.springboot.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("/email")
public class SendEmailController {
    private static final Logger LOGGER = LoggerFactory.getLogger(SendEmailController.class);

    @Resource
    private EmailService emailService;
    
    @Resource
    private UserService userService;

    @GetMapping("/code/{email}")
    public Result<?> sendCode(@PathVariable String email) {
        try {
            // 检查邮箱是否已注册
            try {
                if (userService.getByEmail(email) != null) {
                    return Result.error("邮箱已被注册");
                }
            } catch (ServiceException e) {
                // 邮箱不存在，可以继续发送验证码
                if (!e.getMessage().equals("邮箱不存在")) {
                    throw e;
                }
            }

            // 同步发送验证码邮件
            String code = emailService.sendVerificationCodeAsync(email);
            LOGGER.info("验证码邮件发送成功：{}", email);
            
            return Result.success(code);
        } catch (Exception e) {
            LOGGER.error("验证码发送失败：{}", e.getMessage(), e);
            return Result.error("验证码发送失败：" + e.getMessage());
        }
    }

    @GetMapping("/findByEmail/{email}")
    public Result<?> findByEmail(@PathVariable String email) {
        LOGGER.info("FIND BY EMAIL: {}", email);

        try {
            // 检查邮箱是否存在
            User user = userService.getByEmail(email);
            if (user == null) {
                return Result.error("邮箱不存在");
            }

            // 同步发送重置密码邮件
            String code = emailService.sendResetPasswordEmailAsync(email);
            LOGGER.info("密码重置邮件发送成功：{}", email);
            
            return Result.success(code);
        } catch (ServiceException e) {
            if (e.getMessage().equals("邮箱不存在")) {
                return Result.error("邮箱不存在");
            }
            LOGGER.error("密码重置邮件发送失败：{}", e.getMessage(), e);
            return Result.error("密码重置邮件发送失败：" + e.getMessage());
        } catch (Exception e) {
            LOGGER.error("密码重置邮件发送异常：{}", e.getMessage(), e);
            return Result.error("邮件发送异常，请联系管理员");
        }
    }
    
    @GetMapping("/notify/{email}")
    public Result<?> sendNotification(
            @PathVariable String email, 
            @RequestParam String subject, 
            @RequestParam String content) {
        try {
            // 检查邮箱是否存在
            try {
                userService.getByEmail(email);
            } catch (ServiceException e) {
                return Result.error("邮箱不存在");
            }

            // 同步发送通知邮件
            emailService.sendNotificationEmailAsync(email, subject, content);
            LOGGER.info("通知邮件发送成功：{}", email);
            
            return Result.success("通知邮件发送成功");
        } catch (Exception e) {
            LOGGER.error("通知邮件发送异常：{}", e.getMessage(), e);
            return Result.error("通知邮件发送失败：" + e.getMessage());
        }
    }
    
    @GetMapping("/verify")
    public Result<?> verifyEmailCode(@RequestParam String email, @RequestParam String code) {
        try {
            boolean verified = emailService.verifyCode(email, code);
            if (verified) {
                return Result.success(true);
            } else {
                return Result.error("验证码错误或已过期");
            }
        } catch (Exception e) {
            LOGGER.error("验证码验证失败：{}", e.getMessage(), e);
            return Result.error("验证码验证失败：" + e.getMessage());
        }
    }
}
