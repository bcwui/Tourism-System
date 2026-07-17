# 旅游平台项目 — 面试问答指南（代码级）

> 以下问答均基于你的实际代码。**回答时先说结论，再展开细节，能带行号就带行号。**

---

# 一、JWT + 自研拦截器认证授权

## Q1: 你的认证授权整体设计是什么样的？

**回答：**

三层协作：
1. **JWT 签发**（`JwtTokenUtils.java:52-61`）：用户登录成功后，用 `HMAC256(user.getPassword())` 签名，Token 同时存入 Redis → `user:token:{token}`，有效期 2h
2. **拦截器校验**（`JwtInterceptor.java:33-69`）：拦截所有 `/api/**`，从 Header 拿 token → JWT.decode 取 userId → 查 DB 确认用户存在 → 用该用户 password 做 HMAC256 验签
3. **Spring Security 只做 BCrypt**（`SecurityConfig.java:25-33`）：`anyRequest().permitAll()` + `csrf().disable()`，认证全交给 JwtInterceptor

**关键设计：**
- **一人一密**：用 password 做签名密钥 → 用户改密码后**所有旧 Token 自动失效**，天然实现"修改密码踢下线"
- **Redis 存 Token**：可主动 `clearUserCache` 强制下线（`JwtTokenUtils.java:135-143`）
- **用户缓存**：`user:id:{userId}` → User JSON，减少查库（`JwtTokenUtils.java:87-113`）

## Q2: 拦截器怎么注册的？哪些路径放行？

**回答：**（`WebConfig.java:45-65`）

```java
registry.addInterceptor(jwtInterceptor)
    .addPathPatterns("/api/**")
    .excludePathPatterns(
        "/api/user/login", "/api/user/forget", "/api/user/add",
        "/api/email/**", "/api/img/**", "/api/file/**",
        "/api/alipay/return", "/api/alipay/notify",
        "/api/doc.html", ...  // Knife4j文档
    );
```
放行：登录、注册、忘记密码、邮件、文件、支付宝回调、Knife4j 文档。

## Q3: 为什么不用 Spring Security 的认证链？

> Security 太重了（Filter Chain、SecurityContextHolder、投票器），我们认证需求简单（登录校验 + 角色隔离），一个 HandlerInterceptor 就够。
> 但 Security 的 BCrypt 编码器（`SecurityConfig.java:19-21`）还是用了，`BCryptPasswordEncoder(10)` 表示 2¹⁰ 轮哈希。

## Q4: Token 过期了怎么办？做没做刷新？

> Token 有效期 2h（`JwtTokenUtils.java:43`、`JwtTokenUtils.java:55`），Redis 存的 Token 也是 2h 过期（`JwtTokenUtils.java:58`）。
> 前端拦截器检测到 401 → 清除本地 token → 跳转登录页。
> 没有做 RefreshToken 双令牌，但如果要做：短 AccessToken(30min) + 长 RefreshToken(7d 存 Redis)，AccessToken 过期用 RefreshToken 换新的。

## Q5: 你的拦截器没有按角色区分，管理员和普通用户怎么隔离？

> 拦截器只做了**登录校验**（是否合法用户），角色隔离在**业务层**做。比如退款操作（`TicketOrderService.java:192`）：
> ```java
> if (!"ADMIN".equals(currentUser.getRoleCode())) {
>     throw new ServiceException("无权执行退款操作");
> }
> ```
> 同样取消订单（`TicketOrderService.java:142`）：管理员或本人才能操作。这种方式简单直接，适合当前业务复杂度。

---

# 二、Redis 分布式锁（普通购票）

## Q1: 详细说下你的 Redis 分布式锁实现？

**回答：**（核心代码 `RedisLockUtil.java:36-75`）

```
加锁：SET lock:ticket:stock:{ticketId} {UUID} NX EX 10
         ↓
业务代码（TicketOrderService.java:72-93）：
  try {
      查库存 → UPDATE ticket SET stock = stock - ? WHERE stock >= ? → 写入订单
  } finally {
      释放锁（Lua脚本）
  }
```

