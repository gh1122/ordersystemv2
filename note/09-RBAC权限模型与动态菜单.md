# 09 - RBAC 权限模型与动态菜单（进行中）

> 开始日期：2026-10-10
> 用途：RBAC 是分多次完成的模块，本文档是**总纲**，随时回来看进度和关键设计。

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
| 3 | 用户侧边栏菜单 `GET /menu/nav`（三表查询链路 + 复用 buildTree） | 🔨 进行中 |
| 4 | `@RequireRole` 注解 + 拦截器接口鉴权 | ⬜ |

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

### 第 3 步：用户侧边栏菜单（当前进度，方案已定）

接口：`GET /menu/nav`，链路 userId → roleIds → menuIds → menus → buildTree。

**userId 来源**：`request.getAttribute("userId")`——JWT 拦截器解析后挂上去的。
前端类比：路由守卫把用户信息挂到 store，页面直接取用。
**为什么不能从参数收 userId？** 用户 A 传 userId=B 就越权了，必须用 Token 里解出来的。

三个防坑点：

1. **空集合不能进 `wrapper.in()`** → 生成非法 SQL `IN ()`。每步查完判空，为空提前返回空树（类比 `if (!ids.length) return []`）
2. **menuIds 要去重**（多角色菜单重叠）→ `Set<Integer>` 或 `distinct()`（类比 `[...new Set(ids)]`）
3. **三次单表查询而不是 JOIN** → 全程只用 BaseMapper + Wrapper 零手写 SQL；学 MyBatis XML 后再重构对比

改动文件：`MenuService`（加签名）、`MenuServiceImpl`（注入 UserRoleMapper/RoleMenuMapper，一个 Service 多个 Mapper 正常）、`MenuController`（加端点）。

### 第 4 步：@RequireRole 接口鉴权（规划）

- 注解是空壳元数据（类比装饰器参数），逻辑在拦截器
- 判断依据：`t_menu.permission` 标识（如 `system:user:delete`），比角色名更细
- 流程：JwtInterceptor 验 Token → 取 userId 的权限集合 → 与注解声明的 permission 比对 → 不匹配 403
- 前端类比：高阶组件 `withAuth('system:user:delete')`，业务代码零侵入

## 六、联调测试清单

- [ ] `GET /api/menu/tree`（带 Token）→ 两棵树：系统管理 + 订单管理
- [ ] `gh` 登录 → `GET /api/menu/nav` → 全部菜单（admin 角色）
- [ ] 新注册用户登录 → `/menu/nav` → 空数组（没分配角色）
- [ ] （第 3 步完成后）给 user 角色关联订单管理 → 普通用户能看到订单管理
- [ ] （第 4 步完成后）无权限调 `DELETE /user/{id}` → 403

## 七、待完成

- [ ] 第 3 步代码：MenuService / MenuServiceImpl / MenuController
- [ ] 第 4 步：@RequireRole + 拦截器
- [ ] 角色管理接口（角色 CRUD、给用户分配角色、给角色分配菜单）——动态菜单的前置管理功能
- [ ] 收尾后更新 CLAUDE.md 学习进度（目前写的还是"第二步完成"，已落后）
