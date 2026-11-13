SET NAMES utf8mb4;

-- 智能预警闭环中心迁移脚本（可重复执行）
-- 说明：
-- 1) 补齐 warning 闭环字段
-- 2) 创建 warning_process_log / warning_ai_analysis
-- 3) 创建 warning_rule_config 规则中心表
-- 4) 初始化默认规则

-- 1. warning 主表补字段
-- 注意：MySQL 8 不支持 MariaDB 的 ADD COLUMN IF NOT EXISTS，改用 information_schema 判断后动态执行
SET @db_name = DATABASE();

SET @sql = IF(
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = @db_name AND table_name = 'warning' AND column_name = 'warning_no'),
    'SELECT 1',
    'ALTER TABLE `warning` ADD COLUMN `warning_no` VARCHAR(40) DEFAULT NULL COMMENT ''预警编号'' AFTER `id`'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = @db_name AND table_name = 'warning' AND column_name = 'related_crop_id'),
    'SELECT 1',
    'ALTER TABLE `warning` ADD COLUMN `related_crop_id` BIGINT DEFAULT NULL COMMENT ''关联作物ID'' AFTER `related_device_id`'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = @db_name AND table_name = 'warning' AND column_name = 'trigger_source'),
    'SELECT 1',
    'ALTER TABLE `warning` ADD COLUMN `trigger_source` VARCHAR(32) DEFAULT ''RULE_ENGINE'' COMMENT ''触发来源'' AFTER `related_crop_id`'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = @db_name AND table_name = 'warning' AND column_name = 'trigger_time'),
    'SELECT 1',
    'ALTER TABLE `warning` ADD COLUMN `trigger_time` DATETIME DEFAULT NULL COMMENT ''触发时间'' AFTER `trigger_source`'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = @db_name AND table_name = 'warning' AND column_name = 'ai_analysis_summary'),
    'SELECT 1',
    'ALTER TABLE `warning` ADD COLUMN `ai_analysis_summary` VARCHAR(1000) DEFAULT NULL COMMENT ''AI分析摘要'' AFTER `trigger_time`'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = @db_name AND table_name = 'warning' AND column_name = 'ai_suggestion_summary'),
    'SELECT 1',
    'ALTER TABLE `warning` ADD COLUMN `ai_suggestion_summary` VARCHAR(1000) DEFAULT NULL COMMENT ''AI建议摘要'' AFTER `ai_analysis_summary`'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = @db_name AND table_name = 'warning' AND column_name = 'handler_name'),
    'SELECT 1',
    'ALTER TABLE `warning` ADD COLUMN `handler_name` VARCHAR(64) DEFAULT NULL COMMENT ''处理人'' AFTER `ai_suggestion_summary`'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = @db_name AND table_name = 'warning' AND column_name = 'handle_time'),
    'SELECT 1',
    'ALTER TABLE `warning` ADD COLUMN `handle_time` DATETIME DEFAULT NULL COMMENT ''处理时间'' AFTER `handler_name`'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = @db_name AND table_name = 'warning' AND column_name = 'handle_remark'),
    'SELECT 1',
    'ALTER TABLE `warning` ADD COLUMN `handle_remark` VARCHAR(1000) DEFAULT NULL COMMENT ''处理说明'' AFTER `handle_time`'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = @db_name AND table_name = 'warning' AND column_name = 'close_time'),
    'SELECT 1',
    'ALTER TABLE `warning` ADD COLUMN `close_time` DATETIME DEFAULT NULL COMMENT ''闭环时间'' AFTER `handle_remark`'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = @db_name AND table_name = 'warning' AND column_name = 'is_false_alarm'),
    'SELECT 1',
    'ALTER TABLE `warning` ADD COLUMN `is_false_alarm` TINYINT NOT NULL DEFAULT 0 COMMENT ''是否误报'' AFTER `close_time`'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

