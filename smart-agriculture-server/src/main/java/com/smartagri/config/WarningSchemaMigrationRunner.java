package com.smartagri.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 预警相关数据库兼容迁移：
 * 1. 闭环字段与日志子表
 * 2. 规则中心表与默认规则初始化
 */
@Slf4j
@Component
public class WarningSchemaMigrationRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Value("${smart-agri.warning.auto-migrate-closure-schema:true}")
    private boolean autoMigrateClosureSchema;

    public WarningSchemaMigrationRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!autoMigrateClosureSchema) {
            return;
        }

        try {
            migrateRuleCenterTable();
            migrateInspectionTaskLogTable();
            initDefaultRules();

            if (!tableExists("warning")) {
                log.info("未检测到 warning 表，跳过闭环字段迁移，仅完成规则中心表初始化。");
                return;
            }

            migrateWarningMainTable();
            backfillWarningData();
            migrateWarningSubTables();
            initProcessLogForHistoryWarnings();
            log.info("预警闭环与规则中心自动迁移完成。");
        } catch (Exception ex) {
            log.error("预警自动迁移失败，请检查数据库权限与表结构。", ex);
        }
    }

    private void migrateWarningMainTable() {
        addColumnIfMissing("warning", "warning_no",
                "ALTER TABLE `warning` ADD COLUMN `warning_no` VARCHAR(40) DEFAULT NULL COMMENT '预警编号' AFTER `id`");
        addColumnIfMissing("warning", "related_crop_id",
                "ALTER TABLE `warning` ADD COLUMN `related_crop_id` BIGINT DEFAULT NULL COMMENT '关联作物ID' AFTER `related_device_id`");
        addColumnIfMissing("warning", "trigger_source",
                "ALTER TABLE `warning` ADD COLUMN `trigger_source` VARCHAR(32) DEFAULT 'RULE_ENGINE' COMMENT '触发来源' AFTER `related_crop_id`");
        addColumnIfMissing("warning", "trigger_time",
                "ALTER TABLE `warning` ADD COLUMN `trigger_time` DATETIME DEFAULT NULL COMMENT '触发时间' AFTER `trigger_source`");
        addColumnIfMissing("warning", "ai_analysis_summary",
                "ALTER TABLE `warning` ADD COLUMN `ai_analysis_summary` VARCHAR(1000) DEFAULT NULL COMMENT 'AI分析摘要' AFTER `trigger_time`");
        addColumnIfMissing("warning", "ai_suggestion_summary",
                "ALTER TABLE `warning` ADD COLUMN `ai_suggestion_summary` VARCHAR(1000) DEFAULT NULL COMMENT 'AI建议摘要' AFTER `ai_analysis_summary`");
        addColumnIfMissing("warning", "handler_name",
                "ALTER TABLE `warning` ADD COLUMN `handler_name` VARCHAR(64) DEFAULT NULL COMMENT '处理人' AFTER `ai_suggestion_summary`");
        addColumnIfMissing("warning", "handle_time",
                "ALTER TABLE `warning` ADD COLUMN `handle_time` DATETIME DEFAULT NULL COMMENT '处理时间' AFTER `handler_name`");
        addColumnIfMissing("warning", "handle_remark",
                "ALTER TABLE `warning` ADD COLUMN `handle_remark` VARCHAR(1000) DEFAULT NULL COMMENT '处理说明' AFTER `handle_time`");
        addColumnIfMissing("warning", "close_time",
                "ALTER TABLE `warning` ADD COLUMN `close_time` DATETIME DEFAULT NULL COMMENT '闭环时间' AFTER `handle_remark`");
        addColumnIfMissing("warning", "is_false_alarm",
                "ALTER TABLE `warning` ADD COLUMN `is_false_alarm` TINYINT NOT NULL DEFAULT 0 COMMENT '是否误报' AFTER `close_time`");

        jdbcTemplate.execute("ALTER TABLE `warning` MODIFY COLUMN `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0待确认，1处理中，2已处理，3已闭环'");
    }

    private void backfillWarningData() {
        jdbcTemplate.update("""
                UPDATE `warning`
                SET `warning_no` = CONCAT('WARN', DATE_FORMAT(COALESCE(`create_time`, NOW()), '%Y%m%d'), LPAD(`id`, 6, '0'))
                WHERE `warning_no` IS NULL OR `warning_no` = ''
                """);

        jdbcTemplate.update("""
                UPDATE `warning`
                SET `trigger_source` = CASE
                    WHEN `trigger_source` IS NOT NULL AND `trigger_source` <> '' THEN `trigger_source`
                    WHEN `warning_type` LIKE 'DEVICE_%' THEN 'DEVICE_STATUS'
                    WHEN `warning_type` LIKE 'MANUAL_%' THEN 'MANUAL_INPUT'
                    ELSE 'ENV_MONITOR'
                END
                WHERE `trigger_source` IS NULL OR `trigger_source` = ''
                """);

        jdbcTemplate.update("""
                UPDATE `warning`
                SET `trigger_time` = COALESCE(`trigger_time`, `create_time`, NOW())
                WHERE `trigger_time` IS NULL
                """);

        jdbcTemplate.update("""
                UPDATE `warning`
                SET `status` = CASE
                    WHEN `status` IS NULL THEN 0
                    WHEN `status` <= 1 THEN `status`
                    WHEN `status` = 2 THEN 2
                    ELSE 3
                END
                """);

        createIndexIfMissing("warning", "uk_warning_no",
                "ALTER TABLE `warning` ADD UNIQUE KEY `uk_warning_no` (`warning_no`)");
        createIndexIfMissing("warning", "idx_warning_related_device_id",
                "ALTER TABLE `warning` ADD KEY `idx_warning_related_device_id` (`related_device_id`)");
        createIndexIfMissing("warning", "idx_warning_related_crop_id",
                "ALTER TABLE `warning` ADD KEY `idx_warning_related_crop_id` (`related_crop_id`)");
        createIndexIfMissing("warning", "idx_warning_trigger_source",
                "ALTER TABLE `warning` ADD KEY `idx_warning_trigger_source` (`trigger_source`)");
        createIndexIfMissing("warning", "idx_warning_trigger_time",
                "ALTER TABLE `warning` ADD KEY `idx_warning_trigger_time` (`trigger_time`)");
        createIndexIfMissing("warning", "idx_warning_create_time",
                "ALTER TABLE `warning` ADD KEY `idx_warning_create_time` (`create_time`)");
    }

    private void migrateWarningSubTables() {
        jdbcTemplate.execute("""
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
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预警处理日志表'
                """);

        jdbcTemplate.execute("""
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
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预警AI分析记录表'
                """);
    }

    private void initProcessLogForHistoryWarnings() {
        jdbcTemplate.update("""
                INSERT INTO `warning_process_log`
                (`warning_id`, `action_type`, `status_from`, `status_to`, `operator_name`, `process_note`, `process_time`)
                SELECT w.`id`,
                       'INIT_IMPORT',
                       NULL,
                       w.`status`,
                       '系统迁移脚本',
                       '历史预警数据初始化',
                       COALESCE(w.`trigger_time`, w.`create_time`, NOW())
                FROM `warning` w
                WHERE NOT EXISTS (
                    SELECT 1 FROM `warning_process_log` p WHERE p.`warning_id` = w.`id`
                )
                """);
    }

    private void migrateRuleCenterTable() {
        jdbcTemplate.execute("""
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
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预警规则配置表'
                """);
    }

    private void migrateInspectionTaskLogTable() {
        jdbcTemplate.execute("""
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
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='定时巡检日志表'
                """);
    }

    private void initDefaultRules() {
        insertDefaultRule(
                "全局温度过高",
                "RULE_TEMP_HIGH_GLOBAL",
                "TEMPERATURE_HIGH",
                3,
                "GLOBAL",
                null,
                "temperature",
                ">",
                new BigDecimal("32"),
                2,
                30,
                "全局温度过高自动预警");
        insertDefaultRule(
                "全局温度过低",
                "RULE_TEMP_LOW_GLOBAL",
                "TEMPERATURE_LOW",
                2,
                "GLOBAL",
                null,
                "temperature",
                "<",
                new BigDecimal("10"),
                2,
                30,
                "全局温度过低自动预警");
        insertDefaultRule(
                "全局土壤湿度过低",
                "RULE_SOIL_MOISTURE_LOW_GLOBAL",
                "SOIL_MOISTURE_LOW",
                3,
                "GLOBAL",
                null,
                "soil_moisture",
                "<",
                new BigDecimal("30"),
                2,
                45,
                "全局土壤湿度过低自动预警");
        insertDefaultRule(
                "全局光照偏低",
                "RULE_LIGHT_LOW_GLOBAL",
                "LIGHT_ABNORMAL",
                2,
                "GLOBAL",
                null,
                "light",
                "<",
                new BigDecimal("5000"),
                2,
                30,
                "全局光照偏低自动预警");
        insertDefaultRule(
                "全局设备离线",
                "RULE_DEVICE_OFFLINE_GLOBAL",
                "DEVICE_OFFLINE",
                2,
                "GLOBAL",
                null,
                "device_offline",
                ">=",
                new BigDecimal("1"),
                1,
                20,
                "全局设备离线自动预警");
    }

    private void insertDefaultRule(String ruleName,
                                   String ruleCode,
                                   String warningType,
                                   Integer warningLevel,
                                   String targetScopeType,
                                   Long targetScopeId,
                                   String metricKey,
                                   String compareOperator,
                                   BigDecimal thresholdValue,
                                   Integer continuousCount,
                                   Integer deduplicateMinutes,
                                   String description) {
        jdbcTemplate.update("""
                INSERT INTO `warning_rule_config`
                (`rule_name`, `rule_code`, `warning_type`, `warning_level`, `target_scope_type`, `target_scope_id`,
                 `metric_key`, `compare_operator`, `threshold_value`, `continuous_count`, `deduplicate_minutes`, `enabled`, `description`)
                SELECT ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?
                WHERE NOT EXISTS (SELECT 1 FROM `warning_rule_config` WHERE `rule_code` = ?)
                """,
                ruleName, ruleCode, warningType, warningLevel, targetScopeType, targetScopeId,
                metricKey, compareOperator, thresholdValue, continuousCount, deduplicateMinutes, description, ruleCode);
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(1)
                FROM information_schema.tables
                WHERE table_schema = DATABASE() AND table_name = ?
                """, Integer.class, tableName);
        return count != null && count > 0;
    }

    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(1)
                FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?
                """, Integer.class, tableName, columnName);
        return count != null && count > 0;
    }

    private boolean indexExists(String tableName, String indexName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(1)
                FROM information_schema.statistics
                WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ?
                """, Integer.class, tableName, indexName);
        return count != null && count > 0;
    }

    private void addColumnIfMissing(String tableName, String columnName, String ddl) {
        if (!columnExists(tableName, columnName)) {
            jdbcTemplate.execute(ddl);
        }
    }

    private void createIndexIfMissing(String tableName, String indexName, String ddl) {
        if (!indexExists(tableName, indexName)) {
            jdbcTemplate.execute(ddl);
        }
    }
}
