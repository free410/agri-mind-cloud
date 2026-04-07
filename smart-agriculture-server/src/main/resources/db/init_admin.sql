SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `admin` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `username` varchar(50) NOT NULL COMMENT '登录账号',
    `password` varchar(255) NOT NULL COMMENT 'BCrypt加密密码',
    `nickname` varchar(50) DEFAULT NULL COMMENT '管理员昵称',
    `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
    `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
    `avatar` varchar(255) DEFAULT NULL COMMENT '头像地址',
    `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1正常，0禁用',
    `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
    `remark` varchar(255) DEFAULT NULL COMMENT '备注',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_admin_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员表';

INSERT INTO `admin`
(`id`, `username`, `password`, `nickname`, `phone`, `email`, `avatar`, `status`, `last_login_time`, `remark`, `create_time`, `update_time`)
VALUES
(1, 'admin', '$2a$10$m3Cd5cuqRgLVVp0c6PxVrO/sDIvhWQpqvlQ9jAuh09MWyGgYs/Bom', '系统管理员', '13800000000', 'admin@smartagri.local', NULL, 1, NULL, '比赛项目初始化管理员账号', NOW(), NOW())
ON DUPLICATE KEY UPDATE
`password` = VALUES(`password`),
`nickname` = VALUES(`nickname`),
`phone` = VALUES(`phone`),
`email` = VALUES(`email`),
`status` = VALUES(`status`),
`remark` = VALUES(`remark`),
`update_time` = NOW();
