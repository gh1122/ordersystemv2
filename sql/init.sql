-- ============================================
-- ordersystem 数据库初始化脚本
-- 幂等设计：可重复执行，不会报错
-- ============================================

-- 1. 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS ordersystem DEFAULT CHARSET utf8mb4;
USE ordersystem;

-- 2. 用户表
CREATE TABLE IF NOT EXISTS t_user (
  id           INT AUTO_INCREMENT PRIMARY KEY     COMMENT '主键ID',
  username     VARCHAR(50)  NOT NULL UNIQUE       COMMENT '用户名',
  password     VARCHAR(100) NOT NULL              COMMENT '密码（后续改为BCrypt加密后存储）',
  create_time  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
