SET NAMES utf8mb4;

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

