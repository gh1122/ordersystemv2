# ordersystemv2 - 订单管理系统

Spring Boot 后端学习项目，实现用户登录（JWT）、RBAC 权限模型、动态菜单、商品与订单管理。

## 技术栈

| 类别 | 技术 |
|---|---|
| 语言 / 基础框架 | Java 17、Spring Boot 3.2.5 |
| Web / 安全 | Spring Web、Spring Security + JJWT（JWT 认证）、Validation、AOP |
| ORM / 数据库 | MyBatis-Plus 3.5.5、MySQL 8.0（Docker 容器，宿主机端口 3307） |
| 中间件 | Redis 7（Lettuce）、Kafka 3（KRaft） |
| 工具库 | Lombok、Hutool 5.8.27 |

## 项目启动

1. 启动基础设施（MySQL / Redis / Kafka）

    ```powershell
    docker compose up -d
    ```

2. 启动应用：IDEA 运行 `Ordersystemv2Application`，或执行

    ```powershell
    mvn spring-boot:run
    ```

3. 访问地址：`http://localhost:8087/api`

## 数据库更新

`sql/init.sql` 是数据库初始化脚本（幂等，可重复执行），更新分两种场景：

**全新环境（数据卷为空时自动执行）**

第一次 `docker compose up -d` 时，MySQL 容器会自动执行挂载在 `/docker-entrypoint-initdb.d` 的 `init.sql`，无需手动操作。

**日常改动同步到现有库（手动执行）**

改了 `init.sql`（如新增表）后，数据卷已有数据不会自动同步，需手动执行：

```powershell
docker cp sql\init.sql ordersystem-mysql:/tmp/init.sql
docker exec ordersystem-mysql sh -c "mysql -uroot -proot --default-character-set=utf8mb4 < /tmp/init.sql"
```

**彻底重置（⚠️ 清空所有数据）**

```powershell
docker compose down -v   # 删除数据卷，库内数据全部丢失
docker compose up -d     # 重新走首次初始化，自动执行 init.sql
```