**调用代码**（`TicketOrderService.java:72-93`）：
```java
String lockValue = redisLockUtil.tryLock(lockKey, 10);  // 10s超时
if (lockValue == null) throw new ServiceException("系统繁忙");
try {
    if (!ticketService.deductStock(..., quantity))   // DB原子扣
        throw new ServiceException("库存不足");
    ticketOrderMapper.insert(order);                   // 写订单
} finally {
    redisLockUtil.releaseLock(lockKey, lockValue);    // 一定释放
}
```

**三个保障：**
1. **UUID 做 value**：每个线程唯一标识，释放时校验（防止 A 的锁过期后被 B 误删）
2. **Lua 原子释放**（`RedisLockUtil.java:23-28`）：`if get(key)==ARGV[1] then del(key)` — get+del 一步完成
3. **finally 释放**：业务抛异常也能释放，不会死锁

## Q2: 锁过期时间设了多少？为什么是 10 秒？

> 10 秒（`TicketOrderService.java:73`）。
> 原因：扣库存是 `UPDATE ... WHERE` 原子操作，1-2s 内完成；10s 是保守兜底。如果业务超过 10s（网络抖动、MySQL 慢查询），锁会自动过期，防止死锁。

## Q3: 锁过期了但业务没执行完怎么办？（看门狗问题）

> **这是 Redis 分布式锁的经典痛点。**
> 当前项目没做续期，依赖 10s 超时兜底。
> 如果生产环境要做：引入**看门狗机制**（Redisson 的做法）—— 后台线程每 3s 续期一次，业务完成主动释放。
> 项目中手写 `RedisLockUtil` 是为了轻量，没有引入 Redisson，但了解续期方案。

## Q4: 为什么不用数据库乐观锁替代 Redis 锁？

> 数据库乐观锁（version 字段）在高并发下会产生大量 `UPDATE ... WHERE version=?` 冲突重试，直接打爆 MySQL。
> Redis 锁在**缓存层拦截**：同一时刻同一门票只有一个请求拿到锁，走到 DB 时是串行压力不是并发压力。

## Q5: 如果 Redis 是主从/集群架构，这个锁有什么问题？

> **红锁（RedLock）问题：** Redis 主从异步复制，主机宕机后从机提升，但原主机的锁可能没同步到从机 → 两个客户端同时拿到锁。
> 回答：
> - 当前项目：单机 Redis，问题不存在
> - 生产环境：换 Redisson 的 RedLock（N/2+1 节点投票），或换 ZK/etcd（CP 模型，强一致性）

## Q6: 你 DB 层也做了 `WHERE stock >= #{quantity}`，和 Redis 锁什么关系？

> **双重保障**（`TicketMapper.java:12`）：
> ```sql
> UPDATE ticket SET stock = stock - #{quantity} WHERE id = #{ticketId} AND stock >= #{quantity}
> ```
> Redis 锁是**前端拦截**（保证同一时刻只有一个请求操作），DB 的 WHERE 条件是**最终防线**（锁万一失效，DB 层面也能拦截超卖）。`affected > 0` 判断扣减是否成功。

---

# 三、秒杀系统

## Q1: 秒杀从点击到下单，完整流程是什么？

**回答：**（`SeckillService.java:128-194`）

```
用户点击抢购
  → ① 校验活动（时间/status=1）
  → ② 校验门票（存在/可用）
  → ③ 限购检查（Redis Set：seckill:users:{activityId}，SISMEMBER 判重）
  → ④ 限量检查（limitPerUser，默认1张）
  → ⑤ 库存预热兜底（SETNX 自动从DB加载，防并发覆写）
  → ⑥ Redis DECR 扣库存（seckill:stock:{activityId}）
      ↓ remaining < 0 → INCR 回滚 → 抛"库存不足"
  → ⑦ Redis SADD 标记用户已购（seckill:users:{activityId}）
  → ⑧ 发送 MQ 消息（seckill.exchange → seckill.order.queue）
      ↓ 发送失败 → increment 回滚库存 + SREM 移除用户标记
  → ⑨ 返回"抢购成功"，前端轮询订单状态
      ↓
  消费者（SeckillOrderConsumer.java:48-102）：
  → ⑩ DB 扣门票库存（deductStock）
  → ⑪ DB 扣秒杀活动库存（SeckillActivityMapper.deductStock）
  → ⑫ 创建订单（status=0 待支付）
      ↓ 任何一步失败 → 事务回滚 + Redis回滚（rollbackSeckill）
```

