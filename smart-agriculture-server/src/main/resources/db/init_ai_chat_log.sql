DROP TABLE IF EXISTS `ai_chat_log`;

CREATE TABLE `ai_chat_log` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `admin_id` bigint NOT NULL COMMENT '管理员ID',
    `session_id` varchar(64) NOT NULL COMMENT '会话ID',
    `question` varchar(1000) NOT NULL COMMENT '问题内容',
    `answer` text COMMENT 'AI回答内容',
    `related_module` varchar(50) DEFAULT NULL COMMENT '关联模块',
    `related_record_id` bigint DEFAULT NULL COMMENT '关联记录ID',
    `model_name` varchar(100) DEFAULT NULL COMMENT '模型名称',
    `context_snapshot` text COMMENT '上下文快照',
    `response_status` tinyint NOT NULL DEFAULT 1 COMMENT '响应状态：1成功，0失败',
    `response_time_ms` int DEFAULT NULL COMMENT '响应耗时（毫秒）',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_ai_admin_time` (`admin_id`, `create_time`),
    KEY `idx_ai_session` (`session_id`),
    KEY `idx_ai_related` (`related_module`, `related_record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI对话记录表';

INSERT INTO `ai_chat_log`
(`admin_id`, `session_id`, `question`, `answer`, `related_module`, `related_record_id`, `model_name`, `context_snapshot`, `response_status`, `response_time_ms`)
VALUES
(1, 'session-demo-001', '请分析当前农田环境是否适合作物生长？', '分析结论：当前农田温度偏高、土壤湿度偏低，建议及时滴灌并加强通风降温。', 'ENVIRONMENT', 1, 'mock-smart-agri-v1', '{"fieldName":"一号示范田","temperature":33.6,"soilMoisture":21.4}', 1, 428),
(1, 'session-demo-002', '这条高温预警意味着什么？', '原因解释：当前温室温度超过草莓适宜区间，若持续时间过长，可能影响果实品质与植株蒸腾平衡。', 'WARNING', 3, 'mock-smart-agri-v1', '{"warningType":"高温预警","fieldName":"草莓温室"}', 1, 391);
