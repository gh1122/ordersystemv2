# 09 - RBAC 权限模型与动态菜单（已完成 ✅ 2026-10-12）

> 开始日期：2026-10-10，完成日期：2026-10-12
> 用途：RBAC 总纲。下次做"角色管理接口"或前端联调动态菜单时，回来看进度和关键设计。

---

## 一、RBAC 概念速记

**RBAC（Role-Based Access Control）**：在"用户"和"权限"之间加一层"角色"。

```
没有 RBAC：用户 ──→ 权限        （一对一，没法复用）
有 RBAC：  用户 ──→ 角色 ──→ 权限 （通过角色复用）
```

| 概念 | 说明 | 前端类比 |
|------|------|---------|
| 用户（User） | 登录账号 | — |
| 角色（Role） | 一组权限的集合 | `class="btn-primary"`（权限的打包复用单元） |
| 权限/菜单（Menu） | 页面/按钮/接口的最小单元 | 路由的 `meta.permission` |
| 用户-角色关联 | 张三 是 管理员 | `Map<userId, roleIds[]>` |
| 角色-菜单关联 | 管理员 拥有 订单管理 | 同上 |

**为什么权限表叫 `t_menu`？** 每条记录天然对应一个页面/按钮，正好是前端渲染菜单树的数据源。`type` 字段区分：0 目录 / 1 菜单 / 2 按钮。

## 二、表设计逻辑（4 张表）

```
t_user          t_user_role           t_role         t_role_menu           t_menu
┌──────┐       ┌────────────┐        ┌───────┐      ┌────────────┐       ┌────────┐
│ id   │←──┐   │ user_id    │──→│    │ id    │ ←────│ role_id    │──→│  │ id     │
│ name │   └───│ role_id    │    └─── │ name  │      │ menu_id    │   └──│ name   │
└──────┘       └────────────┘         └───────┘      └────────────┘       └────────┘
```

**多对多必须拆中间表**——数据库无法直接表达多对多。

设计亮点（建表时自己定的，值得记住为什么）：

| 设计 | 好处 |
|------|------|
| `uk_user_role` / `uk_role_menu` 联合唯一键 | 防重复分配，`Set` 去重的作用 |
| `t_menu.parent_id`（0=根）+ `type` | 一张表撑起侧边栏树 + 按钮级权限 |
| `t_menu.permission`（如 `system:user:list`） | 接口权限的细粒度标识，第 4 步 `@RequireRole` 直接用 |
| `idx_parent_id` / `idx_menu_id` / `idx_role_id` | 树查询、反查不走全表扫 |
| `uk_role_name` | 角色名不重复 |

种子数据（`sql/init.sql`，幂等：`INSERT IGNORE` 依赖唯一键）：

- 角色：1=admin，2=user
- 菜单树：系统管理(1,目录) → 用户管理(2,菜单) → 新增用户(3,按钮)/删除用户(4,按钮)；角色管理(5,菜单)；订单管理(6,菜单,根)
- admin 关联全部菜单，user 只关联订单管理
- 第一个注册的用户（gh，id=1）设为 admin

## 三、整体数据流

```
登录 → JWT(含 userId)
  │
  ├─ 渲染侧边栏:  GET /menu/nav
  │     userId → t_user_role → roleIds
  │            → t_role_menu → menuIds（去重）
  │            → t_menu where id in (menuIds)
  │            → buildTree → 前端动态路由 + 侧边栏
  │
  ├─ 管理员配菜单: GET /menu/tree（全量，不做权限过滤）
  │
  └─ 调接口:      userId → 角色列表 → 和 @RequireRole 声明比对 → 放行/403
```

前端类比：全量树 ≈ 全量 `routes` 数组；用户菜单 ≈ 按 `meta.roles` 过滤后动态注册的路由。

## 四、实施步骤（总进度）

| 步骤 | 内容 | 状态 |
|------|------|------|
| 0 | 建表（Workbench 手动）+ 种子数据进 init.sql | ✅ |
| 1 | Entity + Mapper（4 实体 + 4 Mapper，风格与 User 一致） | ✅ |
| 2 | 完整菜单树 `GET /menu/tree`（getMenuTree + buildTree + toMenuVo） | ✅ |
| 3 | 用户侧边栏菜单 `GET /menu/nav`（三表查询链路 + 复用 buildTree） | ✅ 2026-10-12 |
| 4 | `@RequireRole` 注解 + 拦截器接口鉴权 | ✅ 2026-10-12 |

## 五、关键步骤与代码要点

### 第 1 步：Entity + Mapper

- 字段映射：数据库下划线 → Java 驼峰（全局 `map-underscore-to-camel-case` 自动转）
- 中间表也要建实体：分配角色 = 往 t_user_role 插一行，取消 = 删一行，MyBatis-Plus 增删认实体
- `status`/`type` 先用 `Integer`，枚举后续重构（先跑通，再优化）
- 不写 `@TableId`：全局 `id-type: auto` + 字段名叫 id 的约定

