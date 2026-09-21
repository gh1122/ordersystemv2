# 08 - 用户列表 CRUD 与分页查询

> 日期：2026-09-21

---

## 一、今日完成

### 1. 用户列表查询（GET /user/list）

**需求**：获取所有用户列表（需要登录）。

**类比前端**：`axios.get('/api/user/list')` → 返回用户数组

#### Service 层

```java
@Override
public BaseVo<List<UserVo>> getUserList() {
    // 1. 查数据库（返回 Entity 列表）
    List<User> users = userMapper.selectList(null);
    
    // 2. Entity → VO 转换（脱敏：去掉密码等敏感字段）
    List<UserVo> voList = users.stream()
        .map(this::toUserVo)
        .collect(Collectors.toList());
    
    return BaseVo.success(voList);
}

// 抽一个转换方法，复用
private UserVo toUserVo(User user) {
    UserVo vo = new UserVo();
    vo.setId(user.getId());
    vo.setUsername(user.getUsername());
    vo.setPhone(user.getPhone());
    vo.setCreateTime(user.getCreateTime());
    return vo;
}
```

**为什么转 VO？**

Entity 包含密码等敏感字段，VO 只返回前端需要的字段。

类比前端：接口返回数据要做脱敏处理。

---

### 2. 分页查询（MyBatis-Plus Pagination）

**需求**：支持分页查询，返回分页信息（总条数、总页数、当前页数据）。

**类比前端**：
```javascript
axios.get('/api/user/page', {
  params: { pageNum: 1, pageSize: 10 }
})
```

#### 第一步：配置分页插件（必须）

**新建 `config/MybatisPlusConfig.java`**：

```java
@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
```

**为什么需要？** 类似前端插件需要 `Vue.use()` 注册。

#### 第二步：分页参数 DTO

**新建 `model/dto/UserQueryDTO.java`**：

```java
@Data
public class UserQueryDTO {
    private Integer pageNum = 1;    // 第几页，默认第 1 页
    private Integer pageSize = 10;  // 每页几条，默认 10 条
}
```

#### 第三步：Service 实现

```java
@Override
public BaseVo<Page<UserVo>> getUserPage(UserQueryDTO dto) {
    // 1. 创建分页对象（第几页，每页几条）
    Page<User> page = new Page<>(dto.getPageNum(), dto.getPageSize());
    
    // 2. 执行分页查询
    userMapper.selectPage(page, null);
    
    // 3. 转换成 VO 分页
    Page<UserVo> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
    List<UserVo> voList = page.getRecords().stream()
        .map(this::toUserVo)
        .collect(Collectors.toList());
    voPage.setRecords(voList);
    
    return BaseVo.success(voPage);
}
```

#### 第四步：Controller

```java
@GetMapping("/user/page")
public BaseVo<Page<UserVo>> getUserPage(UserQueryDTO dto) {
    return userService.getUserPage(dto);
}
```

**注意**：参数不加 `@RequestBody`，分页参数用 URL 传参：`/user/page?pageNum=1&pageSize=10`

---

### 3. 多条件模糊搜索

**需求**：支持按用户名、手机号模糊搜索。

**类比前端**：
```javascript
axios.get('/api/user/page', {
  params: { pageNum: 1, pageSize: 10, username: 'user', phone: '138' }
})
```

**等价 SQL**：
```sql
SELECT * FROM t_user 
WHERE username LIKE '%user%' AND phone LIKE '%138%'
LIMIT 0, 10
```

#### 第一步：UserQueryDTO 加搜索字段

```java
@Data
public class UserQueryDTO {
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    
    // 👇 新增搜索条件
    private String username;
    private String phone;
}
```

#### 第二步：Service 使用 Wrapper 构建条件

```java
@Override
public BaseVo<Page<UserVo>> getUserPage(UserQueryDTO dto) {
    Page<User> page = new Page<>(dto.getPageNum(), dto.getPageSize());
    
    // 👇 构建查询条件
    QueryWrapper<User> wrapper = new QueryWrapper<>();
    
    if (dto.getUsername() != null && !dto.getUsername().isEmpty()) {
        wrapper.like("username", dto.getUsername());
    }
    
    if (dto.getPhone() != null && !dto.getPhone().isEmpty()) {
        wrapper.like("phone", dto.getPhone());
    }
    
    wrapper.orderByDesc("create_time");
    
    userMapper.selectPage(page, wrapper);
    
    // 转换 VO...
}
```

#### Wrapper 语法速查

```java
wrapper.like("username", "user");     // LIKE '%user%'
wrapper.eq("status", 1);              // = 1
wrapper.ge("age", 18);                // >= 18
wrapper.between("age", 18, 60);       // BETWEEN 18 AND 60
wrapper.orderByDesc("create_time");   // ORDER BY create_time DESC
```

