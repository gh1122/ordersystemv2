# SQL 语法速查

> 日期：2026-08-17

---

## 一、数据库操作（DDL）

### 查看所有数据库
```sql
SHOW DATABASES;
```

### 创建数据库
```sql
CREATE DATABASE study DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
```
> `utf8mb4` 支持 emoji 和生别字，`utf8mb4_unicode_ci` 是不区分大小写的排序规则。

### 选择当前使用的数据库
```sql
USE study;
```

### 删除数据库（⚠️ 危险操作，会删掉所有表和数据）
```sql
DROP DATABASE study;
```

---

## 二、表操作（DDL）

### 查看当前库的所有表
```sql
SHOW TABLES;
```

### 查看表结构
```sql
DESC user;
-- 或者
SHOW CREATE TABLE user;
```

### 创建表
```sql
CREATE TABLE user (
  id          INT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  name        VARCHAR(50) NOT NULL COMMENT '用户名',
  password    VARCHAR(255) NOT NULL COMMENT '密码',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
```

### 删除表（⚠️ 危险操作）
```sql
DROP TABLE user;
```

### 修改表 - 添加字段
```sql
ALTER TABLE user ADD email VARCHAR(100) COMMENT '邮箱';
```

### 修改表 - 修改字段类型
```sql
ALTER TABLE user MODIFY email VARCHAR(200) COMMENT '邮箱';
```

### 修改表 - 删除字段
```sql
ALTER TABLE user DROP COLUMN email;
```

### 修改表 - 改字段名
```sql
ALTER TABLE user CHANGE email mail VARCHAR(200) COMMENT '邮箱';
```

---

## 三、数据操作（DML）

### 插入数据

```sql
-- 插入完整一行
INSERT INTO user (name, password) VALUES ('张三', '123456');

-- 插入多行
INSERT INTO user (name, password) VALUES
  ('李四', 'abc123'),
  ('王五', 'qwe456'),
  ('赵六', 'zxc789');
```

### 查询数据（最常用）

```sql
-- 查询所有数据
SELECT * FROM user;

-- 查询指定字段
SELECT id, name FROM user;

-- 条件查询（WHERE）
SELECT * FROM user WHERE id = 1;
SELECT * FROM user WHERE name = '张三';

-- 模糊查询（LIKE）
SELECT * FROM user WHERE name LIKE '张%';   -- 姓张的
SELECT * FROM user WHERE name LIKE '%三';   -- 以"三"结尾
SELECT * FROM user WHERE name LIKE '%三%';  -- 包含"三"

-- 范围查询
SELECT * FROM user WHERE id BETWEEN 1 AND 3;
SELECT * FROM user WHERE id IN (1, 3, 5);

-- 排序（ORDER BY）
SELECT * FROM user ORDER BY id ASC;   -- 升序（默认）
SELECT * FROM user ORDER BY id DESC;  -- 降序

-- 分页（LIMIT）
SELECT * FROM user LIMIT 10;           -- 前10条
SELECT * FROM user LIMIT 10 OFFSET 20; -- 跳过20条，取10条
SELECT * FROM user LIMIT 20, 10;       -- 同上（offset 在前，limit 在后）

-- 去重（DISTINCT）
SELECT DISTINCT name FROM user;

-- 聚合函数
SELECT COUNT(*) FROM user;             -- 总数
SELECT MAX(id) FROM user;              -- 最大值
SELECT MIN(id) FROM user;              -- 最小值
SELECT AVG(id) FROM user;              -- 平均值

-- 分组（GROUP BY）
SELECT name, COUNT(*) FROM user GROUP BY name;
```

### 更新数据（⚠️ 务必加 WHERE，否则全表更新）

```sql
-- 更新单条
UPDATE user SET name = '张三丰' WHERE id = 1;

-- 更新多个字段
UPDATE user SET name = '张三丰', email = 'zhang@example.com' WHERE id = 1;
```

### 删除数据（⚠️ 务必加 WHERE，否则全表删除）

```sql
-- 删除单条
DELETE FROM user WHERE id = 1;

-- 删除所有数据（逐行删除，可回滚，慢）
DELETE FROM user;

-- 清空表（直接删表再重建，不可回滚，快）
TRUNCATE TABLE user;
```

---

## 四、条件运算符

| 运算符 | 含义 | 示例 |
|--------|------|------|
| `=` | 等于 | `WHERE id = 1` |
| `!=` 或 `<>` | 不等于 | `WHERE id != 1` |
| `>` `<` `>=` `<=` | 比较 | `WHERE id > 10` |
| `BETWEEN ... AND ...` | 范围 | `WHERE id BETWEEN 1 AND 10` |
| `IN (...)` | 在列表中 | `WHERE id IN (1, 3, 5)` |
| `LIKE` | 模糊匹配 | `WHERE name LIKE '张%'` |
| `IS NULL` | 为空 | `WHERE email IS NULL` |
| `IS NOT NULL` | 不为空 | `WHERE email IS NOT NULL` |
| `AND` | 并且 | `WHERE id > 1 AND id < 10` |
| `OR` | 或者 | `WHERE id = 1 OR id = 2` |
| `NOT` | 取反 | `WHERE NOT (id = 1)` |

---

## 五、常用小技巧

### 起别名
```sql
SELECT id AS 用户ID, name AS 用户名 FROM user;
```

### 条件判断（CASE WHEN）
```sql
SELECT name,
  CASE
    WHEN id < 3 THEN '新用户'
    ELSE '老用户'
  END AS 用户类型
FROM user;
```

### 处理 NULL
```sql
SELECT IFNULL(email, '未填写') AS 邮箱 FROM user;
```

---

## 六、速记口诀

| 操作 | 关键字 | 记忆 |
|------|--------|------|
| 增 | `INSERT INTO` | 插入 |
| 删 | `DELETE FROM` | 删除行 |
| 改 | `UPDATE ... SET` | 更新 |
| 查 | `SELECT ... FROM` | 查询 |

> 💡 **记忆技巧**：英文直译 —— `INSERT` 插入、`DELETE` 删除、`UPDATE` 更新、`SELECT` 选择。

---

## 七、⚠️ 注意事项

1. **UPDATE 和 DELETE 必须加 WHERE**，否则影响全表
2. **字符串用单引号** `'张三'`，不是双引号
3. **MySQL 关键字大写**是约定（`SELECT`、`WHERE`），字段名小写
4. **每句以分号 `;` 结尾**
5. 生产环境操作前**先备份**