**踩坑**：Role 实体一开始写成 `role_name`（应为 `roleName`）和 `updateTime`（表里没有这列，应为 `createTime`）——实体和表结构不同步，插入直接 SQL 报错。

### 第 2 步：buildTree（核心难点，已定稿 Map 版）

两种 O(n) 方案对比：

| | Map 版（已采用） | groupingBy 版 |
|---|---|---|
| 索引方向 | `Map<id, node>`，**孩子找爸爸** | `Map<parentId, 孩子列表>`，**爸爸找孩子** |
| 找根 | map 里找不到父节点 = 根 | `parentId == 0` |
| 孤儿节点（父菜单不存在） | 兜底渲染成根，不丢数据 | 静默消失（生产环境烦人 bug） |

Java 直译（和前端 buildTreeByMap 完全同构）：

```java
// 第一步：全部节点存入 map，预创建 children（null 会 NPE）
Map<Integer, MenuVo> map = new HashMap<>();
for (MenuVo vo : voList) {
  vo.setChildren(new ArrayList<>());
  map.put(vo.getId(), vo);
}
// 第二步：挂载；找不到父节点 = 根节点
List<MenuVo> roots = new ArrayList<>();
for (MenuVo vo : voList) {
  MenuVo parent = map.get(vo.getParentId());
  if (parent != null) parent.getChildren().add(vo);
  else roots.add(vo);
}
return roots;
```

前端原版对照：

```javascript
function buildTreeByMap(list) {
  const map = new Map();
  const roots = [];
  for (const item of list) {
    map.set(item.id, { ...item, children: [] });
  }
  for (const item of list) {
    const node = map.get(item.id);
    const parent = map.get(item.parentId);
    if (parent) parent.children.push(node);
    else roots.push(node);
  }
  return roots;
}
```

其他要点：

- Entity → VO 用 `toMenuVo` 私有方法逐字段 set（同 toUserVo 套路）；不用构造器引用、不用 BeanUtils.copyProperties（编译期检查 > 运行期静默失败）
- 查询加 `wrapper.orderByAsc("sort")`，否则树内顺序不保证
- 包装类比较用 `equals`，不用 `==`（Integer 缓存池 -128~127 之外会翻车）

### 第 3 步：用户侧边栏菜单（已完成）

接口：`GET /menu/nav`，链路 userId → roleIds → menuIds → menus → buildTree。

**userId 来源**：`request.getAttribute("userId")`——JWT 拦截器解析后挂上去的。
前端类比：路由守卫把用户信息挂到 store，页面直接取用。
**为什么不能从参数收 userId？** 用户 A 传 userId=B 就越权了，必须用 Token 里解出来的。
**为什么 Service 不解析 Token？** 拦截器解析过还附带 Redis 登录态校验，Service 再解析是重复劳动 + 绕过校验开后门；分层上 Service 不该有 HTTP 概念（`request` 在 ServiceImpl 里根本不存在，编译报错就是架构在提醒放错层了）。

三个防坑点（都真实踩过/修复过）：

1. **空集合不能进 `wrapper.in()`** → 生成非法 SQL `IN ()`。每步查完判空，为空提前返回空树（类比 `if (!ids.length) return []`）
2. **menuIds 要去重**（多角色菜单重叠）→ `distinct()`（类比 `[...new Set(ids)]`）
3. **三次单表查询而不是 JOIN** → 全程只用 BaseMapper + Wrapper 零手写 SQL；学 MyBatis XML 后再重构对比

改动文件：`MenuService`（加签名）、`MenuServiceImpl`（注入 UserRoleMapper/RoleMenuMapper，一个 Service 多个 Mapper 正常）、`MenuController`（加端点）。

**实现实测数据**（admin 全量 vs user 只见订单管理）：

| 账号 | 角色 | /menu/nav 结果 |
|------|------|---------------|
| gh (id=1) | admin | 2 棵树（系统管理 + 订单管理） |
| xiaohong (id=2) | user | 1 棵树（订单管理） |
| 未分配角色的用户 | — | 空数组（触发判空分支） |

### 第 4 步：@RequireRole 接口鉴权（已完成）

**实际实现**（和最初"用 permission 标识"的规划略有出入，最终用的是**角色名**，permission 字段留给以后做按钮级细粒度控制）：

```
请求 → JwtInterceptor（验 Token，挂 userId）→ RoleInterceptor（验角色）→ Controller
```

- **注解** `@RequireRole`（`annotation/RequireRole.java`）：
  - `@Target(METHOD)` 限贴方法，`@Retention(RUNTIME)` 保留到运行时（拦截器靠反射读，编译期丢信息就用不了）
  - 注解是**空壳元数据**，逻辑全在拦截器——类比装饰器 `withAuth('admin')`，参数存注解的 `value()`
- **拦截器** `RoleInterceptor`（`interceptor/RoleInterceptor.java`）核心流程：
  1. `handler instanceof HandlerMethod` 强转（静态资源请求没有方法可查 → 放行）
  2. `getMethodAnnotation(RequireRole.class)` 反射取注解；**没有注解 → 放行**（大部分接口只要登录）
  3. 取 `request.getAttribute("userId")`，为空 → 403
  4. 查角色链路 userId → roleIds → roleNames（`Set<String>`）
  5. `roleNames` 和注解 `value()` 有交集 → 放行，否则 403
