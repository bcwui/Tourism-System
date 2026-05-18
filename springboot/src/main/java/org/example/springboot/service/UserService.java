package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;

import org.example.springboot.annotation.RedisCache;
import org.example.springboot.entity.User;
import org.example.springboot.DTO.UserPasswordUpdateDTO;
import org.example.springboot.enumClass.AccountStatus;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.UserMapper;
import org.example.springboot.util.JwtTokenUtils;
import org.example.springboot.util.RedisUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
    private static final String USER_CACHE_PREFIX = "user:";
    private static final String USER_LIST_CACHE_KEY = "user:list";
    private static final String USER_PAGE_CACHE_PREFIX = "user:page:";
    private static final long USER_CACHE_EXPIRE = 3600; // 缓存1小时
    
    @Resource
    private UserMapper userMapper;
    
    @Resource
    private RedisUtil redisUtil;
    
    @Value("${user.defaultPassword}")
    private String DEFAULT_PWD;

    @Resource
    private PasswordEncoder bCryptPasswordEncoder;

    @RedisCache(prefix = "user", key = "'email:' + #email", expire = 3600)
    public User getByEmail(String email) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        if (user == null) {
            throw new ServiceException("邮箱不存在");
        }
        return user;
    }

    public User login(User user) {
        User dbUser = getByUsername(user.getUsername());
        // 用户存在性检查已经在 getByUsername 中处理
        if (dbUser.getStatus().equals(AccountStatus.PENDING_REVIEW.getValue())) {
            throw new ServiceException("账号正在审核");
        }
        if (dbUser.getStatus().equals(AccountStatus.REVIEW_FAILED.getValue())) {
            throw new ServiceException("账号审核不通过，请修改个人信息");
        }
        if (!bCryptPasswordEncoder.matches(user.getPassword(), dbUser.getPassword())) {
            throw new ServiceException("用户名或密码错误");
        }
        
        return generateLoginResponse(dbUser);
    }

    public User loginByEmail(User user) {
        User dbUser = getByEmail(user.getEmail());
        // 用户存在性检查已经在 getByEmail 中处理
        if (dbUser.getStatus().equals(AccountStatus.PENDING_REVIEW.getValue())) {
            throw new ServiceException("账号正在审核");
        }
        if (dbUser.getStatus().equals(AccountStatus.REVIEW_FAILED.getValue())) {
            throw new ServiceException("账号审核不通过，请修改个人信息");
        }
        if (!bCryptPasswordEncoder.matches(user.getPassword(), dbUser.getPassword())) {
            throw new ServiceException("邮箱或密码错误");
        }
        
        return generateLoginResponse(dbUser);
    }

    private User generateLoginResponse(User dbUser) {
        String token = JwtTokenUtils.genToken(String.valueOf(dbUser.getId()), dbUser.getPassword());
        dbUser.setToken(token);
        
        // 将用户信息放入Redis，便于后续请求使用
        String userCacheKey = USER_CACHE_PREFIX + dbUser.getId();
        redisUtil.set(userCacheKey, dbUser, USER_CACHE_EXPIRE);
        logger.info("用户登录信息已缓存，key: {}", userCacheKey);
        
        // 将用户加入在线用户统计
        String onlineUserKey = "online:users";
        String userInfoKey = "online:info:" + dbUser.getId();
        
        // 存储用户登录信息（用于统计和展示）
        Map<String, Object> userLoginInfo = new HashMap<>();
        userLoginInfo.put("userId", dbUser.getId());
        userLoginInfo.put("username", dbUser.getUsername());
        userLoginInfo.put("nickname", dbUser.getNickname());
        userLoginInfo.put("roleCode", dbUser.getRoleCode());
        userLoginInfo.put("loginTime", System.currentTimeMillis());
        userLoginInfo.put("ip", getClientIp());
        
        // 使用Hash结构存储在线用户信息
        redisUtil.hset(onlineUserKey, dbUser.getId().toString(), dbUser.getUsername());
        // 使用String结构存储用户详细登录信息
        redisUtil.set(userInfoKey, userLoginInfo, USER_CACHE_EXPIRE);
        
        return dbUser;
    }

    @RedisCache(prefix = "user", key = "'role:' + #roleCode", expire = 3600)
    public List<User> getUserByRole(String roleCode) {
        List<User> users = userMapper.selectList(
            new LambdaQueryWrapper<User>()
                .eq(User::getRoleCode, roleCode)
        );
        if (users.isEmpty()) {
            throw new ServiceException("未找到该角色的用户");
        }
        return users;
    }

    public void createUser(User user) {
        // 检查用户名是否存在
        if (userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                    .eq(User::getUsername, user.getUsername())
            ) != null) {
            throw new ServiceException("用户名已存在");
        }
        
        // 检查邮箱是否被使用
        if (userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                    .eq(User::getEmail, user.getEmail())
            ) != null) {
            throw new ServiceException("邮箱已被使用");
        }

        user.setPassword(StringUtils.isNotBlank(user.getPassword()) ? user.getPassword() : DEFAULT_PWD);
        user.setPassword(bCryptPasswordEncoder.encode(user.getPassword()));
        
        if (userMapper.insert(user) <= 0) {
            throw new ServiceException("用户创建失败");
        }
        
        // 清除用户列表缓存
        redisUtil.del(USER_LIST_CACHE_KEY);
        // 清除分页缓存
        redisUtil.delByPrefix(USER_PAGE_CACHE_PREFIX);
    }

    public void updateUser(Long id, User user) {
        // 检查用户是否存在
        if (userMapper.selectById(id) == null) {
            throw new ServiceException("要更新的用户不存在");
        }
        
        // 检查新用户名是否与其他用户重复
        if (user.getUsername() != null) {
            User existUser = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                    .eq(User::getUsername, user.getUsername())
            );
            if (existUser != null && !existUser.getId().equals(id)) {
                throw new ServiceException("新用户名已被使用");
            }
        }
        
        user.setId(id);
        if (userMapper.updateById(user) <= 0) {
            throw new ServiceException("用户更新失败");
        }
        
        // 更新成功后，清除该用户的缓存
        String userCacheKey = USER_CACHE_PREFIX + id;
        redisUtil.del(userCacheKey);
        
        // 清除用户名缓存
        if (user.getUsername() != null) {
            redisUtil.del(USER_CACHE_PREFIX + "username:" + user.getUsername());
        }
        
        // 清除邮箱缓存
        if (user.getEmail() != null) {
            redisUtil.del(USER_CACHE_PREFIX + "email:" + user.getEmail());
        }
        
        // 清除用户列表缓存
        redisUtil.del(USER_LIST_CACHE_KEY);
        
        // 清除角色列表缓存
        if (user.getRoleCode() != null) {
            redisUtil.del(USER_CACHE_PREFIX + "role:" + user.getRoleCode());
        }
        
        // 清除分页缓存
        redisUtil.delByPrefix(USER_PAGE_CACHE_PREFIX);
    }

    @RedisCache(prefix = "user", key = "'username:' + #username", expire = 3600)
    public User getByUsername(String username) {
        User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
        );
        if (user == null) {
            throw new ServiceException("用户不存在");
        }
        return user;
    }

    public void deleteBatch(List<Integer> ids) {
        if (userMapper.deleteByIds(ids) <= 0) {
            throw new ServiceException("批量删除失败");
        }
        
        // 批量删除用户缓存
        for (Integer id : ids) {
            redisUtil.del(USER_CACHE_PREFIX + id);
        }
        
        // 清除用户列表缓存
        redisUtil.del(USER_LIST_CACHE_KEY);
        
        // 清除分页缓存
        redisUtil.delByPrefix(USER_PAGE_CACHE_PREFIX);
        
        // 清除角色缓存
        redisUtil.delByPrefix(USER_CACHE_PREFIX + "role:");
    }

    @RedisCache(prefix = "user", key = "'list'", expire = 3600)
    public List<User> getUserList() {
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<>());
        if (users.isEmpty()) {
            throw new ServiceException("未找到任何用户");
        }
        return users;
    }

    @RedisCache(prefix = "user", key = "#id", expire = 3600)
    public User getUserById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new ServiceException("用户不存在");
        }
        return user;
    }

    @RedisCache(prefix = "user:page", key = "#username + ':' + #sex + ':' + #nickname + ':' + #roleCode + ':' + #currentPage + ':' + #size", expire = 3600)
    public Page<User> getUsersByPage(String username, String sex, String nickname, String roleCode, Integer currentPage, Integer size) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        
        // 添加查询条件
        if (StringUtils.isNotBlank(username)) {
            queryWrapper.like(User::getUsername, username);
        }
        if (StringUtils.isNotBlank(sex)) {
            queryWrapper.eq(User::getSex, sex);
        }
        if (StringUtils.isNotBlank(nickname)) {
            queryWrapper.like(User::getNickname, nickname);
        }
        if (StringUtils.isNotBlank(roleCode)) {
            queryWrapper.eq(User::getRoleCode, roleCode);
        }
        
        return userMapper.selectPage(new Page<>(currentPage, size), queryWrapper);
    }

    public void updatePassword(Long id, UserPasswordUpdateDTO update) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new ServiceException("用户不存在");
        }
        
        // 验证旧密码
        if (!bCryptPasswordEncoder.matches(update.getOldPassword(), user.getPassword())) {
            throw new ServiceException("原密码错误");
        }
        
        // 更新新密码
        user.setPassword(bCryptPasswordEncoder.encode(update.getNewPassword()));
        if (userMapper.updateById(user) <= 0) {
            throw new ServiceException("密码修改失败");
        }
        
        // 更新密码后，清除用户缓存
        redisUtil.del(USER_CACHE_PREFIX + id);
    }

    public void forgetPassword(String email, String newPassword) {
        User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>()
                .eq(User::getEmail, email)
        );
        if (user == null) {
            throw new ServiceException("邮箱不存在");
        }
        
        user.setPassword(bCryptPasswordEncoder.encode(newPassword));
        if (userMapper.updateById(user) <= 0) {
            throw new ServiceException("密码重置失败");
        }
        
        // 清除用户缓存
        redisUtil.del(USER_CACHE_PREFIX + user.getId());
        redisUtil.del(USER_CACHE_PREFIX + "email:" + email);
        if (user.getUsername() != null) {
            redisUtil.del(USER_CACHE_PREFIX + "username:" + user.getUsername());
        }
    }

    public void deleteUserById(Long id) {
        if (userMapper.deleteById(id) <= 0) {
            throw new ServiceException("删除失败");
        }
        
        // 删除用户缓存
        redisUtil.del(USER_CACHE_PREFIX + id);
        
        // 清除用户列表缓存
        redisUtil.del(USER_LIST_CACHE_KEY);
        
        // 清除分页缓存
        redisUtil.delByPrefix(USER_PAGE_CACHE_PREFIX);
    }

    public void resetPassword(Long id, String newPassword) {
        User user = userMapper.selectById(id);
        if (user == null) throw new ServiceException("用户不存在");
        user.setPassword(bCryptPasswordEncoder.encode(newPassword));
        if (userMapper.updateById(user) <= 0) throw new ServiceException("密码重置失败");
        
        // 清除用户缓存
        redisUtil.del(USER_CACHE_PREFIX + id);
    }

    public List<User> getUsersByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        // 从数据库获取用户列表
        return userMapper.selectBatchIds(ids);
    }

    @RedisCache(prefix = "user", key = "'phone:' + #phone", expire = 3600)
    public User getByPhone(String phone) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
        if (user == null) {
            throw new ServiceException("手机号不存在");
        }
        return user;
    }

    /**
     * 用户登出
     * @param token 用户token
     */
    public void logout(String token) {
        if (StringUtils.isBlank(token)) {
            return;
        }
        
        try {
            // 获取当前用户
            User currentUser = JwtTokenUtils.getCurrentUser();
            if (currentUser != null) {
                // 清除Redis中的用户缓存
                String userCacheKey = USER_CACHE_PREFIX + currentUser.getId();
                redisUtil.del(userCacheKey);
                
                // 将用户从在线用户统计中移除
                String onlineUserKey = "online:users";
                String userInfoKey = "online:info:" + currentUser.getId();
                redisUtil.hdel(onlineUserKey, currentUser.getId().toString());
                redisUtil.del(userInfoKey);
                
                // 清除JWT相关缓存
                JwtTokenUtils.clearUserCache(token);
                
                logger.info("用户登出成功，用户ID: {}", currentUser.getId());
            }
        } catch (Exception e) {
            logger.error("用户登出异常", e);
        }
    }
    
    /**
     * 获取在线用户数量
     * @return 在线用户数量
     */
    public long getOnlineUserCount() {
        String onlineUserKey = "online:users";
        return redisUtil.hsize(onlineUserKey);
    }
    
    /**
     * 获取在线用户列表
     * @return 在线用户列表
     */
    public List<Map<String, Object>> getOnlineUserList() {
        String onlineUserKey = "online:users";
        Map<Object, Object> onlineUsers = redisUtil.hmget(onlineUserKey);
        
        List<Map<String, Object>> resultList = new ArrayList<>();
        if (onlineUsers != null && !onlineUsers.isEmpty()) {
            for (Map.Entry<Object, Object> entry : onlineUsers.entrySet()) {
                String userId = entry.getKey().toString();
                String userInfoKey = "online:info:" + userId;
                Object userInfo = redisUtil.get(userInfoKey);
                if (userInfo != null) {
                    if (userInfo instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> userInfoMap = (Map<String, Object>) userInfo;
                        resultList.add(userInfoMap);
                    }
                }
            }
        }
        
        return resultList;
    }
    
    /**
     * 获取客户端IP地址
     * @return IP地址
     */
    private String getClientIp() {
        try {
            ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (requestAttributes != null) {
                String ip = requestAttributes.getRequest().getHeader("X-Forwarded-For");
                if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
                    ip = requestAttributes.getRequest().getHeader("Proxy-Client-IP");
                }
                if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
                    ip = requestAttributes.getRequest().getHeader("WL-Proxy-Client-IP");
                }
                if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
                    ip = requestAttributes.getRequest().getHeader("HTTP_CLIENT_IP");
                }
                if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
                    ip = requestAttributes.getRequest().getHeader("HTTP_X_FORWARDED_FOR");
                }
                if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
                    ip = requestAttributes.getRequest().getRemoteAddr();
                }
                // 多次反向代理后会有多个IP值，第一个为真实IP
                if (ip != null && ip.contains(",")) {
                    ip = ip.split(",")[0];
                }
                return ip;
            }
            return "unknown";
        } catch (Exception e) {
            logger.error("获取客户端IP异常", e);
            return "unknown";
        }
    }
}
