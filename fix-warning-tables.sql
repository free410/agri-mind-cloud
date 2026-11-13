-- 创建 warning 表（如果不存在）
CREATE TABLE IF NOT EXISTS `warning` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `warning_no` VARCHAR(40) DEFAULT NULL COMMENT '预警编号',
    `warning_type` VARCHAR(50) NOT NULL COMMENT '预警类型',
    `content` VARCHAR(500) NOT NULL COMMENT '预警内容',
    `level` TINYINT NOT NULL COMMENT '预警等级�?低，2中，3�?,
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0待确认，1处理中，2已处理，3已闭�?,
    `farmland_id` BIGINT DEFAULT NULL COMMENT '所属农田ID',
    `related_device_id` BIGINT DEFAULT NULL COMMENT '关联设备ID',
    `related_crop_id` BIGINT DEFAULT NULL COMMENT '关联作物ID',
    `trigger_source` VARCHAR(32) DEFAULT 'RULE_ENGINE' COMMENT '触发来源',
    `trigger_time` DATETIME DEFAULT NULL COMMENT '触发时间',
    `ai_analysis_summary` VARCHAR(1000) DEFAULT NULL COMMENT 'AI分析摘要',
    `ai_suggestion_summary` VARCHAR(1000) DEFAULT NULL COMMENT 'AI建议摘要',
    `handler_name` VARCHAR(64) DEFAULT NULL COMMENT '处理�?,
    `handle_time` DATETIME DEFAULT NULL COMMENT '处理时间',
    `handle_remark` VARCHAR(1000) DEFAULT NULL COMMENT '处理说明',
    `close_time` DATETIME DEFAULT NULL COMMENT '闭环时间',
    `is_false_alarm` TINYINT NOT NULL DEFAULT 0 COMMENT '是否误报�?否，1�?,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_warning_no` (`warning_no`),
    KEY `idx_warning_level` (`level`),
    KEY `idx_warning_status` (`status`),
    KEY `idx_warning_farmland_id` (`farmland_id`),
    KEY `idx_warning_related_device_id` (`related_device_id`),
    KEY `idx_warning_related_crop_id` (`related_crop_id`),
    KEY `idx_warning_trigger_source` (`trigger_source`),
    KEY `idx_warning_trigger_time` (`trigger_time`),
    KEY `idx_warning_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预警主表（支持闭环流程）';

-- 预警处理日志�?
CREATE TABLE IF NOT EXISTS `warning_process_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `warning_id` BIGINT NOT NULL COMMENT '预警ID',
    `action_type` VARCHAR(32) NOT NULL COMMENT '动作类型',
    `status_from` TINYINT DEFAULT NULL COMMENT '变更前状�?,
    `status_to` TINYINT DEFAULT NULL COMMENT '变更后状�?,
    `operator_name` VARCHAR(64) DEFAULT NULL COMMENT '操作�?,
    `process_note` VARCHAR(1000) DEFAULT NULL COMMENT '处理说明',
    `process_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '处理时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_warning_process_log_warning_id` (`warning_id`),
    KEY `idx_warning_process_log_process_time` (`process_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预警处理日志�?;

-- 预警AI分析记录�?
CREATE TABLE IF NOT EXISTS `warning_ai_analysis` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `warning_id` BIGINT NOT NULL COMMENT '预警ID',
    `analysis_source` VARCHAR(32) NOT NULL DEFAULT 'MOCK' COMMENT '分析来源',
    `analysis_summary` VARCHAR(1000) DEFAULT NULL COMMENT 'AI分析摘要',
    `suggestion_summary` VARCHAR(1000) DEFAULT NULL COMMENT 'AI建议摘要',
    `risk_score` INT DEFAULT NULL COMMENT '风险评分�?-100�?,
    `priority_level` TINYINT DEFAULT NULL COMMENT '优先级（1低，2中，3高）',
    `raw_response` TEXT COMMENT '原始响应',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_warning_ai_analysis_warning_id` (`warning_id`),
    KEY `idx_warning_ai_analysis_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预警AI分析记录�?;

-- 初始化默认规则（如果不存在）
INSERT INTO `warning_rule_config`
(`rule_name`, `rule_code`, `warning_type`, `warning_level`, `target_scope_type`, `target_scope_id`,
 `metric_key`, `compare_operator`, `threshold_value`, `continuous_count`, `deduplicate_minutes`, `enabled`, `description`)
SELECT '全局温度过高', 'RULE_TEMP_HIGH_GLOBAL', 'TEMPERATURE_HIGH', 3, 'GLOBAL', NULL,
       'temperature', '>', 32, 2, 30, 1, '全局温度过高自动预警'
WHERE NOT EXISTS (SELECT 1 FROM `warning_rule_config` WHERE `rule_code` = 'RULE_TEMP_HIGH_GLOBAL');

INSERT INTO `warning_rule_config`
(`rule_name`, `rule_code`, `warning_type`, `warning_level`, `target_scope_type`, `target_scope_id`,
 `metric_key`, `compare_operator`, `threshold_value`, `continuous_count`, `deduplicate_minutes`, `enabled`, `description`)
SELECT '全局温度过低', 'RULE_TEMP_LOW_GLOBAL', 'TEMPERATURE_LOW', 2, 'GLOBAL', NULL,
       'temperature', '<', 10, 2, 30, 1, '全局温度过低自动预警'
WHERE NOT EXISTS (SELECT 1 FROM `warning_rule_config` WHERE `rule_code` = 'RULE_TEMP_LOW_GLOBAL');

INSERT INTO `warning_rule_config`
(`rule_name`, `rule_code`, `warning_type`, `warning_level`, `target_scope_type`, `target_scope_id`,
 `metric_key`, `compare_operator`, `threshold_value`, `continuous_count`, `deduplicate_minutes`, `enabled`, `description`)
SELECT '全局土壤湿度过低', 'RULE_SOIL_MOISTURE_LOW_GLOBAL', 'SOIL_MOISTURE_LOW', 3, 'GLOBAL', NULL,
       'soil_moisture', '<', 30, 2, 45, 1, '全局土壤湿度过低自动预警'
WHERE NOT EXISTS (SELECT 1 FROM `warning_rule_config` WHERE `rule_code` = 'RULE_SOIL_MOISTURE_LOW_GLOBAL');

INSERT INTO `warning_rule_config`
(`rule_name`, `rule_code`, `warning_type`, `warning_level`, `target_scope_type`, `target_scope_id`,
 `metric_key`, `compare_operator`, `threshold_value`, `continuous_count`, `deduplicate_minutes`, `enabled`, `description`)
SELECT '全局光照偏低', 'RULE_LIGHT_LOW_GLOBAL', 'LIGHT_ABNORMAL', 2, 'GLOBAL', NULL,
       'light', '<', 5000, 2, 30, 1, '全局光照偏低自动预警'
WHERE NOT EXISTS (SELECT 1 FROM `warning_rule_config` WHERE `rule_code` = 'RULE_LIGHT_LOW_GLOBAL');

INSERT INTO `warning_rule_config`
(`rule_name`, `rule_code`, `warning_type`, `warning_level`, `target_scope_type`, `target_scope_id`,
 `metric_key`, `compare_operator`, `threshold_value`, `continuous_count`, `deduplicate_minutes`, `enabled`, `description`)
SELECT '全局设备离线', 'RULE_DEVICE_OFFLINE_GLOBAL', 'DEVICE_OFFLINE', 2, 'GLOBAL', NULL,
       'device_offline', '>=', 1, 1, 20, 1, '全局设备离线自动预警'
WHERE NOT EXISTS (SELECT 1 FROM `warning_rule_config` WHERE `rule_code` = 'RULE_DEVICE_OFFLINE_GLOBAL');