- **注册顺序决定生死**（WebMvcConfig）：JwtInterceptor 先注册，RoleInterceptor 才拿得到 userId——类比 Express 中间件 `app.use` 顺序
- **403 为什么手动写 JSON？** `@RestControllerAdvice` 只拦 Controller 抛的异常，拦截器阶段还没进 Controller，只能 `response.getWriter().write(...)`（和 JwtInterceptor 返回 401 同理）

用法（业务代码零侵入，这就是注解方案的价值）：

```java
@RequireRole("admin")              // 单角色；多角色 @RequireRole({"admin", "audit"})
@DeleteMapping("/user/{id}")
public BaseVo<String> deleteUser(@PathVariable Integer id) { ... }
```

四个测试用例全部通过（2026-10-12）：admin 删除 → 200；user 角色删除 → 403；无 Token → 401；未贴注解接口 → 不受影响。

## 六、联调测试清单（2026-10-12 全部通过 ✅）

- [x] `GET /api/menu/tree`（带 Token）→ 200 两棵树：系统管理 + 订单管理
- [x] `gh` 登录 → `GET /api/menu/nav` → 200 全部菜单（admin 角色，2 棵树）
- [x] 无角色用户登录 → `/menu/nav` → 200 空数组（触发判空分支，没分配角色）
- [x] `xiaohong`（user 角色）→ `/menu/nav` → 200 只有订单管理
- [x] 无权限调 `DELETE /user/{id}` → 403；admin 调 → 200；无 Token → 401
- [x] 未贴注解的接口（如 /menu/nav）→ 不受 RoleInterceptor 影响

## 七、本次新增文件清单

```
annotation/RequireRole.java       # 自定义注解（角色门禁的"标签"）
interceptor/RoleInterceptor.java  # 角色校验拦截器
config/WebMvcConfig.java          # 注册双拦截器（顺序：Jwt → Role）
model/entity/Role|Menu|UserRole|RoleMenu.java
repository/Role|Menu|UserRole|RoleMenuMapper.java
model/vo/MenuVo.java              # 比 Entity 多 children 字段（树形自引用）
service/MenuService + impl        # getMenuTree / getNavMenu / buildTree / toMenuVo
controller/MenuController.java    # GET /menu/tree（全量）+ GET /menu/nav（按权限）
```

账号密码备忘：gh / xiaohong 的密码都是 `123456`（xiaohong 是重置的，id=2；user001 的密码当时批量插入的另说）。

## 八、本次踩坑记录（重要程度排序）

1. **实体和表结构不同步 → SQL 直接报错**：Role 实体写成 `role_name`（应为驼峰 `roleName`）和 `updateTime`（表里根本没有这列，应为 `createTime`）。教训：实体字段必须和表 DDL 一一核对，MyBatis-Plus 插入时会把实体字段当列名写。
2. **Controller `return null` → 200 + `Content-Length: 0`**：Spring 把 null 序列化为空 body，不报错。排查三板斧：看状态码、看 Content-Length、看实际 body。200 空基本 = Controller 没返回数据。
3. **改了代码"重启"但 JVM 没换**：class 文件 10:19 编译，进程 10:12 启动——跑的还是旧类。**Spring Boot 改代码必须真正停止→启动**（没有 Vite 那种热更新），排查时对比进程启动时间和 class 编译时间。
4. **PowerShell 吞 `$` 符号**：命令里写 BCrypt 哈希 `$2a$10$...`，`$2a`/`$10` 被当成变量解析成空，哈希存坏了。解法：整段用单引号 + SQL 里 `''` 转义。教训：含 `$` 的字符串在 PowerShell 双引号里必须警惕。
5. **忘记 `@Slf4j` 对应的调试姿势**：`System.out.println` 调试完要删；`log.info("x: {}", var)` 占位符惰性求值，级别一关自动安静，可以永远留在代码里。

顺手学到的进阶写法（RoleInterceptor 里自己用出来的）：

- `LambdaQueryWrapper`：`eq(UserRole::getUserId, ...)` 方法引用代替字符串列名，列名写错编译期就报
- `instanceof HandlerMethod hm` 模式匹配：判断 + 强转一行搞定（Java 16+）

## 九、待完成（遗留到下次）

- [ ] 角色管理接口（角色 CRUD、给用户分配角色、给角色分配菜单）——RBAC 的管理端，现在关联数据全靠手插 SQL
- [ ] 细粒度按钮权限：`t_menu.permission` 字段已备好（如 `system:user:delete`），可升级 @RequireRole 支持 permission 维度
- [ ] 前端联调：Vue/React 侧动态路由 + 侧边栏渲染 `/menu/nav` 返回的树（第七步统一做）
- [ ] 下一大步：**第五步 订单系统 + @Transactional 事务 + 聚合统计**（新开笔记 10）
