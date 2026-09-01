# Docker 命令速查

> ordersystemv2 项目的 Docker 常用命令

---

## 启动服务

```powershell
# 进入项目目录
cd "C:\Users\GuoHao\Desktop\个人\qzkf\ordersystemv2"

# 启动所有服务（后台运行）
docker compose up -d
```

---

## 停止服务

```powershell
# 停止容器（不删除）
docker compose stop

# 停止并删除容器（数据卷保留）
docker compose down

# 停止并删除容器 + 数据卷（彻底重置，数据全丢）
docker compose down -v
```

---

## 查看状态

```powershell
# 查看运行中的容器
docker ps

# 查看所有容器（包括停止的）
docker ps -a

# 查看日志
docker compose logs -f mysql
docker compose logs -f redis
docker compose logs -f kafka
```

---

## 重启服务

```powershell
# 重启单个容器
docker compose restart mysql

# 重启所有
docker compose restart
```

---

## 常见问题

### 端口被占用

```
Error: ports are not available: exposing port TCP 0.0.0.0:3306
```

**解决**：改 `docker-compose.yml` 的端口映射，比如 `"3307:3306"`

---

## 服务端口

| 服务 | 容器端口 | 本地端口 | 说明 |
|------|---------|---------|------|
| MySQL | 3306 | 3307 | 数据库（宿主机 3306 被占用，改用 3307） |
| Redis | 6379 | 6379 | 缓存 |
| Kafka | 9092 | 9092 | 消息队列 |

---

## 启动顺序

```
① docker compose up -d    → 启动三个服务
② docker ps              → 确认都是 Up 状态
③ 启动 Spring Boot 项目  → 端口 8087
④ curl 测试接口          → 验证
```
