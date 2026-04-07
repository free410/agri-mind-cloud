SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `farmland` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `field_name` varchar(100) NOT NULL COMMENT '农田名称',
    `area` decimal(10,2) NOT NULL COMMENT '农田面积',
    `location` varchar(255) NOT NULL COMMENT '地理位置',
    `soil_type` varchar(50) NOT NULL COMMENT '土壤类型',
    `status` tinyint NOT NULL DEFAULT 1 COMMENT '农田状态：0闲置，1种植中，2维护中',
    `remark` varchar(255) DEFAULT NULL COMMENT '备注',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_farmland_status` (`status`),
    KEY `idx_farmland_name` (`field_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='农田表';

INSERT INTO `farmland`
(`id`, `field_name`, `area`, `location`, `soil_type`, `status`, `remark`, `create_time`, `update_time`)
VALUES
(1, '一号示范田', 20.50, '山东省寿光市智慧农业园区A区', '壤土', 1, '适合比赛演示和环境监测展示', NOW(), NOW()),
(2, '二号露天农田', 35.00, '山东省寿光市智慧农业园区B区', '砂壤土', 0, '当前处于闲置待种植状态', NOW(), NOW()),
(3, '三号温室农田', 16.80, '山东省寿光市智慧农业园区C区', '腐殖土', 2, '正在进行设备维护和土壤改良', NOW(), NOW())
ON DUPLICATE KEY UPDATE
`field_name` = VALUES(`field_name`),
`area` = VALUES(`area`),
`location` = VALUES(`location`),
`soil_type` = VALUES(`soil_type`),
`status` = VALUES(`status`),
`remark` = VALUES(`remark`),
`update_time` = NOW();
