SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `warning_rule_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `rule_name` VARCHAR(100) NOT NULL COMMENT '规则名称',
    `rule_code` VARCHAR(64) NOT NULL COMMENT '规则编码',
    `warning_type` VARCHAR(50) NOT NULL COMMENT '预警类型',
    `warning_level` TINYINT NOT NULL COMMENT '预警等级（1低，2中，3高）',
    `target_scope_type` VARCHAR(16) NOT NULL COMMENT '作用域：GLOBAL/FARMLAND/CROP',
    `target_scope_id` BIGINT DEFAULT NULL COMMENT '作用范围ID',
    `metric_key` VARCHAR(32) NOT NULL COMMENT '指标键',
    `compare_operator` VARCHAR(4) NOT NULL COMMENT '比较符',
    `threshold_value` DECIMAL(10,2) NOT NULL COMMENT '阈值',
    `continuous_count` INT NOT NULL DEFAULT 1 COMMENT '连续触发次数',
    `deduplicate_minutes` INT NOT NULL DEFAULT 30 COMMENT '重复预警抑制分钟',
    `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用（0否，1是）',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '规则说明',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_warning_rule_code` (`rule_code`),
    KEY `idx_warning_rule_enabled_metric` (`enabled`, `metric_key`),
    KEY `idx_warning_rule_scope` (`target_scope_type`, `target_scope_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预警规则配置表';

INSERT INTO `warning_rule_config`
(`rule_name`, `rule_code`, `warning_type`, `warning_level`, `target_scope_type`, `target_scope_id`,
 `metric_key`, `compare_operator`, `threshold_value`, `continuous_count`, `deduplicate_minutes`, `enabled`, `description`)
VALUES
('全局温度过高', 'RULE_TEMP_HIGH_GLOBAL', 'TEMPERATURE_HIGH', 3, 'GLOBAL', NULL, 'temperature', '>', 32, 2, 30, 1, '全局温度过高自动预警'),
('全局温度过低', 'RULE_TEMP_LOW_GLOBAL', 'TEMPERATURE_LOW', 2, 'GLOBAL', NULL, 'temperature', '<', 10, 2, 30, 1, '全局温度过低自动预警'),
('全局土壤湿度过低', 'RULE_SOIL_MOISTURE_LOW_GLOBAL', 'SOIL_MOISTURE_LOW', 3, 'GLOBAL', NULL, 'soil_moisture', '<', 30, 2, 45, 1, '全局土壤湿度过低自动预警'),
('全局光照偏低', 'RULE_LIGHT_LOW_GLOBAL', 'LIGHT_ABNORMAL', 2, 'GLOBAL', NULL, 'light', '<', 5000, 2, 30, 1, '全局光照偏低自动预警'),
('全局设备离线', 'RULE_DEVICE_OFFLINE_GLOBAL', 'DEVICE_OFFLINE', 2, 'GLOBAL', NULL, 'device_offline', '>=', 1, 1, 20, 1, '全局设备离线自动预警')
ON DUPLICATE KEY UPDATE
`rule_name` = VALUES(`rule_name`),
`warning_type` = VALUES(`warning_type`),
`warning_level` = VALUES(`warning_level`),
`target_scope_type` = VALUES(`target_scope_type`),
`target_scope_id` = VALUES(`target_scope_id`),
`metric_key` = VALUES(`metric_key`),
`compare_operator` = VALUES(`compare_operator`),
`threshold_value` = VALUES(`threshold_value`),
`continuous_count` = VALUES(`continuous_count`),
`deduplicate_minutes` = VALUES(`deduplicate_minutes`),
`enabled` = VALUES(`enabled`),
`description` = VALUES(`description`),
`update_time` = NOW();

