# Tourism System 旅游信息系统

基于 Spring Boot + Vue3 的旅游信息管理系统，支持景区浏览、门票预订、支付宝支付、攻略发布等功能。

## 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | Vue 3 + Element Plus + Vue Router + Pinia + Axios |
| 后端 | Spring Boot 3.2 + MyBatis-Plus + Spring Security + JWT |
| 数据库 | MySQL + Redis |
| 文档 | Knife4j (Swagger) |
| 支付 | 支付宝沙箱 (Alipay SDK) |
| 工具 | Hutool, Lombok |

## 功能模块

- 景区管理：分类浏览、详情展示、收藏
- 门票预订：在线购票、订单管理
- 支付宝支付：沙箱环境支付流程
- 旅游攻略：发布、编辑、浏览攻略
- 住宿管理：住宿信息查询与评价
- 评论系统：景区/攻略评论与点赞
- 用户中心：注册登录、个人信息、收藏管理
- 后台管理：数据看板、内容管理、用户管理
- 轮播图管理、邮件通知

## 项目结构

```
├── springboot/          # Spring Boot 后端
│   ├── src/main/java/   # Java 源码
│   ├── src/main/resources/
│   │   └── application.properties.example  # 配置模板
│   └── pom.xml
├── vue3/                # Vue3 前端
│   ├── src/views/       # 页面组件
│   │   ├── frontend/    # 用户端
│   │   └── backend/     # 管理后台
│   ├── public/
│   └── package.json
├── sql/                 # 数据库 SQL
│   └── tourism_system.sql
└── README.md
```

## 快速开始

### 环境要求

- JDK 17+
- Node.js 16+
- MySQL 8.0+
- Redis
- Maven 3.6+

### 后端启动

```bash
cd springboot

# 复制配置模板并填入真实值
cp src/main/resources/application.properties.example src/main/resources/application.properties

# 启动
mvn spring-boot:run
```

### 前端启动

```bash
cd vue3

# 安装依赖
npm install

# 启动开发服务器
npm run serve
```

### 数据库

导入 `sql/tourism_system.sql` 到 MySQL 数据库。

## 配置说明

复制 `springboot/src/main/resources/application.properties.example` 为 `application.properties`，并填入以下配置：

| 配置项 | 说明 |
|--------|------|
| `spring.datasource.*` | MySQL 数据库连接信息 |
| `spring.mail.*` | QQ 邮箱 SMTP 配置 |
| `spring.data.redis.*` | Redis 连接信息 |
| `alipay.*` | 支付宝沙箱应用配置 |
| `knife4j.basic.*` | API 文档认证信息 |

> 注意：`application.properties` 已在 `.gitignore` 中排除，请勿将真实密钥提交到仓库。