## Q2: 为什么秒杀用 DECR 而不用 SETNX 分布式锁？我简历是不是写错了？

> **没写错，只是两种场景用了不同策略：**
>
> | 场景 | 工具 | 原因 |
> |------|------|------|
> | **普通购票**（TicketOrderService） | SETNX 分布式锁 | 锁保护的是"查库存 + 扣库存 + 写订单"**三个 DB 操作的事务组合** |
> | **秒杀**（SeckillService） | Redis DECR + DB WHERE | DECR 是 Redis **单线程原子操作**，一步完成"查+扣"，不需要额外加锁 |
>
> **面试话术**：秒杀用的是 Redis 原子 DECR 做库存预扣，这是比锁更高效的方案。锁是用在普通购票流程的，那里需要包裹多个 DB 操作。

## Q3: 如果 Redis DECR 扣了库存，但 MQ 消息发不出去怎么办？

**回答**（`SeckillService.java:180-191`）：
```java
try {
    rabbitTemplate.convertAndSend(SECKILL_EXCHANGE, SECKILL_ORDER_ROUTING_KEY, request);
} catch (Exception e) {
    // MQ发送失败 → 立即回滚Redis
    stringRedisTemplate.opsForValue().increment(stockKey, buyQty);
    stringRedisTemplate.opsForSet().remove(usersKey, user.getId().toString());
    throw new ServiceException("系统繁忙，请稍后重试");
}
```
**M Q 发送失败 → 立即回滚 Redis 库存 + 移除用户购买标记，用户看到"系统繁忙"可重试。**

## Q4: MQ 消费端如果失败怎么处理？会重试吗？

**回答**（`SeckillOrderConsumer.java:94-101`）：
```java
catch (Exception e) {
    TransactionAspectSupport.currentTransactionStatus().setRollbackOnly(); // DB回滚
    seckillService.rollbackSeckill(activityId, userId, quantity);          // Redis回滚
    // 不抛异常 → 消息ACK → 不重试
}
```
**不重试的原因**：消费失败通常是库存不足（DB `WHERE stock >= quantity` 不满足），重试无意义。且如果重试，`rollbackSeckill` 会被重复调用 → Redis 库存膨胀。

## Q5: 怎么防止同一用户重复抢购？

**回答**（`SeckillService.java:149-153`）：
```java
String usersKey = SECKILL_USERS_PREFIX + activityId;
Boolean alreadyBought = stringRedisTemplate.opsForSet().isMember(usersKey, user.getId().toString());
if (Boolean.TRUE.equals(alreadyBought)) {
    throw new ServiceException("您已参与过本次秒杀");
}
```
**用 Redis Set 记录已购用户 → SISMEMBER 判重 → 秒杀成功后 SADD 标记。活动结束自动过期。**

## Q6: 库存预热是怎么做的？如果没预热呢？

**预热**（`SeckillService.java:113-123`）：管理端触发，将 DB 库存同步到 Redis，TTL 设到活动结束时间。

**兜底**（`SeckillService.java:162-168`）：
```java
Boolean exists = stringRedisTemplate.hasKey(stockKey);
if (Boolean.FALSE.equals(exists)) {
    stringRedisTemplate.opsForValue().setIfAbsent(stockKey,
        String.valueOf(activity.getSeckillStock()), ttl, TimeUnit.SECONDS);
}
```
**用 SETNX 自动初始化，防并发覆写。如果 Redis 里没库存，自动从 DB 加载。**

## Q7: 你的秒杀能支持多大并发？怎么做流量过滤？

> **漏斗模型：推荐用数字表达**
> - 前端：按钮防重 + 验证码（项目里 limitPerUser + idCard 也能防黄牛）
> - Gateway：可扩展令牌桶限流（当前项目未配）
> - 业务层：库存预热到 Redis + Redis Set 防重复购买
> - Redis：原子 DECR，< 0 直接拒绝，不碰 DB
> - MQ：异步削峰，消费者按 DB 承载能力（如 1000/s）消费
>
> 理论瓶颈在 Redis（单线程但 10w+ QPS），实际瓶颈在 DB 连接池和 RabbitMQ 吞吐。

