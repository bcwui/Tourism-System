package org.example.springboot.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.example.springboot.common.Result;
import org.example.springboot.entity.User;
import org.example.springboot.DTO.UserPasswordUpdateDTO;
import org.example.springboot.mapper.UserMapper;
import org.example.springboot.service.EmailService;
import org.example.springboot.service.UserService;
import org.example.springboot.util.JwtTokenUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Tag(name="用户管理接口")
@RestController
@RequestMapping("/user")
public class UserController {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserController.class);
    @Resource
    private UserService userService;
    @Autowired
    private UserMapper userMapper;
    @Resource
    private EmailService emailService;

    @Operation(summary = "根据id获取用户信息")
    @GetMapping("/{id}")
    public Result<?> getById(@PathVariable Long id) {
        // 如果用户不存在会抛出异常，由全局异常处理器处理
        User user = userService.getUserById(id);
        return Result.success(user);
    }

    @Operation(summary = "根据username获取用户信息")
    @GetMapping("/username/{username}")
    public Result<?> getUserByUsername(@PathVariable String username) {
        // 不存在的用户会抛出异常
        User user = userService.getByUsername(username);
        return Result.success(user);
    }

    @Operation(summary = "登录")
    @PostMapping("/login")
    public Result<?> login(@RequestBody User user) {
        User loginUser = userService.login(user);
        return Result.success(loginUser);
    }

    @Operation(summary = "邮箱登录")
    @PostMapping("/login/email")
    public Result<?> loginByEmail(@RequestBody User user) {
        if (!StringUtils.hasText(user.getEmail()) || !StringUtils.hasText(user.getPassword())) {
            return Result.error("邮箱和密码不能为空");
        }
        User loginUser = userService.loginByEmail(user);
        return Result.success(loginUser);
    }

    @Operation(summary = "密码修改")
    @PutMapping("/password/{id}")
    public Result<?> updatePassword(@PathVariable Long id, @RequestBody UserPasswordUpdateDTO userPasswordUpdate) {
        // 密码修改失败会抛出异常
        userService.updatePassword(id, userPasswordUpdate);
        return Result.success("密码修改成功");
    }

    @Operation(summary = "忘记密码")
    @GetMapping("/forget")
    public Result<?> forgetPassword(@RequestParam String email, @RequestParam String newPassword) {
        // 密码重置失败会抛出异常
        userService.forgetPassword(email, newPassword);
        return Result.success("密码重置成功");
    }

    @Operation(summary = "分页查询用户")
    @GetMapping("/page")
    public Result<?> getUsersByPage(
            @RequestParam(defaultValue = "") String username,
            @RequestParam(defaultValue = "") String sex,
            @RequestParam(defaultValue = "") String nickname,
            @RequestParam(defaultValue = "") String roleCode,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size) {
        Page<User> page = userService.getUsersByPage(username, sex, nickname, roleCode, currentPage, size);
        return Result.success(page);
    }

    @Operation(summary = "根据角色获取用户列表")
    @GetMapping("/role/{roleCode}")
    public Result<?> getUserByRole(@PathVariable String roleCode) {
        List<User> users = userService.getUserByRole(roleCode);
        return Result.success(users);
    }

    @Operation(summary = "批量删除用户")
    @DeleteMapping("/deleteBatch")
    public Result<?> deleteBatch(@RequestParam List<Integer> ids) {
        userService.deleteBatch(ids);
        return Result.success("批量删除成功");
    }

    @Operation(summary = "获取所有用户")
    @GetMapping("/all")
    public Result<?> getUserList() {
        List<User> list = userService.getUserList();
        return Result.success(list);
    }

    @Operation(summary = "创建新用户")
    @PostMapping("/add")
    public Result<?> createUser(@RequestBody  User user) {
        userService.createUser(user);
        return Result.success("创建成功");
    }

    @Operation(summary = "更新用户信息")
    @PutMapping("/{id}")
    public Result<?> updateUser(@PathVariable Long id, @RequestBody User user) {
        // 更新失败会抛出具体原因的异常
        userService.updateUser(id, user);
        return Result.success("更新成功");
    }

    @Operation(summary = "根据id删除用户")
    @DeleteMapping("/delete/{id}")
    public Result<?> deleteUserById(@PathVariable Long id) {
        // 删除失败会抛出异常
        userService.deleteUserById(id);
        return Result.success("删除成功");
    }

    @Operation(summary = "获取当前登录用户信息")
    @GetMapping("/current")
    public Result<?> getCurrentUser() {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            return Result.error("获取当前用户信息失败，请重新登录");
        }
        return Result.success(currentUser);
    }
    @Operation(summary = "修改用户状态")
    @PutMapping("/status/{userId}")
    public Result<?> updateStatus(@PathVariable Long userId, @RequestParam Integer status) {
        User user = userMapper.selectById(userId);
        user.setStatus(status);
        userService.updateUser(userId,user);
        return Result.success();

    }

    @Operation(summary = "管理员重置用户密码")
    @PutMapping("/resetPassword/{id}")
    public Result<?> resetPassword(@PathVariable Long id, @RequestBody UserPasswordUpdateDTO dto) {
        var currentUser = org.example.springboot.util.JwtTokenUtils.getCurrentUser();
        if (currentUser == null || !"ADMIN".equals(currentUser.getRoleCode())) {
            return Result.error("无权限");
        }
        userService.resetPassword(id, dto.getNewPassword());
        return Result.success("重置密码成功");
    }

    @Operation(summary = "用户登出")
    @PostMapping("/logout")
    public Result<?> logout(HttpServletRequest request) {
        String token = request.getHeader("token");
        if (StringUtils.isEmpty(token)) {
            token = request.getParameter("token");
        }
        
        userService.logout(token);
        return Result.success("登出成功");
    }
    
    @Operation(summary = "获取在线用户数量")
    @GetMapping("/online/count")
    public Result<?> getOnlineUserCount() {
        long count = userService.getOnlineUserCount();
        return Result.success(count);
    }
    
    @Operation(summary = "获取在线用户列表")
    @GetMapping("/online/list")
    public Result<?> getOnlineUserList() {
        var currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null || !"ADMIN".equals(currentUser.getRoleCode())) {
            return Result.error("无权限");
        }
        
        List<Map<String, Object>> onlineUsers = userService.getOnlineUserList();
        return Result.success(onlineUsers);
    }

    @Operation(summary = "手机号注册")
    @PostMapping("/register/phone")
    public Result<?> registerByPhone(@RequestBody User user, @RequestParam String verifyCode) {
        // 验证参数
        if (user == null || !StringUtils.hasText(user.getPhone()) || !StringUtils.hasText(user.getPassword())) {
            return Result.error("手机号和密码不能为空");
        }
        
        // 调用短信验证接口验证验证码
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = "http://localhost:8080/sms/verify?phone={phone}&code={code}";
            Map<String, String> params = new HashMap<>();
            params.put("phone", user.getPhone());
            params.put("code", verifyCode);
            
            // 发送验证请求
            restTemplate.getForObject(url, Result.class, params);
            
            // 生成默认用户名
            if (!StringUtils.hasText(user.getUsername())) {
                user.setUsername("user_" + user.getPhone().substring(user.getPhone().length() - 4));
            }
            
            // 设置默认角色
            user.setRoleCode("USER");
            
            // 创建用户
            userService.createUser(user);
            return Result.success("注册成功");
        } catch (Exception e) {
            LOGGER.error("手机号注册失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    @Operation(summary = "邮箱验证码注册")
    @PostMapping("/register/email")
    public Result<?> registerByEmail(@RequestBody User user, @RequestParam String verifyCode) {
        // 验证参数
        if (user == null || !StringUtils.hasText(user.getEmail()) || !StringUtils.hasText(user.getPassword())) {
            return Result.error("邮箱和密码不能为空");
        }
        
        try {
            // 验证验证码
            boolean isValid = emailService.verifyCode(user.getEmail(), verifyCode);
            if (!isValid) {
                return Result.error("验证码错误或已过期");
            }
            
            // 生成默认用户名
            if (!StringUtils.hasText(user.getUsername())) {
                String username = generateUniqueUsername(user);
                user.setUsername(username);
            }
            
            // 设置默认角色
            user.setRoleCode("USER");
            
            // 创建用户
            userService.createUser(user);
            return Result.success("注册成功");
        } catch (Exception e) {
            LOGGER.error("邮箱注册失败", e);
            return Result.error(e.getMessage());
        }
    }


    // 定义一个方法用于生成随机字母
    public  String getRandomLetters() {
        String letters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            int index = random.nextInt(letters.length());
            sb.append(letters.charAt(index));
        }
        return sb.toString();
    }

    // 生成默认用户名，确保不重复
    public  String generateUniqueUsername(User user) {
        String username = user.getUsername();
        if (!StringUtils.hasText(username)) {
            String emailPrefix = user.getEmail().split("@")[0];
            // 初始化随机部分
            String randomSuffix = getRandomLetters();
            // 生成初始用户名
            String candidateUsername = emailPrefix + randomSuffix;
            // 检查是否重复，如果重复则循环生成新的直到不重复
            while (isUsernameExist(candidateUsername)) {
                randomSuffix = getRandomLetters();
                candidateUsername = emailPrefix + randomSuffix;
            }
            user.setUsername(candidateUsername);
            return candidateUsername;
        } else {
            // 如果已有用户名，检查是否重复，重复则按照相同逻辑生成
            if (isUsernameExist(username)) {
                String emailPrefix = user.getEmail().split("@")[0];
                String randomSuffix = getRandomLetters();
                String candidateUsername = emailPrefix + randomSuffix;
                while (isUsernameExist(candidateUsername)) {
                    randomSuffix = getRandomLetters();
                    candidateUsername = emailPrefix + randomSuffix;
                }
                user.setUsername(candidateUsername);
                return candidateUsername;
            } else {
                return username;
            }
        }
    }

    // 检查用户名是否在数据库中存在
    public  boolean isUsernameExist(String username) {
        // 实现数据库查询逻辑，返回是否存在该用户名

        return userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername,username))>0;
    }
}
