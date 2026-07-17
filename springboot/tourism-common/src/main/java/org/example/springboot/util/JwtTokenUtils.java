package org.example.springboot.util;


import cn.hutool.core.date.DateUtil;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.example.springboot.entity.User;
import org.example.springboot.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Date;
import java.util.Map;

@Component
public class JwtTokenUtils {
    private static UserMapper staticUserMapper;
    private static RedisUtil staticRedisUtil;
    private static ObjectMapper staticObjectMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private ObjectMapper objectMapper;

    public static final Logger LOGGER = LoggerFactory.getLogger(JwtTokenUtils.class);

    private static final String USER_TOKEN_KEY_PREFIX = "user:token:";
    private static final String USER_ID_KEY_PREFIX = "user:id:";
    private static final long TOKEN_EXPIRE = 7200; // 2小时

    @PostConstruct
    public void setServices() {
        staticUserMapper = userMapper;
        staticRedisUtil = redisUtil;
        staticObjectMapper = objectMapper;
    }

    public static String genToken(String userId, String sign) {
        String token = JWT.create()
                .withAudience(userId)
                .withExpiresAt(DateUtil.offsetHour(new Date(), 2))
                .sign(Algorithm.HMAC256(sign));

        staticRedisUtil.set(USER_TOKEN_KEY_PREFIX + token, userId, TOKEN_EXPIRE);

        return token;
    }

    public static User getCurrentUser() {
        String token = null;
        try {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
            token = request.getHeader("token");
            if (StringUtils.isBlank(token)) {
                token = request.getParameter("token");
            }
            if (StringUtils.isBlank(token)) {
                LOGGER.error("获取当前登录的token失败，token{}", token);
                return null;
            }

            String userId = (String) staticRedisUtil.get(USER_TOKEN_KEY_PREFIX + token);

            if (StringUtils.isBlank(userId)) {
                userId = JWT.decode(token).getAudience().get(0);
                if (StringUtils.isBlank(userId)) {
                    LOGGER.error("从token中解析用户ID失败，token: {}", token);
                    return null;
                }
                staticRedisUtil.set(USER_TOKEN_KEY_PREFIX + token, userId, TOKEN_EXPIRE);
            }

            String userKey = USER_ID_KEY_PREFIX + userId;
            Object userObj = staticRedisUtil.get(userKey);
            User user = null;

            if (userObj != null) {
                try {
                    if (userObj instanceof User) {
                        user = (User) userObj;
                    } else if (userObj instanceof Map) {
                        String jsonStr = staticObjectMapper.writeValueAsString(userObj);
                        user = staticObjectMapper.readValue(jsonStr, User.class);
                    } else {
                        String jsonStr = staticObjectMapper.writeValueAsString(userObj);
                        user = staticObjectMapper.readValue(jsonStr, User.class);
                    }
                } catch (Exception e) {
                    LOGGER.error("Redis缓存中的用户数据转换失败，将从数据库重新获取", e);
                    user = null;
                }
            }

            if (user == null) {
                user = staticUserMapper.selectById(Long.valueOf(userId));
                if (user != null) {
                    staticRedisUtil.set(userKey, user, TOKEN_EXPIRE);
                }
            }

            return user;
        } catch (Exception e) {
            LOGGER.error("获取当前用户信息失败，token: {}", token, e);
            return null;
        }
    }

    /**
     * 更新Redis中的用户信息
     */
    public static void updateUserCache(User user) {
        if (user != null && user.getId() != null) {
            String userKey = USER_ID_KEY_PREFIX + user.getId();
            staticRedisUtil.set(userKey, user, TOKEN_EXPIRE);
        }
    }

    /**
     * 清除用户的缓存信息
     */
    public static void clearUserCache(String token) {
        if (StringUtils.isNotBlank(token)) {
            String userId = (String) staticRedisUtil.get(USER_TOKEN_KEY_PREFIX + token);
            if (StringUtils.isNotBlank(userId)) {
                staticRedisUtil.del(USER_ID_KEY_PREFIX + userId);
            }
            staticRedisUtil.del(USER_TOKEN_KEY_PREFIX + token);
        }
    }
}