---

# 四、RabbitMQ 异步削峰

## Q1: 你用了什么 Exchange？为什么？

**回答**（`RabbitMQConfig.java:32-45`）：**DirectExchange**，三个：
- `order.exchange` — 订单状态变更
- `email.exchange` — 邮件异步发送
- `seckill.exchange` — 秒杀下单

Direct 路由键精确匹配，不需要广播（Fanout）或模糊匹配（Topic），最简单高效。

## Q2: 消息不丢的保障有哪些？

| 环节 | 你的做法 | 代码位置 |
|------|---------|---------|
| **生产者发送** | try-catch，失败回滚 Redis | `SeckillService.java:180-191` |
| **Broker 持久化** | `QueueBuilder.durable()` | `RabbitMQConfig.java:49-61` |
| **消费者处理** | 异常回滚 Redis，事务回滚 DB | `SeckillOrderConsumer.java:94-101` |
| **消息序列化** | `Jackson2JsonMessageConverter` + `JavaTimeModule` | `RabbitMQConfig.java:82-84` |

## Q3: 消费者 `@ConditionalOnProperty` 为什么这么用？

**回答**（`SeckillOrderConsumer.java:30`）：
```java
@ConditionalOnProperty(name = "mq.consumer.seckill", havingValue = "true")
```
不同微服务选择性开启消费者。比如 `tourism-order` 处理秒杀消费，`tourism-scenic` 不处理。通过配置文件控制开关，灵活部署。

## Q4: 为什么简历写 RocketMQ，实际用 RabbitMQ？

> **面试前一定要确认清楚！** 代码注释里有一处写了"RocketMQ"（`TicketOrderService.java:95`），但实际用的是**RabbitMQ**（Spring AMQP）。
> 建议话术：实际使用 RabbitMQ，生态成熟、Spring AMQP 集成好，适合业务解耦场景。RocketMQ 更适合阿里云生态和事务消息场景。

## Q5: 订单状态变更通知是怎么流转的？

**回答**（`TicketOrderService.java`）：
四种事件都发 `order.exchange` → `order.queue`：
- 创建订单 → ORDER_CREATED（通知用户/准备付款提醒）
- 支付成功 → ORDER_PAID
- 手动取消/超时取消 → ORDER_CANCELLED
- 退款 → ORDER_REFUNDED
- 完成 → ORDER_COMPLETED

消费者 `OrderMessageConsumer` 根据 `eventType` 做不同处理（发邮件等）。

---

# 五、微服务治理

## Q1: 6 个服务怎么划分的？公共模块放了什么？

| 服务 | 职责 | 端口 |
|------|------|------|
| tourism-user | 登录/注册/个人信息 | 8081 |
| tourism-scenic | 景点/分类/门票管理 | 8082 |
| tourism-order | 订单/支付/秒杀 | 8083 |
| tourism-social | 评论/攻略/收藏/轮播/搜索 | 8084 |
| tourism-accommodation | 住宿预订/评价 | 8085 |
| tourism-file | 文件上传/邮件/短信/监控 | 8086 |

**tourism-common** 公共模块：Entity、Mapper、Service、DTO、Util（RedisLockUtil、JwtTokenUtils、RedisUtil）、全局配置（JwtInterceptor、SecurityConfig、RabbitMQConfig、RedisConfig、WebConfig）、AOP 切面（RedisCacheAspect、RateLimitAspect）、GlobalExceptionHandler。通过 Maven 依赖引入，`scanBasePackages = "org.example.springboot"` 扫描。

## Q2: 请求全链路怎么走？

```
客户端 → localhost:8080 (Gateway)
  → Nacos 发现服务列表
  → lb://tourism-xxx 负载均衡
  → 微服务实例 → WebConfig 自动加 /api 前缀
  → JwtInterceptor 验签 → Controller → Service → Mapper
```

关键细节（`WebConfig.java:37-43`）：`configurePathMatch` 给所有 `@RestController` 自动加 `/api` 前缀，所以 Controller 写 `/user/login` 实际映射 `/api/user/login`。

## Q3: Gateway 路由规则和 StripPrefix 为什么是 0？

