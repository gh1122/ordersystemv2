# CLAUDE.md - 项目说明

## 关于开发者

- **背景**：前端开发工程师，正在学习 Spring Boot（已有初步项目经验）
- **技术栈**：熟悉前端技术（JavaScript/TypeScript、Vue/React 等），后端入门阶段
- **沟通偏好**：
  - 解释后端概念时，尽量用前端能理解的类比（比如：Spring Bean ≈ 前端单例对象，AOP ≈ 高阶组件/装饰器，依赖注入 ≈ props 注入，Interceptor ≈ 前端路由守卫，Filter ≈ axios 请求拦截器）
  - 涉及专业术语时给出简要说明
  - 代码改动要解释"为什么"，不只是"怎么做"
- **学习方式**：代码自己手敲为主，看懂原理再写，不直接复制粘贴

## 项目概述

- **项目名**：order-system（订单管理系统）
- **用途**：Spring Boot 综合练习项目，覆盖企业级开发核心知识点
- **技术栈**：
  - Spring Boot 3.x（Web、Security、AOP、Scheduling）
  - MyBatis-Plus（ORM）
  - MySQL 8（关系型数据库）
  - Redis（缓存 + Session）
  - Kafka（消息中间件）
  - JWT（Token 鉴权）
  - Docker（容器化部署 MySQL/Redis/Kafka）

## 项目功能模块

### 1. 用户认证模块
- 注册 / 登录 / 修改密码 / 注销
- Token 体系：JWT 签发、刷新、校验
- 密码加密：BCrypt

### 2. 用户管理模块
- 用户列表 CRUD
- 分页查询（MyBatis-Plus Pagination）
- 多条件模糊搜索

### 3. 权限管理模块
- RBAC 角色权限模型（用户-角色-菜单）
- 动态菜单栏（不同角色看到不同菜单）
- 接口权限控制（注解 + 拦截器）

### 4. 订单模块
- 订单 CRUD
- 事务管理（@Transactional）
- 定时任务（@Scheduled / Quartz）
- 聚合函数统计（COUNT/SUM/AVG/GROUP BY）

## 项目结构

```
order-system/
├── pom.xml                          # Maven 依赖
├── docker-compose.yml               # Docker 编排（MySQL + Redis + Kafka）
├── sql/
│   └── init.sql                     # 数据库初始化脚本
├── src/main/
│   ├── java/com/gh/ordersystem/
│   │   ├── OrderSystemApplication.java  # 启动类
│   │   ├── config/                  # 配置类
│   │   │   ├── MybatisPlusConfig.java   # MyBatis-Plus 配置（分页插件等）
│   │   │   ├── RedisConfig.java         # Redis 序列化配置
│   │   │   ├── CorsConfig.java          # 跨域配置
│   │   │   └── KafkaConfig.java         # Kafka 配置
│   │   ├── controller/              # 控制层 —— 接收请求、返回响应
│   │   │   ├── UserController.java
│   │   │   ├── RoleController.java
│   │   │   ├── MenuController.java
│   │   │   └── OrderController.java
│   │   ├── service/                 # 业务逻辑层
│   │   │   ├── UserService.java         # 接口
│   │   │   ├── impl/
│   │   │   │   └── UserServiceImpl.java # 实现
│   │   │   ├── RoleService.java
│   │   │   ├── MenuService.java
│   │   │   └── OrderService.java
│   │   ├── repository/              # 数据访问层（MyBatis-Plus Mapper）
│   │   │   ├── UserMapper.java
│   │   │   ├── RoleMapper.java
│   │   │   ├── MenuMapper.java
│   │   │   └── OrderMapper.java
│   │   ├── model/                   # 数据模型
│   │   │   ├── entity/              # 实体类（对应数据库表）
│   │   │   ├── dto/                 # 接收前端参数
│   │   │   ├── vo/                  # 返回给前端（已脱敏）
│   │   │   └── enums/               # 枚举类
│   │   ├── interceptor/             # 拦截器（类似前端路由守卫）
│   │   │   └── JwtInterceptor.java
│   │   ├── filter/                  # 过滤器（类似 axios 拦截器）
│   │   │   └── CorsFilter.java
│   │   ├── annotation/              # 自定义注解
│   │   │   └── RequireRole.java
│   │   ├── exception/               # 全局异常处理
│   │   │   ├── GlobalExceptionHandler.java
│   │   │   └── BusinessException.java
│   │   ├── scheduler/               # 定时任务
│   │   │   └── OrderScheduler.java
│   │   ├── kafka/                   # Kafka 生产者/消费者
│   │   │   ├── OrderProducer.java
│   │   │   └── OrderConsumer.java
│   │   └── util/                    # 工具类
│   │       ├── JwtUtil.java
│   │       └── RedisUtil.java
│   └── resources/
│       ├── application.yml          # 主配置
│       └── mapper/                  # MyBatis XML（复杂 SQL）
└── note/                            # 学习笔记
```

## 核心知识点贯穿

| 知识点 | 在项目中的体现 |
|--------|---------------|
| **Spring 分层架构** | Controller → Service → Repository 三层职责清晰 |
| **依赖注入** | @Autowired / @Resource，构造器注入 |
| **MyBatis-Plus** | BaseMapper CRUD、Wrapper 条件构造、分页插件 |
| **拦截器** | JwtInterceptor 校验 Token（类似路由守卫） |
| **过滤器** | CorsFilter 处理跨域（类似 axios 拦截器） |
| **全局配置** | @ConfigurationProperties、application.yml |
| **CORS** | CorsConfig 全局跨域配置 |
| **Session/Token** | JWT 无状态鉴权 + Redis 存储登录态 |
| **事务** | @Transactional 订单创建（多表操作） |
| **定时任务** | @Scheduled 订单超时取消 |
| **聚合函数** | 订单统计（COUNT/SUM/GROUP BY） |
| **Redis** | 缓存、Session、分布式锁 |
| **Kafka** | 订单异步通知、日志收集 |
| **Docker** | docker-compose 编排中间件 |

## 重要约定

- Entity 不能直接返回给前端，必须转成 VO
- 数据库字段用下划线命名（如 `create_time`），Java 字段用驼峰（如 `createTime`）
- 主键策略：数据库自增（`IdType.AUTO`）
- 统一响应格式：`Result<T>{ code, message, data }`
- 异常统一由 GlobalExceptionHandler 处理

## 数据库设计（核心表）

```
t_user        —— 用户表
t_role        —— 角色表
t_menu        —— 菜单表
t_user_role   —— 用户-角色关联表
t_role_menu   —— 角色-菜单关联表
t_order       —— 订单表
t_order_item  —— 订单明细表
t_product     —— 商品表
```

## 学习顺序建议

1. **第一步**：搭建项目骨架 + 配置 MySQL/Redis/Kafka（Docker）
2. **第二步**：用户注册登录 + JWT Token 体系 + 拦截器
3. **第三步**：用户列表 CRUD + 分页 + 全局异常处理
4. **第四步**：RBAC 权限模型 + 动态菜单
5. **第五步**：订单系统 + 事务 + 聚合统计
6. **第六步**：定时任务 + Kafka 异步消息
7. **第七步**：CORS 跨域 + 前后端联调
