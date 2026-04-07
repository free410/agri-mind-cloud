SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `crop` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `crop_name` varchar(100) NOT NULL COMMENT '作物名称',
    `crop_type` varchar(50) NOT NULL COMMENT '作物类型',
    `farmland_id` bigint NOT NULL COMMENT '所属农田ID',
    `sowing_date` date NOT NULL COMMENT '播种日期',
    `maturity_date` date NOT NULL COMMENT '预计成熟日期',
    `growth_stage` varchar(30) NOT NULL COMMENT '生长阶段编码',
    `health_status` varchar(30) NOT NULL COMMENT '健康状态编码',
    `remark` varchar(255) DEFAULT NULL COMMENT '备注',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_crop_name` (`crop_name`),
    KEY `idx_crop_type` (`crop_type`),
    KEY `idx_crop_farmland_id` (`farmland_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作物表';

INSERT INTO `crop`
(`id`, `crop_name`, `crop_type`, `farmland_id`, `sowing_date`, `maturity_date`, `growth_stage`, `health_status`, `remark`, `create_time`, `update_time`)
VALUES
(1, '樱桃番茄', '果蔬类', 1, '2026-03-01', '2026-06-10', 'GROWING', 'HEALTHY', '适合作为温室精细化种植展示样本', NOW(), NOW()),
(2, '甜玉米', '粮食类', 2, '2026-03-15', '2026-07-20', 'SEEDLING', 'ATTENTION', '当前处于苗期，需重点关注土壤湿度', NOW(), NOW()),
(3, '草莓', '水果类', 3, '2026-02-20', '2026-05-30', 'FRUITING', 'DISEASE_RISK', '适合结合环境监测数据做病害风险演示', NOW(), NOW())
ON DUPLICATE KEY UPDATE
`crop_name` = VALUES(`crop_name`),
`crop_type` = VALUES(`crop_type`),
`farmland_id` = VALUES(`farmland_id`),
`sowing_date` = VALUES(`sowing_date`),
`maturity_date` = VALUES(`maturity_date`),
`growth_stage` = VALUES(`growth_stage`),
`health_status` = VALUES(`health_status`),
`remark` = VALUES(`remark`),
`update_time` = NOW();
