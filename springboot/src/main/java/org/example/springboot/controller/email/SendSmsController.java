package org.example.springboot.controller.email;

import jakarta.annotation.Resource;
import org.example.springboot.common.Result;
import org.example.springboot.entity.User;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("/sms")
public class SendSmsController {
    private static final Logger LOGGER = LoggerFactory.getLogger(SendSmsController.class);

    // 用于存储手机号和验证码的映射，实际项目中应该使用Redis
    private static final Map<String, CodeInfo> PHONE_CODE_MAP = new ConcurrentHashMap<>();
    
    // 验证码有效期（毫秒）
    private static final long CODE_EXPIRE_TIME = 5 * 60 * 1000;
    
    @Resource
    UserService userService;

    @GetMapping("/code/{phone}")
    public Result<?> sendCode(@PathVariable String phone) {
        // 检查手机号格式
        if (!phone.matches("^1[3-9]\\d{9}$")) {
            throw new ServiceException("手机号格式不正确");
        }
        
        // 检查手机号是否已注册
        try {
            User existingUser = userService.getByPhone(phone);
            if (existingUser != null) {
                throw new ServiceException("手机号已被注册");
            }
        } catch (ServiceException e) {
            // 手机号不存在，可以继续发送验证码
            if (!e.getMessage().equals("手机号不存在")) {
                throw e;
            }
        }

        try {
            int code = generateAndSendCode(phone);
            return Result.success(code);
        } catch (Exception e) {
            throw new ServiceException("验证码发送失败：" + e.getMessage());
        }
    }

    @GetMapping("/verify")
    public Result<?> verifyCode(@RequestParam String phone, @RequestParam String code) {
        if (!phone.matches("^1[3-9]\\d{9}$")) {
            throw new ServiceException("手机号格式不正确");
        }
        
        CodeInfo codeInfo = PHONE_CODE_MAP.get(phone);
        if (codeInfo == null) {
            throw new ServiceException("验证码不存在或已过期");
        }
        
        // 检查验证码是否过期
        if (System.currentTimeMillis() - codeInfo.getCreateTime() > CODE_EXPIRE_TIME) {
            PHONE_CODE_MAP.remove(phone);
            throw new ServiceException("验证码已过期");
        }
        
        // 验证码比对
        if (!codeInfo.getCode().equals(code)) {
            throw new ServiceException("验证码错误");
        }
        
        // 验证成功后移除验证码
        PHONE_CODE_MAP.remove(phone);
        return Result.success();
    }

    private int generateAndSendCode(String phone) {
        Random random = new Random();
        int code = random.nextInt(899999) + 100000;
        
        // 将验证码存入内存Map
        PHONE_CODE_MAP.put(phone, new CodeInfo(String.valueOf(code), System.currentTimeMillis()));
        
        // 这里应该调用真实的短信发送服务，如阿里云、腾讯云等
        // 为演示，只打印日志
        LOGGER.info("向手机号 {} 发送验证码: {}", phone, code);
        
        return code;
    }
    
    // 内部类，存储验证码信息
    private static class CodeInfo {
        private final String code;
        private final long createTime;
        
        public CodeInfo(String code, long createTime) {
            this.code = code;
            this.createTime = createTime;
        }
        
        public String getCode() {
            return code;
        }
        
        public long getCreateTime() {
            return createTime;
        }
    }
} 