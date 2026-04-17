//mysql创建语句

-- 创建数据库
CREATE DATABASE IF NOT EXISTS korokingdom DEFAULT CHARACTER SET utf8mb4;

USE korokingdom;

-- 用户表
CREATE TABLE t_user (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
                        username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
                        email VARCHAR(100) NOT NULL UNIQUE COMMENT '邮箱',
                        phone VARCHAR(20) COMMENT '手机号',
                        password_hash VARCHAR(255) NOT NULL COMMENT '加密后的密码',
                        nickname VARCHAR(50) COMMENT '昵称',
                        avatar_url VARCHAR(500) COMMENT '头像URL',
                        created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                        updated_at DATETIME COMMENT '更新时间'
);