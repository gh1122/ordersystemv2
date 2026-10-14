-- ============================================
-- ordersystem 数据库初始化脚本
-- 幂等设计：可重复执行，不会报错
-- ============================================
-- 1. 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS ordersystem DEFAULT CHARSET utf8mb4;
USE ordersystem;
-- 2. 用户表
CREATE TABLE IF NOT EXISTS t_user (
  id INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
  password VARCHAR(100) NOT NULL COMMENT '密码（后续改为BCrypt加密后存储）',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户表';
-- ============================================
-- RBAC 种子数据
-- 幂等说明：依赖唯一键（uk_role_name / uk_user_role / uk_role_menu），
-- INSERT IGNORE 在数据已存在时自动跳过，可重复执行
-- ============================================
-- 1. 角色：管理员 + 普通用户（显式指定 id，保证下面外键引用稳定）
INSERT IGNORE INTO t_role (id, role_name, description)
VALUES (1, 'admin', '管理员，拥有全部权限'),
  (2, 'user', '普通用户');
-- 2. 菜单树，结构如下：
--   系统管理 (1, 目录)
--   ├── 用户管理 (2, 菜单)
--   │   ├── 新增用户 (3, 按钮)
--   │   └── 删除用户 (4, 按钮)
--   └── 角色管理 (5, 菜单)
--   订单管理 (6, 菜单，根节点)
INSERT IGNORE INTO t_menu (
    id,
    menu_name,
    path,
    icon,
    parent_id,
    sort,
    type,
    permission
  )
VALUES (
    1,
    '系统管理',
    '/system',
    'setting',
    0,
    1,
    0,
    ''
  ),
  (
    2,
    '用户管理',
    '/system/user',
    'user',
    1,
    1,
    1,
    'system:user:list'
  ),
  (
    3,
    '新增用户',
    '',
    '',
    2,
    1,
    2,
    'system:user:add'
  ),
  (
    4,
    '删除用户',
    '',
    '',
    2,
    2,
    2,
    'system:user:delete'
  ),
  (
    5,
    '角色管理',
    '/system/role',
    'role',
    1,
    2,
    1,
    'system:role:list'
  ),
  (
    6,
    '订单管理',
    '/order',
    'order',
    0,
    2,
    1,
    'order:list'
  );
-- 3. 角色-菜单关联：admin 拿全部，普通用户只拿订单管理
INSERT IGNORE INTO t_role_menu (role_id, menu_id)
VALUES (1, 1),
  (1, 2),
  (1, 3),
  (1, 4),
  (1, 5),
  (1, 6),
  (2, 6);
-- 4. 用户-角色：把第一个注册的用户设为管理员（没有用户时这句插不进任何行，安全）
INSERT IGNORE INTO t_user_role (user_id, role_id)
SELECT id,
  1
FROM t_user
ORDER BY id
LIMIT 1;
-- 5.商品表
CREATE TABLE IF NOT EXISTS t_product (
  id INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  name VARCHAR(100) NOT NULL COMMENT '商品名称',
  description TEXT COMMENT '商品描述',
  price DECIMAL(10, 2) NOT NULL COMMENT '商品价格',
  stock INT NOT NULL COMMENT '库存数量',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '商品表';
-- 6. 订单主表
CREATE TABLE IF NOT EXISTS t_order (
  id INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  order_no VARCHAR(50) NOT NULL UNIQUE COMMENT '订单编号',
  user_id INT NOT NULL COMMENT '用户ID',
  total_amount DECIMAL(10, 2) NOT NULL COMMENT '订单总金额',
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '订单状态（PENDING, PAID, SHIPPED, COMPLETED, CANCELED）',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  -- 新增普通索引：用户查自己订单列表高频查询
  INDEX idx_user_id (user_id) -- FOREIGN KEY (user_id) REFERENCES t_user(id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '订单主表';
-- 7. 订单明细表
CREATE TABLE IF NOT EXISTS t_order_item (
  id INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  order_id INT NOT NULL COMMENT '订单ID',
  product_id INT NOT NULL COMMENT '商品ID',
  quantity INT NOT NULL COMMENT '商品数量',
  price DECIMAL(10, 2) NOT NULL COMMENT '商品单价',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  product_name VARCHAR(100) NOT NULL COMMENT '商品名称',
  -- 新增普通索引：查询订单详情，按order_id查明细
  INDEX idx_order_id (order_id) -- FOREIGN KEY (order_id) REFERENCES t_order(id),
  -- FOREIGN KEY (product_id) REFERENCES t_product(id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '订单明细表';