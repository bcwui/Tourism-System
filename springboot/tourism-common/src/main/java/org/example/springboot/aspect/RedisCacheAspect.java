package org.example.springboot.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.example.springboot.annotation.RedisCache;
import org.example.springboot.util.RedisUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * Redis缓存切面
 * 实现方法结果的自动缓存
 */
@Aspect
@Component
public class RedisCacheAspect {

    private static final Logger logger = LoggerFactory.getLogger(RedisCacheAspect.class);

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private ObjectMapper objectMapper;
    
    // SpEL表达式解析器
    private final SpelExpressionParser parser = new SpelExpressionParser();
    
    // 参数名发现器
    private final DefaultParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();

    /**
     * 定义切点：所有使用了RedisCache注解的方法
     */
    @Pointcut("@annotation(org.example.springboot.annotation.RedisCache)")
    public void redisCachePointcut() {
    }

    /**
     * 环绕通知：在方法执行前后实现缓存处理
     */
    @Around("redisCachePointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取目标方法
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        
        // 获取RedisCache注解
        RedisCache redisCache = method.getAnnotation(RedisCache.class);
        
        // 获取缓存键
        String prefix = redisCache.prefix();
        String key = redisCache.key();
        long expire = redisCache.expire();
        boolean refresh = redisCache.refresh();
        
        // 如果没有指定前缀，则使用类名+方法名作为前缀
        if (!StringUtils.hasText(prefix)) {
            prefix = method.getDeclaringClass().getSimpleName() + ":" + method.getName();
        }
        
        // 解析SpEL表达式获取实际缓存键
        String cacheKey = prefix;
        if (StringUtils.hasText(key)) {
            cacheKey = prefix + ":" + parseKey(key, method, joinPoint.getArgs());
        } else {
            // 如果未指定key，使用参数的hashCode
            cacheKey = prefix + ":" + argsToString(joinPoint.getArgs());
        }
        
        logger.info("Redis缓存键: {}", cacheKey);
        
        // 如果需要刷新缓存，则删除缓存
        if (refresh) {
            redisUtil.del(cacheKey);
            logger.info("刷新缓存: {}", cacheKey);
        }
        
        // 尝试从缓存获取数据
        Object result = null;
        if (!refresh && redisUtil.hasKey(cacheKey)) {
            Object cacheValue = redisUtil.get(cacheKey);
            logger.info("从缓存获取数据: {}", cacheKey);
            
            // 获取方法返回类型
            Class<?> returnType = method.getReturnType();
            
            // 正确处理缓存中的数据转换
            if (cacheValue != null) {
                try {
                    // 如果缓存值是Map类型（通常是LinkedHashMap），则需要转换为目标类型
                    if (cacheValue instanceof java.util.Map) {
                        // 使用ObjectMapper将Map转换为JSON字符串，再转换为目标类型
                        String jsonValue = objectMapper.writeValueAsString(cacheValue);
                        result = objectMapper.readValue(jsonValue, returnType);
                    } else if (cacheValue instanceof String && returnType != String.class) {
                        // 如果缓存值是字符串，但返回类型不是字符串，则尝试将其转换为目标类型
                        result = objectMapper.readValue((String) cacheValue, returnType);
                    } else {
                        // 如果缓存值类型与返回类型匹配，则直接使用
                        result = cacheValue;
                    }
                } catch (Exception e) {
                    logger.error("缓存数据转换失败，将执行原方法: {}", e.getMessage());
                    // 转换失败则执行原方法
                    result = null;
                }
            }
        }
        
        // 如果缓存中没有数据或转换失败，执行原方法并缓存结果
        if (result == null) {
            logger.info("缓存未命中或转换失败，执行方法: {}", method.getName());
            result = joinPoint.proceed();
            
            // 缓存结果（非null）
            if (result != null) {
                redisUtil.set(cacheKey, result, expire);
                logger.info("将结果缓存: {}, 过期时间: {}秒", cacheKey, expire);
            }
        }
        
        return result;
    }
    
    /**
     * 解析SpEL表达式
     */
    private String parseKey(String key, Method method, Object[] args) {
        // 获取方法参数名
        String[] parameterNames = nameDiscoverer.getParameterNames(method);
        if (parameterNames == null || parameterNames.length == 0) {
            return key;
        }
        
        // 创建表达式上下文
        EvaluationContext context = new StandardEvaluationContext();
        
        // 将方法参数加入上下文
        for (int i = 0; i < parameterNames.length; i++) {
            context.setVariable(parameterNames[i], args[i]);
        }
        
        // 解析表达式
        Expression expression = parser.parseExpression(key);
        Object value = expression.getValue(context);
        
        return value == null ? "" : value.toString();
    }
    
    /**
     * 将参数转换为字符串，用于构建缓存键
     */
    private String argsToString(Object[] args) {
        if (args == null || args.length == 0) {
            return "no_args";
        }
        
        StringBuilder sb = new StringBuilder();
        for (Object arg : args) {
            if (arg != null) {
                sb.append(arg.toString()).append("_");
            }
        }
        
        return sb.toString().hashCode() + "";
    }
} 