**回答**（`tourism-gateway/application.yml`）：
- 6 组路由，按 Path 前缀分发
- `StripPrefix=0`：因为 `WebConfig.addPathPrefix("/api")` 已经给 Controller 加了 `/api` 前缀，Gateway 不需要剥离
- 如果 `StripPrefix=1` 会剥离 `/api` → 服务收到 `/user/login` → 匹配不到带 `/api` 前缀的 Controller → 404

## Q4: 服务间怎么通信？

> 当前：所有 Mapper 和 Service 都在 `tourism-common` 中，服务间**走到同一个 JVM 的本地方法调用**，不走 RPC。
> 异步通信：RabbitMQ（订单创建后通知其他服务）。
> 如果后续拆分数据库：需要引入 OpenFeign + Nacos 服务发现做远程调用。

## Q5: Spring Security 的角色是什么？

> 只提供 BCrypt 密码编码器（`SecurityConfig.java:19-21`），认证链完全放行（`anyRequest().permitAll()`），关闭 CSRF。
> 认证全由自定义 `JwtInterceptor` + `WebConfig` 拦截器注册完成。
> 这么做的好处：更轻量、更可控、出问题好排查。

---

# 六、@RedisCache 注解 + AOP

## Q1: @RedisCache 怎么实现的？

**回答**（`RedisCacheAspect.java:57-139`）：

```java
@Around("@annotation(org.example.springboot.annotation.RedisCache)")
public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
    // 1. 获取注解参数（prefix、key、expire、refresh）
    // 2. 解析 SpEL 生成缓存键（如 user:info:#userId）
    // 3. refresh=true → 先删缓存
    // 4. 查 Redis → 命中返回（处理类型转换：LinkedHashMap→目标类型）
    // 5. 未命中 → 执行目标方法 → 结果写 Redis（设过期时间）
}
```
**关键细节：**
- **SpEL 表达式**：`DefaultParameterNameDiscoverer` 获取参数名，`StandardEvaluationContext` 绑定参数值
- **类型转换**：Jackson 处理 `LinkedHashMap → 目标类型`（`RedisCacheAspect.java:107-110`）
- **缓存刷新**：`refresh=true` 先删缓存（`RedisCacheAspect.java:89-92`），用于更新操作

## Q2: 缓存穿透/击穿/雪崩怎么处理的？

| 问题 | 当前处理 | 如果面试官追问 |
|------|---------|--------------|
| **穿透** | 结果 null 时不缓存（`RedisCacheAspect.java:132`） | 生产加布隆过滤器 / 缓存空值设短TTL |
| **击穿** | 未实现互斥锁 | 可结合 `RedisLockUtil` 加锁，或设"逻辑过期" |
| **雪崩** | 支持自定义 expire | 过期时间加随机值防同时过期 |

> 话术：项目初期先实现了基础缓存功能。生产环境会在过期时间加随机值防雪崩，布隆过滤器防穿透，热点数据加互斥锁防击穿。

---

# 七、@RateLimit 接口限流

## Q1: 限流是怎么实现的？

**回答**（`RateLimitAspect.java:38-53`）：
```java
Long currentCount = stringRedisTemplate.opsForValue().increment(key);
if (currentCount == 1) {
    stringRedisTemplate.expire(key, rateLimit.window(), TimeUnit.SECONDS);
}
if (currentCount > rateLimit.maxRequests()) {
    throw new ServiceException(rateLimit.message());
}
```
**固定窗口计数器**：Redis INCR + 首次设置 TTL。默认按 **IP + 方法名** 维度限流，支持 SpEL 自定义 key。

## Q2: 固定窗口有什么问题？

> **边界突刺问题**：假设 10s 窗口限制 100 次，第 9s 来 100 次 + 第 11s 来 100 次，实际 2s 内处理了 200 次请求。
> **改进**：改用**滑动窗口**（ZSet 记录每个请求时间戳，窗口外自动清理），或直接用 Sentinel 的滑动窗口实现。

---

# 八、数据库设计与防超卖

## Q1: 数据库层面怎么防超卖？

