# 07 - 登录接口与 JWT 鉴权

> 日期：2026-09-15

---

## 一、今日完成

### 1. 密码 BCrypt 加密

**问题**：之前注册时密码是明文存储，不安全。

**解决**：BCrypt 是单向哈希算法，每次加密结果不同，自带盐值防彩虹表。

类比前端：`bcryptjs.hash(pwd, 10)` / `bcryptjs.compare(pwd, hash)`

#### 改动 1：SecurityConfig 注册 PasswordEncoder Bean

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

#### 改动 2：注册时加密密码

```java
// 之前
newUser.setPassword(password);

// 改成
newUser.setPassword(passwordEncoder.encode(password));
```

#### 注意

数据库里**已有的明文密码**会比对失败，需要清空测试数据：

```sql
USE ordersystem;
TRUNCATE TABLE t_user;
```

---

### 2. 登录接口（JWT 签发）

#### JWT 是什么？

JSON Web Token，三段用 `.` 连接：

```
eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjF9.FAKE_SIGNATURE
└─────────┘ └────────────────────┘ └────────────┘
  Header        Payload（用户信息）    签名（防篡改）
```

类比前端：登录成功返回 `{ token: "xxx" }`，前端存 localStorage，以后每次请求带上。

#### 新建文件

| 文件 | 作用 |
|------|------|
| `model/dto/UserLoginDTO.java` | 登录参数（username + password） |
| `util/JwtUtil.java` | 生成 Token + 验证 Token |

#### JwtUtil 核心代码

```java
@Component
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration}")
    private Long expiration;

    // 生成 Token
    public String createToken(Integer userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);

        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSecretKey())
                .compact();
    }

    // 验证并解析 Token
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
```

#### UserService 加 login 方法

**接口**：
```java
BaseVo<Map<String, String>> login(UserLoginDTO dto);
```

**实现**：
```java
@Override
public BaseVo<Map<String, String>> login(UserLoginDTO dto) {
    // 1. 查用户
    User user = UserMapper.findByUsername(dto.getUsername());
    if (user == null) {
        return BaseVo.error(ErrorCode.USER_NOT_FOUND.getCode(),
                            ErrorCode.USER_NOT_FOUND.getMessage());
    }

    // 2. 比对密码（BCrypt）
    if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
        return BaseVo.error(ErrorCode.PASSWORD_ERROR.getCode(),
                            ErrorCode.PASSWORD_ERROR.getMessage());
    }

    // 3. 签发 JWT
    String token = jwtUtil.createToken(user.getId(), user.getUsername());

    // 4. 返回 token
    Map<String, String> data = new HashMap<>();
    data.put("token", token);
    return BaseVo.success(data);
}
```

#### Controller 加登录端点

```java
@PostMapping("/login")
public BaseVo<Map<String, String>> login(@Valid @RequestBody UserLoginDTO dto) {
    return userService.login(dto);
}
```

#### 新增错误码

```java
USER_NOT_FOUND(1005, "用户不存在"),
PASSWORD_ERROR(1006, "密码错误");
```

#### 登录返回示例

```json
{
  "code": 200,
  "success": true,
  "message": "操作成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9..."
  }
}
```

---

### 3. JWT 拦截器

**作用**：在请求到达 Controller 之前验证 Token。

类比前端：路由守卫 `router.beforeEach()`。

#### 新建 JwtInterceptor

```java
@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(401);
            response.getWriter().write("未登录或Token无效");
            return false;  // 不放行
        }

        String token = authHeader.substring(7);  // 去掉 "Bearer "

        try {
            Claims claims = jwtUtil.parseToken(token);
            request.setAttribute("userId", claims.get("userId"));
            return true;  // 放行
        } catch (Exception e) {
            response.setStatus(401);
            response.getWriter().write("Token无效或已过期");
            return false;
        }
    }
}
```

**关键点**：

| 代码 | 作用 |
|------|------|
| `implements HandlerInterceptor` | Spring 拦截器接口 |
| `return false` | 不放行，请求到此结束 |
| `request.setAttribute("userId", ...)` | 把 userId 传给 Controller |

#### 注册拦截器：WebMvcConfig

```java
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private JwtInterceptor jwtInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")           // 拦截所有请求
                .excludePathPatterns(              // 排除不需要拦截的
                    "/login",
                    "/user/register",
                    "/hello"
                );
    }
}
```

---

### 4. Redis 存登录态

**为什么？** JWT 签发后无法作废。比如用户改了密码，旧 Token 在过期前照样能用。

**解决**：签发 Token 时，把 userId 存到 Redis，设置同样过期时间。验证时双重检查。

#### 新建 RedisConfig（配置序列化）

```java
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(stringSerializer);
        template.setHashValueSerializer(stringSerializer);

        template.afterPropertiesSet();
        return template;
    }
}
```

**为什么要自己配？** 默认用 JDK 序列化，Redis 里存的是乱码。用 String 序列化后清晰可读：

```
Key: login:userId:1    ← 清清楚楚（而不是乱码）
```

#### 新建 RedisUtil（封装操作）

```java
@Component
public class RedisUtil {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // 存值，带过期时间（分钟）
    public void set(String key, Object value, long minutes) {
        redisTemplate.opsForValue().set(key, value, minutes, TimeUnit.MINUTES);
    }

    // 取值
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    // 删除
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    // 判断 key 是否存在
    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}
```