---

### 4. 用户信息修改（PUT /user/update）

**需求**：修改用户名、手机号等信息。

#### UserUpdateDTO

```java
@Data
public class UserUpdateDTO {
    @NotNull(message = "用户ID不能为空")
    private Integer id;
    
    private String username;
    private String phone;
}
```

#### Service 实现

```java
@Override
public BaseVo<String> updateUser(UserUpdateDTO dto) {
    User user = userMapper.selectById(dto.getId());
    if (user == null) {
        return BaseVo.error(1005, "用户不存在");
    }
    
    if (dto.getUsername() != null) {
        user.setUsername(dto.getUsername());
    }
    if (dto.getPhone() != null) {
        user.setPhone(dto.getPhone());
    }
    
    userMapper.updateById(user);
    return BaseVo.success("修改成功");
}
```

#### Controller

```java
@PutMapping("/user/update")
public BaseVo<String> updateUser(@RequestBody @Valid UserUpdateDTO dto) {
    return userService.updateUser(dto);
}
```

---

### 5. 用户删除（DELETE /user/{id}）

**需求**：按 ID 删除用户。

#### Service 实现

```java
@Override
public BaseVo<String> deleteUser(Integer id) {
    User user = userMapper.selectById(id);
    if (user == null) {
        return BaseVo.error(1005, "用户不存在");
    }
    
    userMapper.deleteById(id);
    return BaseVo.success("删除成功");
}
```

#### Controller

```java
@DeleteMapping("/user/{id}")
public BaseVo<String> deleteUser(@PathVariable Integer id) {
    return userService.deleteUser(id);
}
```

**`@PathVariable` 解释**：URL 路径里的参数，类比 Vue Router 的 `this.$route.params.id`。

---

## 二、踩坑记录

### 1. UserMapper 未继承 BaseMapper

**报错**：`The method selectPage(Page<User>, null) is undefined for the type UserMapper`

**原因**：`UserMapper` 没有继承 `BaseMapper<User>`，无法使用 MyBatis-Plus 内置方法。

**解决**：
```java
@Mapper
public interface UserMapper extends BaseMapper<User> {
    // 自定义方法保留
    @Select("SELECT * FROM t_user WHERE username = #{username}")
    User findByUsername(String username);
}
```

### 2. 主键类型警告

**警告**：`This primary key of "id" is primitive !不建议如此请使用包装类`

**原因**：User 实体类的 `id` 使用了 `int`（基本类型）。

**解决**：改成 `Integer`（包装类）。
```java
// 之前
private int id;

// 改成
private Integer id;
```

---

## 三、新建/修改文件清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `config/MybatisPlusConfig.java` | 新建 | 分页插件配置 |
| `model/dto/UserQueryDTO.java` | 新建 | 分页 + 搜索参数 |
| `model/dto/UserUpdateDTO.java` | 新建 | 修改用户参数 |
| `repository/UserMapper.java` | 修改 | 继承 BaseMapper |
| `service/UserService.java` | 修改 | 加方法签名 |
| `service/impl/UserServiceImpl.java` | 修改 | 实现 CRUD + 分页 + 搜索 |
| `controller/UserController.java` | 修改 | 加端点 |
| `model/entity/User.java` | 修改 | id 改为 Integer |

---

## 四、知识点总结

| 知识点 | 说明 | 前端类比 |
|--------|------|---------|
| BaseMapper | MyBatis-Plus 内置 CRUD 方法 | class 继承 |
| Page<T> | 分页对象（records、total、pages） | 分页组件状态 |
| QueryWrapper | WHERE 条件构造器 | SQL 条件拼接 |
| @PathVariable | URL 路径参数 | Vue Router params |
| Entity → VO | 数据脱敏 | 接口数据转换 |
| Integer vs int | 包装类可为 null | let id = null |

---

## 五、CRUD 接口汇总

| 操作 | HTTP 方法 | URL | 说明 |
|------|-----------|-----|------|
| 注册 | POST | `/user/register` | 创建用户 |
| 登录 | POST | `/login` | 获取 Token |
| 退出 | POST | `/logout` | 失效 Token |
| 列表 | GET | `/user/page` | 分页 + 搜索 |
| 修改 | PUT | `/user/update` | 修改用户信息 |
| 删除 | DELETE | `/user/{id}` | 删除用户 |

---

## 六、待完成（下次做）

- [ ] RBAC 权限模型（用户-角色-菜单）
- [ ] 动态菜单栏
- [ ] 接口权限控制（注解 + 拦截器配合）