**回答**（`TicketMapper.java:12` + `SeckillActivityMapper.java:12`）：
```sql
UPDATE ticket SET stock = stock - #{quantity} 
WHERE id = #{ticketId} AND stock >= #{quantity}

UPDATE seckill_activity SET seckill_stock = seckill_stock - #{quantity} 
WHERE id = #{activityId} AND seckill_stock >= #{quantity}
```
`WHERE stock >= #{quantity}` 是**数据库层面的乐观锁**，affected rows = 0 表示库存不足，代码判断 `affected > 0` 确认扣减成功。

## Q2: 为什么不用 `SELECT ... FOR UPDATE`？

> `FOR UPDATE` 是悲观锁，锁定行直到事务提交，高并发下大量等待和超时。当前 `UPDATE ... WHERE stock >= ?` 是无锁的原子更新，性能更好。

---

# 九、订单超时自动取消

## Q1: 订单超时取消怎么实现？

**回答**（`OrderAutoCancelScheduler.java`）：
```java
@Scheduled(fixedRate = 30000)  // 每30秒扫描
public void autoCancelExpiredOrders() {
    // 查询 status=0 且 create_time < now-15min 的订单
    // 逐个调用 ticketOrderService.cancelOrderSystem(order)
}
```
**取消逻辑**（`TicketOrderService.java:165-184`）：
1. 幂等检查（status != 0 则跳过）
2. 更新 status 0 → 2（待支付 → 已取消）
3. 原子恢复库存（`restoreStock`）
4. 发送 MQ 取消消息

**设计考量**：
- Scheduler 独立组件（不在 Service 内部），避免自注入循环依赖
- `@EnableScheduling` 只在 `tourism-order` 启用（`OrderApplication.java`），避免其他服务空轮询
- 每 30s 扫描 + 15min 超时，既及时又不频繁

---

# 十、综合/BOSS 面高频题

## Q1: 这个项目你遇到的最大难点是什么？

**建议回答**：秒杀的数据一致性问题。

> Redis DECR 扣库存 → MQ → DB 落库这条链路中，任何一环失败都要保证最终一致：
> 1. MQ 发送失败 → catch 块 increment 回滚（`SeckillService.java:186-191`）
> 2. DB 扣减失败 → 事务回滚 + `rollbackSeckill` 恢复 Redis（`SeckillOrderConsumer.java:94-101`）
> 3. 消费异常不重试 → 避免 rollback 重复调用导致 Redis 库存膨胀
> 4. 极端场景需要**定时对账**：比对 Redis 扣减记录 vs DB 订单记录，差异库存补回
>
> 最终一致性比强一致性更实际，99%的情况被上述机制覆盖了。

## Q2: 如果重新做，你会怎么改进？

| 模块 | 改进方案 |
|------|---------|
| **分布式锁** | 引入 Redisson（看门狗续期 + 红锁），替代手写 `RedisLockUtil` |
| **限流** | 引入 Sentinel 做熔断降级 + 滑动窗口限流，替代手写 `@RateLimit` |
| **缓存同步** | Canal 监听 MySQL binlog 实时更新 Redis，替代手动删缓存 |
| **幂等** | MQ 消费者加 msgId 去重表，防止重复消费 |
| **监控** | Prometheus + Grafana 监控 QPS、库存、MQ 积压 |
| **网关** | Gateway 多实例 + Nginx 负载均衡，解决单点问题 |

## Q3: 你选 RabbitMQ 而不选 RocketMQ/Kafka 的原因？

> - **RabbitMQ**：延迟低，Spring AMQP 集成成熟，Direct Exchange 模型简单，适合**业务解耦**（订单通知、邮件发送）
> - **RocketMQ**：更适合阿里云生态，支持事务消息（分布式事务），延迟消息更灵活
> - **Kafka**：高吞吐，适合日志/埋点/大数据管道
>
> 我们的场景是高并发下单但低吞吐消费（削峰），RabbitMQ 足够且集成简单。

## Q4: 黄牛刷单怎么防？

- **Redis Set 限购**：`seckill:users:{activityId}` 记录已购用户，`SISMEMBER` 判重
- **limitPerUser**：每人限购 1 张（可配）
- **idCard 身份证**：下单要求填身份证号（实体字段 `idCard`），可做同证件限购
- **@RateLimit**：IP + 用户维度限流防暴力请求