#### 登录时存 Token 到 Redis

```java
// 签发 JWT 后
String token = jwtUtil.createToken(user.getId(), user.getUsername());

// 存 Redis，key = "login:userId:1"，24小时过期
String redisKey = "login:userId:" + user.getId();
redisUtil.set(redisKey, user.getId(), 24 * 60);
```

#### 拦截器加 Redis 验证

```java
Claims claims = jwtUtil.parseToken(token);
Integer userId = (Integer) claims.get("userId");

// Redis 里还有这个用户吗？
String redisKey = "login:userId:" + userId;
if (!redisUtil.hasKey(redisKey)) {
    response.setStatus(401);
    response.getWriter().write("{\"code\":401,\"message\":\"登录已失效，请重新登录\"}");
    return false;
}
```

#### 退出登录接口

**Controller**：
```java
@PostMapping("/logout")
public BaseVo<String> logout(HttpServletRequest request) {
    Integer userId = (Integer) request.getAttribute("userId");
    return userService.logout(userId);
}
```

**Service 实现**：
```java
@Override
public BaseVo<String> logout(Integer userId) {
    String redisKey = "login:userId:" + userId;
    redisUtil.delete(redisKey);  // 删掉 → Token 立即失效
    return BaseVo.success("退出登录成功");
}
```

---

## 二、完整请求流程

### 登录

```
POST /api/user/login
Body: { "username": "john", "password": "123456" }

→ 查用户 → 不存在 → 返回 1005
→ 比密码 → 不对   → 返回 1006
→ 签发 JWT + 存 Redis → 返回 token
```

### 需要登录的请求

```
GET /api/user/list
Header: Authorization: Bearer eyJ...

→ 拦截器验 JWT 签名 → 失败 → 401
→ 查 Redis 有没有    → 没有 → 401（登录失效）
→ 放行 → Controller
```

### 退出登录

```
POST /api/user/logout
Header: Authorization: Bearer eyJ...

→ 拦截器放行（Token 还有效）
→ Controller → Service 删 Redis
→ 旧 Token 全部失效
```

---

## 三、新建/修改文件清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `config/SecurityConfig.java` | 修改 | 加 PasswordEncoder Bean |
| `service/impl/UserServiceImpl.java` | 修改 | register 加 BCrypt 加密，加 login/logout 方法 |
| `model/enums/ErrorCode.java` | 修改 | 加 USER_NOT_FOUND、PASSWORD_ERROR |
| `model/dto/UserLoginDTO.java` | 新建 | 登录参数 DTO |
| `util/JwtUtil.java` | 新建 | JWT 生成 + 验证 |
| `util/RedisUtil.java` | 新建 | Redis 操作封装 |
| `config/RedisConfig.java` | 新建 | RedisTemplate 序列化配置 |
| `interceptor/JwtInterceptor.java` | 新建 | JWT 拦截器 |
| `config/WebMvcConfig.java` | 新建 | 注册拦截器 + 放行路径 |
| `service/UserService.java` | 修改 | 加 login、logout 方法签名 |
| `controller/UserController.java` | 修改 | 加 /login、/logout 端点 |

---

## 四、知识点总结

| 知识点 | 说明 | 前端类比 |
|--------|------|---------|
| BCrypt | 单向哈希，自带盐值 | `bcryptjs.hash()` / `compare()` |
| PasswordEncoder | Spring 密码加密统一接口 | — |
| JWT | 无状态令牌，Header.Payload.Signature | 登录返回的 token |
| `@Override` | 检查是否正确重写接口方法 | TS 的 `implements` 检查 |
| HandlerInterceptor | 请求前置拦截 | 路由守卫 `beforeEach()` |
| WebMvcConfigurer | 注册拦截器 | `router.beforeEach()` 配置 |
| RedisTemplate | Spring 操作 Redis 的工具 | 类似 axios |
| StringRedisSerializer | String 序列化（可读） | `JSON.stringify()` |

---

## 五、踩坑记录

### 1. JwtUtil 注入失败

**报错**：`required a bean of type 'com.gh.ordersystem.util.JwtUtil' that could not be found`

**原因**：JwtUtil 类上忘了加 `@Component`

**解决**：加 `@Component`，让 Spring 扫描到并创建 Bean。

### 2. RedisTemplate 注入失败

**报错**：`required a bean of type 'org.springframework.data.redis.core.RedisTemplate' that could not be found`

**原因**：没有自定义 RedisTemplate Bean，自动配置可能不完整。

**解决**：新建 `RedisConfig`，手动注册 RedisTemplate Bean，并配置 StringRedisSerializer。

### 3. @Override 的作用

**为什么加？** 告诉编译器"这个方法是重写接口的"，如果方法名拼错了（比如 `logn`），编译就报错，而不是运行起来才发现没调用到。

---

## 六、待完成（下次做）

- [ ] 用户列表 CRUD + 分页查询
- [ ] 多条件模糊搜索
- [ ] RBAC 权限模型（角色-菜单）
- [ ] 动态菜单栏
- [ ] 接口权限控制（注解 + 拦截器配合）
