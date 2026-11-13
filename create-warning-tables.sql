SET NAMES utf8mb4;
CREATE TABLE IF NOT EXISTS warning (
    id BIGINT NOT NULL AUTO_INCREMENT,
    warning_no VARCHAR(40) DEFAULT NULL,
    warning_type VARCHAR(50) NOT NULL,
    content VARCHAR(500) NOT NULL,
    level TINYINT NOT NULL,
    status TINYINT NOT NULL DEFAULT 0,
    farmland_id BIGINT DEFAULT NULL,
    related_device_id BIGINT DEFAULT NULL,
    related_crop_id BIGINT DEFAULT NULL,
    trigger_source VARCHAR(32) DEFAULT 'RULE_ENGINE',
    trigger_time DATETIME DEFAULT NULL,
    ai_analysis_summary VARCHAR(1000) DEFAULT NULL,
    ai_suggestion_summary VARCHAR(1000) DEFAULT NULL,
    handler_name VARCHAR(64) DEFAULT NULL,
    handle_time DATETIME DEFAULT NULL,
    handle_remark VARCHAR(1000) DEFAULT NULL,
    close_time DATETIME DEFAULT NULL,
    is_false_alarm TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_warning_level (level),
    KEY idx_warning_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS warning_process_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    warning_id BIGINT NOT NULL,
    action_type VARCHAR(32) NOT NULL,
    status_from TINYINT DEFAULT NULL,
    status_to TINYINT DEFAULT NULL,
    operator_name VARCHAR(64) DEFAULT NULL,
    process_note VARCHAR(1000) DEFAULT NULL,
    process_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_warning_process_log_warning_id (warning_id),
    KEY idx_warning_process_log_process_time (process_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS warning_ai_analysis (
    id BIGINT NOT NULL AUTO_INCREMENT,
    warning_id BIGINT NOT NULL,
    analysis_source VARCHAR(32) NOT NULL DEFAULT 'MOCK',
    analysis_summary VARCHAR(1000) DEFAULT NULL,
    suggestion_summary VARCHAR(1000) DEFAULT NULL,
    risk_score INT DEFAULT NULL,
    priority_level TINYINT DEFAULT NULL,
    raw_response TEXT,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_warning_ai_analysis_warning_id (warning_id),
    KEY idx_warning_ai_analysis_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
