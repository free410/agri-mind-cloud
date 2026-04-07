SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `device` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `device_code` varchar(50) NOT NULL COMMENT '设备编号',
    `device_name` varchar(100) NOT NULL COMMENT '设备名称',
    `device_type` varchar(50) NOT NULL COMMENT '设备类型',
    `farmland_id` bigint NOT NULL COMMENT '所属农田ID',
    `install_time` datetime NOT NULL COMMENT '安装时间',
    `online_status` tinyint NOT NULL DEFAULT 1 COMMENT '在线状态：0离线，1在线，2故障',
    `run_status` tinyint NOT NULL DEFAULT 1 COMMENT '运行状态：0停用，1运行中，2待机中，3异常',
    `remark` varchar(255) DEFAULT NULL COMMENT '备注',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_device_code` (`device_code`),
    KEY `idx_device_type` (`device_type`),
    KEY `idx_device_farmland_id` (`farmland_id`),
    KEY `idx_device_online_status` (`online_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='设备表';

INSERT INTO `device`
(`id`, `device_code`, `device_name`, `device_type`, `farmland_id`, `install_time`, `online_status`, `run_status`, `remark`, `create_time`, `update_time`)
VALUES
(1, 'DEV-2026-001', '温湿度传感器-01', '温湿度传感器', 1, '2026-03-01 09:00:00', 1, 1, '用于温室环境模拟采集', NOW(), NOW()),
(2, 'DEV-2026-002', '土壤湿度传感器-01', '土壤传感器', 1, '2026-03-02 10:30:00', 1, 2, '用于土壤湿度趋势展示', NOW(), NOW()),
(3, 'DEV-2026-003', '智能灌溉控制器-01', '灌溉控制器', 2, '2026-03-05 14:20:00', 0, 0, '用于灌溉控制场景展示', NOW(), NOW()),
(4, 'DEV-2026-004', '大棚摄像头-01', '摄像头', 3, '2026-03-06 16:00:00', 2, 3, '用于设备故障和异常状态展示', NOW(), NOW())
ON DUPLICATE KEY UPDATE
`device_name` = VALUES(`device_name`),
`device_type` = VALUES(`device_type`),
`farmland_id` = VALUES(`farmland_id`),
`install_time` = VALUES(`install_time`),
`online_status` = VALUES(`online_status`),
`run_status` = VALUES(`run_status`),
`remark` = VALUES(`remark`),
`update_time` = NOW();