ALTER TABLE `warning`
    MODIFY COLUMN `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0待确认，1处理中，2已处理，3已闭环';

-- 2. 历史数据回填
UPDATE `warning`
SET `warning_no` = CONCAT('WARN', DATE_FORMAT(COALESCE(`create_time`, NOW()), '%Y%m%d'), LPAD(`id`, 6, '0'))
WHERE `warning_no` IS NULL OR `warning_no` = '';

UPDATE `warning`
SET `trigger_source` = CASE
    WHEN `trigger_source` IS NOT NULL AND `trigger_source` <> '' THEN `trigger_source`
    WHEN `warning_type` LIKE 'DEVICE_%' THEN 'DEVICE_STATUS'
    WHEN `warning_type` LIKE 'MANUAL_%' THEN 'MANUAL_INPUT'
    ELSE 'ENV_MONITOR'
END
WHERE `trigger_source` IS NULL OR `trigger_source` = '';

UPDATE `warning`
SET `trigger_time` = COALESCE(`trigger_time`, `create_time`, NOW())
WHERE `trigger_time` IS NULL;

-- 3. 索引补齐
SET @db_name = DATABASE();

SET @sql = IF(
    EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = @db_name AND table_name = 'warning' AND index_name = 'uk_warning_no'),
    'SELECT 1',
    'ALTER TABLE `warning` ADD UNIQUE KEY `uk_warning_no` (`warning_no`)'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = @db_name AND table_name = 'warning' AND index_name = 'idx_warning_related_device_id'),
    'SELECT 1',
    'ALTER TABLE `warning` ADD KEY `idx_warning_related_device_id` (`related_device_id`)'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = @db_name AND table_name = 'warning' AND index_name = 'idx_warning_related_crop_id'),
    'SELECT 1',
    'ALTER TABLE `warning` ADD KEY `idx_warning_related_crop_id` (`related_crop_id`)'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = @db_name AND table_name = 'warning' AND index_name = 'idx_warning_trigger_source'),
    'SELECT 1',
    'ALTER TABLE `warning` ADD KEY `idx_warning_trigger_source` (`trigger_source`)'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = @db_name AND table_name = 'warning' AND index_name = 'idx_warning_trigger_time'),
    'SELECT 1',
    'ALTER TABLE `warning` ADD KEY `idx_warning_trigger_time` (`trigger_time`)'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4. 闭环日志与 AI 分析表
CREATE TABLE IF NOT EXISTS `warning_process_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `warning_id` BIGINT NOT NULL COMMENT '预警ID',
    `action_type` VARCHAR(32) NOT NULL COMMENT '动作类型',
    `status_from` TINYINT DEFAULT NULL COMMENT '变更前状态',
    `status_to` TINYINT DEFAULT NULL COMMENT '变更后状态',
    `operator_name` VARCHAR(64) DEFAULT NULL COMMENT '操作人',
    `process_note` VARCHAR(1000) DEFAULT NULL COMMENT '处理说明',
    `process_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '处理时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_warning_process_log_warning_id` (`warning_id`),
    KEY `idx_warning_process_log_process_time` (`process_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预警处理日志表';

CREATE TABLE IF NOT EXISTS `warning_ai_analysis` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `warning_id` BIGINT NOT NULL COMMENT '预警ID',
    `analysis_source` VARCHAR(32) NOT NULL DEFAULT 'MOCK' COMMENT '分析来源',
    `analysis_summary` VARCHAR(1000) DEFAULT NULL COMMENT 'AI分析摘要',
    `suggestion_summary` VARCHAR(1000) DEFAULT NULL COMMENT 'AI建议摘要',
    `risk_score` INT DEFAULT NULL COMMENT '风险评分',
    `priority_level` TINYINT DEFAULT NULL COMMENT '优先级',
    `raw_response` TEXT COMMENT '原始响应',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_warning_ai_analysis_warning_id` (`warning_id`),
    KEY `idx_warning_ai_analysis_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预警AI分析记录表';

-- 5. 规则中心表
CREATE TABLE IF NOT EXISTS `warning_rule_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `rule_name` VARCHAR(100) NOT NULL COMMENT '规则名称',
    `rule_code` VARCHAR(64) NOT NULL COMMENT '规则编码',
    `warning_type` VARCHAR(50) NOT NULL COMMENT '预警类型',
    `warning_level` TINYINT NOT NULL COMMENT '预警等级',
    `target_scope_type` VARCHAR(16) NOT NULL COMMENT '作用域：GLOBAL/FARMLAND/CROP',
    `target_scope_id` BIGINT DEFAULT NULL COMMENT '作用范围ID',
    `metric_key` VARCHAR(32) NOT NULL COMMENT '指标键',
    `compare_operator` VARCHAR(4) NOT NULL COMMENT '比较符',
    `threshold_value` DECIMAL(10,2) NOT NULL COMMENT '阈值',
    `continuous_count` INT NOT NULL DEFAULT 1 COMMENT '连续触发次数',
    `deduplicate_minutes` INT NOT NULL DEFAULT 30 COMMENT '重复预警抑制分钟',
    `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '规则说明',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_warning_rule_code` (`rule_code`),
    KEY `idx_warning_rule_enabled_metric` (`enabled`, `metric_key`),
    KEY `idx_warning_rule_scope` (`target_scope_type`, `target_scope_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预警规则配置表';

-- 6. 巡检任务日志表
CREATE TABLE IF NOT EXISTS `inspection_task_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `inspection_time` DATETIME NOT NULL COMMENT '巡检时间',
    `inspect_env_count` INT NOT NULL DEFAULT 0 COMMENT '环境数据条数',
    `inspect_device_count` INT NOT NULL DEFAULT 0 COMMENT '设备巡检条数',
    `detect_data_count` INT NOT NULL DEFAULT 0 COMMENT '检测总数据条数',
    `triggered_warning_count` INT NOT NULL DEFAULT 0 COMMENT '触发预警条数',
    `status` VARCHAR(20) NOT NULL COMMENT '巡检状态：SUCCESS/FAILED/SKIPPED',
    `summary` VARCHAR(500) DEFAULT NULL COMMENT '巡检摘要',
    `error_message` VARCHAR(1000) DEFAULT NULL COMMENT '错误信息',
    `trigger_mode` VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED' COMMENT '触发方式：SCHEDULED/MANUAL',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_inspection_log_time` (`inspection_time`),
    KEY `idx_inspection_log_status` (`status`),
    KEY `idx_inspection_log_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='定时巡检日志表';

-- 7. 默认规则初始化（不存在则插入）
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