## Q5: 你的 Gateway 是单点吗？挂了怎么办？

> 当前是演示项目，单点部署（8080 端口）。生产环境：
> - Gateway 多实例部署，Nginx 反向代理 + keepalived
> - Nacos 集群（3 节点）保证注册中心高可用
> - K8s 做自动扩缩容和健康检查

---

# 十一、支付宝沙箱支付

## Q1: 支付宝支付流程怎么走的？

1. 前端创建订单（status=0 待支付）
2. 前端请求 `/api/alipay/pay?orderNo=xxx` → 后端生成签名 → 返回支付页面 HTML → 前端渲染
3. 用户扫码支付
4. 支付宝异步通知 `/api/alipay/notify` → 后端验签 → 更新订单 status 0→1 → 发 MQ 消息
5. 同步跳转 `/api/alipay/return` → 验签 → 跳转结果页

**关键细节**：
- 回调 URL 必须走网关（`localhost:8080/api/alipay/...`），因为 `WebConfig` 自动加 `/api` 前缀
- 沙箱不稳定，提供了 `/api/alipay/mock-pay/{orderId}` 模拟支付（本地测试用）
- 同步回调容错：验签失败时二次查 DB 订单状态，防止误判
- 内网穿透用 natapp

---

# 十二、踩坑经验（展示排错能力）

| 坑 | 现象 | 原因 | 解决 |
|----|------|------|------|
| **Nacos Config 报错** | 启动报 `No spring.config.import` | Spring Cloud 2023 强制 Config 导入 | `import-check.enabled: false` |
| **@ConditionalOnProperty 不生效** | Bean 始终不创建 | kebab-case `app-id` 读不到 camelCase `appId` | 改为 `name = "appId"` |
| **公共 Bean 报错** | user 服务报 AlipayConfig 找不到 | 所有服务扫描同一包，无条件创建 | `@ConditionalOnProperty` / `@ConditionalOnBean` |
| **网关 StripPrefix=1 全站 404** | 修改后系统不可用 | 服务端 `WebConfig` 已加 `/api` 前缀，网关剥离后路径不匹配 | 锁定 `StripPrefix=0` |
| **自注入循环依赖** | Service 中 `@Resource private XxxService self` 启动报循环依赖 | Spring Boot 3.x 默认禁止循环引用 | 提取独立 Scheduler 组件，外部调用 |
| **支付宝沙箱 504** | 支付页频繁超时 | 支付宝沙箱环境不稳定 | mock-pay 模拟支付 + 同步回调容错 |

---

# 十三、快速自查清单（面试前过一遍）

- [ ] JWT 签名的密钥是什么？为什么？（**password HMAC256**，一人一密，改密码自动失效）
- [ ] 分布式锁怎么实现的？（**SETNX + UUID + Lua 原子释放 + finally 兜底**）
- [ ] 锁超时多少？为什么？（**10s**，业务 1-2s，保守兜底）
- [ ] 秒杀库存扣用 DECR 还是锁？（**DECR**，单 Key 原子操作，比锁更高效）
- [ ] 普通购票用锁还是 DECR？（**SETNX 锁**，需要包装多个 DB 操作的事务组合）
- [ ] MySQL 防超美 SQL 怎么写？（**`UPDATE ... WHERE stock >= #{quantity}`**）
- [ ] MQ 消息丢了怎么办？（**发送失败回滚 Redis + 消费失败回滚 Redis + 定时对账兜底**）
- [ ] 消费失败重试吗？（**不重试**，避免 Redis 回滚重复导致库存膨胀）
- [ ] 怎么防重复抢购？（**Redis Set SISMEMBER 判重**）
- [ ] 6 个微服务分别是什么？
- [ ] Gateway 路由规则是什么？为什么 StripPrefix=0？
- [ ] 限流用的什么算法？（**固定窗口计数器**）
- [ ] 缓存用 Spring AOP 还是什么？（**@Around AOP + SpEL 表达式**）
- [ ] 和 RocketMQ/Kafka 比，为什么选 RabbitMQ？
- [ ] 最大的技术难点是什么？（**秒杀数据一致性**）
- [ ] 重新做会怎么改进？（**Redisson / Sentinel / Canal / Prometheus**）
