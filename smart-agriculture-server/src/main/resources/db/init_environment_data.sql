SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `env_data` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `farmland_id` bigint NOT NULL COMMENT '农田ID',
    `device_id` bigint NOT NULL COMMENT '设备ID',
    `temperature` decimal(5,2) NOT NULL COMMENT '温度',
    `humidity` decimal(5,2) NOT NULL COMMENT '湿度',
    `soil_moisture` decimal(5,2) NOT NULL COMMENT '土壤湿度',
    `light_intensity` decimal(10,2) NOT NULL COMMENT '光照强度',
    `ph_value` decimal(4,2) NOT NULL COMMENT 'PH值',
    `collect_time` datetime NOT NULL COMMENT '采集时间',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_env_farmland_collect_time` (`farmland_id`, `collect_time`),
    KEY `idx_env_device_collect_time` (`device_id`, `collect_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='环境监测数据表';

INSERT INTO `env_data`
(`id`, `farmland_id`, `device_id`, `temperature`, `humidity`, `soil_moisture`, `light_intensity`, `ph_value`, `collect_time`, `create_time`)
VALUES
(1, 1, 1, 23.50, 71.00, 38.50, 18000.00, 6.40, '2026-03-31 08:00:00', NOW()),
(2, 1, 1, 24.80, 69.50, 37.80, 20500.00, 6.45, '2026-03-31 10:00:00', NOW()),
(3, 1, 2, 26.20, 66.00, 35.10, 23800.00, 6.50, '2026-03-31 12:00:00', NOW()),
(4, 1, 2, 27.50, 63.20, 33.40, 26000.00, 6.55, '2026-03-31 14:00:00', NOW()),
(5, 2, 3, 22.60, 74.00, 41.20, 15000.00, 6.70, '2026-03-31 09:00:00', NOW()),
(6, 3, 4, 28.90, 58.00, 29.60, 28500.00, 6.10, '2026-03-31 11:30:00', NOW())
ON DUPLICATE KEY UPDATE
`temperature` = VALUES(`temperature`),
`humidity` = VALUES(`humidity`),
`soil_moisture` = VALUES(`soil_moisture`),
`light_intensity` = VALUES(`light_intensity`),
`ph_value` = VALUES(`ph_value`),
`collect_time` = VALUES(`collect_time`);
