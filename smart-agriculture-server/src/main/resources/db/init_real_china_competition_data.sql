SET NAMES utf8mb4;

/*
  智慧农业管理系统（比赛版）真实场景初始化数据
  说明：
  1) 农田/作物/设备名称采用中国公开农业示范场景整理；
  2) 环境数据用于比赛演示，结合公开气象区间与设施农业常见监测范围；
  3) 预警数据已适配“智能预警闭环中心”字段。
*/

DELETE FROM `ai_chat_log`;
SET @db_name = DATABASE();
SET @sql = IF(
    EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema = @db_name AND table_name = 'warning_ai_analysis'),
    'DELETE FROM `warning_ai_analysis`',
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(
    EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema = @db_name AND table_name = 'warning_process_log'),
    'DELETE FROM `warning_process_log`',
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
DELETE FROM `warning`;
DELETE FROM `env_data`;
DELETE FROM `device`;
DELETE FROM `crop`;
DELETE FROM `farmland`;
DELETE FROM `admin`;

INSERT INTO `admin`
(`id`, `username`, `password`, `nickname`, `phone`, `email`, `avatar`, `status`, `last_login_time`, `remark`, `create_time`, `update_time`)
VALUES
(1, 'admin', '$2a$10$m3Cd5cuqRgLVVp0c6PxVrO/sDIvhWQpqvlQ9jAuh09MWyGgYs/Bom', '系统管理员', '13800000000', 'admin@smartagri.local', NULL, 1, NULL, '比赛演示管理员账号', NOW(), NOW());

INSERT INTO `farmland`
(`id`, `field_name`, `area`, `location`, `soil_type`, `status`, `remark`, `create_time`, `update_time`)
VALUES
(1, '寿光市智慧农业科技园番茄连栋温室示范区', 240.00, '山东省潍坊市寿光市智慧农业科技园', '壤土', 1, '设施番茄智慧种植核心示范区', NOW(), NOW()),
(2, '东港市圣野浆果草莓智慧温室示范区', 12000.00, '辽宁省丹东市东港市十字街镇赤榆村', '砂壤土', 1, '草莓种植示范与温室监测场景', NOW(), NOW()),
(3, '武清区灰锅口村葡萄设施农业示范区', 2700.00, '天津市武清区梅厂镇灰锅口村', '沙壤土', 1, '葡萄设施农业与灌溉控制示范区', NOW(), NOW()),
(4, '昆山市陆家未来智慧田园A+温室果菜示范区', 28.20, '江苏省苏州市昆山市陆家镇未来智慧田园', '椰糠无土栽培基质', 1, '玻璃温室果菜工厂化示范', NOW(), NOW()),
(5, '昌宁县智慧温室樱桃番茄示范区', 70.00, '云南省保山市昌宁县柯街镇', '壤土', 1, '樱桃番茄设施化生产示范区', NOW(), NOW()),
(6, '武汉经开区智慧温室串收番茄示范园', 150.00, '湖北省武汉市武汉经开区东荆街', '椰糠无土栽培基质', 1, '智慧温室串收番茄示范基地', NOW(), NOW()),
(7, '东台嘉满仓半封闭智能玻璃温室番茄工厂', 150.00, '江苏省盐城市东台市三仓镇现代农业园区', '营养液循环基质栽培', 1, '半封闭温室番茄工厂示范场景', NOW(), NOW());

INSERT INTO `crop`
(`id`, `crop_name`, `crop_type`, `farmland_id`, `sowing_date`, `maturity_date`, `growth_stage`, `health_status`, `remark`, `create_time`, `update_time`)
VALUES
(1, '番茄', '设施蔬菜', 1, '2026-02-18', '2026-06-20', 'GROWING', 'HEALTHY', '寿光设施番茄场景', NOW(), NOW()),
(2, '草莓', '设施水果', 2, '2025-11-05', '2026-05-10', 'FRUITING', 'HEALTHY', '东港草莓温室场景', NOW(), NOW()),
(3, '葡萄', '设施果树', 3, '2025-09-25', '2026-08-20', 'SEEDLING', 'ATTENTION', '武清葡萄设施农业场景', NOW(), NOW()),
(4, '草莓', '设施水果', 4, '2025-11-20', '2026-05-15', 'FRUITING', 'HEALTHY', '昆山A+温室草莓场景', NOW(), NOW()),
(5, '樱桃番茄', '设施蔬菜', 5, '2025-12-22', '2026-05-25', 'FRUITING', 'HEALTHY', '昌宁樱桃番茄场景', NOW(), NOW()),
(6, '串收番茄', '设施蔬菜', 6, '2025-12-10', '2026-06-30', 'GROWING', 'HEALTHY', '武汉智慧温室串收番茄场景', NOW(), NOW()),
(7, '番茄', '设施蔬菜', 7, '2025-12-15', '2026-07-10', 'GROWING', 'HEALTHY', '东台半封闭温室番茄场景', NOW(), NOW());

INSERT INTO `device`
(`id`, `device_code`, `device_name`, `device_type`, `farmland_id`, `install_time`, `online_status`, `run_status`, `remark`, `create_time`, `update_time`)
VALUES
(1, 'ZG-2026-001', '正压湿帘冷暖风机组', '环境调控设备', 1, '2025-12-18 10:00:00', 1, 1, '寿光温室核心环境调控设备', NOW(), NOW()),
(2, 'ZG-2026-002', '智能水肥一体化系统', '水肥设备', 1, '2025-12-20 09:30:00', 1, 1, '寿光温室滴灌与施肥设备', NOW(), NOW()),
(3, 'DG-2026-001', '空气温湿度监测仪', '环境传感器', 2, '2025-10-28 14:20:00', 1, 1, '东港草莓棚空气监测设备', NOW(), NOW()),
(4, 'DG-2026-002', '智慧农业监控摄像头', '视频监控设备', 2, '2025-10-28 15:10:00', 1, 1, '东港草莓棚监控设备', NOW(), NOW()),
(5, 'WQ-2026-001', '节能日光温室环境监测终端', '环境传感器', 3, '2026-01-06 11:00:00', 1, 2, '武清温室环境监测终端', NOW(), NOW()),
(6, 'WQ-2026-002', '葡萄棚灌溉控制柜', '灌溉控制设备', 3, '2026-01-06 11:40:00', 1, 1, '武清葡萄灌溉控制设备', NOW(), NOW()),
(7, 'KS-2026-001', 'A+温室环控终端', '环境传感器', 4, '2025-11-10 09:00:00', 1, 1, '昆山A+温室环控设备', NOW(), NOW()),
(8, 'KS-2026-002', 'AGV运输车', '智能运输设备', 4, '2025-11-11 10:20:00', 0, 2, '昆山A+温室AGV运输设备', NOW(), NOW()),
(9, 'CN-2026-001', '水肥处理中心控制终端', '水肥设备', 5, '2025-12-05 13:00:00', 1, 1, '昌宁示范区水肥中心设备', NOW(), NOW()),
(10, 'CN-2026-002', '樱桃番茄温室环境监测仪', '环境传感器', 5, '2025-12-05 13:40:00', 1, 1, '昌宁温室环境采集设备', NOW(), NOW()),
(11, 'WH-2026-001', 'KUBO温室环境控制系统', '环境调控设备', 6, '2025-11-28 08:30:00', 1, 1, '武汉智慧温室环控设备', NOW(), NOW()),
(12, 'WH-2026-002', '高压雾化降温系统', '降温设备', 6, '2025-11-28 09:10:00', 1, 1, '武汉温室降温设备', NOW(), NOW()),
(13, 'DT-2026-001', 'Ultra-Clima半封闭温室控制系统', '环境调控设备', 7, '2025-12-12 10:00:00', 1, 1, '东台温室环控系统', NOW(), NOW()),
(14, 'DT-2026-002', '番茄授粉机器人', '智能机器人', 7, '2026-01-16 09:20:00', 1, 2, '东台温室授粉机器人', NOW(), NOW());

INSERT INTO `env_data`
(`id`, `farmland_id`, `device_id`, `temperature`, `humidity`, `soil_moisture`, `light_intensity`, `ph_value`, `collect_time`, `create_time`)
VALUES
(1, 1, 1, 8.0, 49.0, 34.5, 21800.0, 6.50, '2026-04-01 09:00:00', NOW()),
(2, 1, 2, 12.0, 46.0, 31.2, 26500.0, 6.45, '2026-04-01 14:00:00', NOW()),
(3, 2, 3, 9.0, 50.0, 42.0, 19500.0, 6.20, '2026-04-01 09:30:00', NOW()),
(4, 2, 4, 15.0, 47.0, 39.6, 23800.0, 6.18, '2026-04-01 14:30:00', NOW()),
(5, 3, 5, 9.0, 80.0, 36.8, 16800.0, 6.80, '2026-04-01 08:40:00', NOW()),
(6, 3, 6, 18.0, 72.0, 33.9, 28600.0, 6.75, '2026-04-01 13:40:00', NOW()),
(7, 4, 7, 13.0, 68.0, 43.5, 22600.0, 6.10, '2026-04-01 09:20:00', NOW()),
(8, 4, 7, 20.0, 61.0, 40.8, 31800.0, 6.05, '2026-04-01 14:20:00', NOW()),
(9, 5, 10, 12.0, 58.0, 37.2, 24200.0, 6.35, '2026-04-01 09:10:00', NOW()),
(10, 5, 9, 24.0, 54.0, 32.4, 35600.0, 6.30, '2026-04-01 14:10:00', NOW()),
(11, 6, 11, 14.0, 63.0, 41.0, 20800.0, 6.40, '2026-04-01 09:50:00', NOW()),
(12, 6, 12, 25.0, 57.0, 35.6, 34200.0, 6.35, '2026-04-01 14:50:00', NOW()),
(13, 7, 13, 12.0, 66.0, 39.1, 21400.0, 6.55, '2026-04-01 09:15:00', NOW()),
(14, 7, 14, 18.0, 60.0, 34.2, 29800.0, 6.48, '2026-04-01 14:15:00', NOW());

INSERT INTO `warning`
(`id`, `warning_no`, `warning_type`, `content`, `level`, `status`, `farmland_id`, `related_device_id`, `related_crop_id`,
 `trigger_source`, `trigger_time`, `ai_analysis_summary`, `ai_suggestion_summary`, `handler_name`, `handle_time`,
 `handle_remark`, `close_time`, `is_false_alarm`, `create_time`, `update_time`)
VALUES
(1, 'WARN202604011410000001', 'SOIL_MOISTURE_LOW', '寿光番茄温室土壤湿度下降至31.2%，接近设施番茄控制下限。', 2, 0, 1, 2, 1,
 'ENV_MONITOR', '2026-04-01 14:10:00', NULL, NULL, NULL, NULL, NULL, NULL, 0, NOW(), NOW()),
(2, 'WARN202604010940000002', 'TEMPERATURE_LOW', '东港草莓温室上午温度仅9℃，低于结果期建议区间。', 2, 1, 2, 3, 2,
 'ENV_MONITOR', '2026-04-01 09:40:00', '当前低温持续风险中等，可能影响果实品质稳定性。', '建议优先核查保温与补温设备，并观察夜间温度回升速度。', '系统管理员', '2026-04-01 09:55:00', '已安排值守人员检查保温帘。', NULL, 0, NOW(), NOW()),
(3, 'WARN202604010850000003', 'HUMIDITY_HIGH', '武清葡萄示范区空气湿度达到80%，苗期病害风险提升。', 1, 0, 3, 5, 3,
 'ENV_MONITOR', '2026-04-01 08:50:00', NULL, NULL, NULL, NULL, NULL, NULL, 0, NOW(), NOW()),
(4, 'WARN202604011020000004', 'DEVICE_OFFLINE', '昆山A+温室 AGV运输车离线，可能影响温室内物流调度。', 2, 2, 4, 8, 4,
 'DEVICE_STATUS', '2026-04-01 10:20:00', '离线时长超过30分钟，已影响运输任务链路。', '建议检查电源和网络，恢复后补跑运输任务。', '系统管理员', '2026-04-01 10:40:00', '已完成网络复位与设备重连。', NULL, 0, NOW(), NOW()),
(5, 'WARN202604011415000005', 'LIGHT_INTENSITY_HIGH', '昌宁樱桃番茄温室光照达35600 lux，需关注高光+高温叠加风险。', 1, 0, 5, 10, 5,
 'ENV_MONITOR', '2026-04-01 14:15:00', NULL, NULL, NULL, NULL, NULL, NULL, 0, NOW(), NOW()),
(6, 'WARN202604011455000006', 'SOIL_MOISTURE_LOW', '武汉串收番茄示范园土壤湿度下降至35.6%，建议及时补水。', 1, 1, 6, 12, 6,
 'RULE_ENGINE', '2026-04-01 14:55:00', '土壤水分连续下滑，建议同步评估温室蒸腾负荷。', '建议先小流量补水，再复测土壤湿度并联动雾化降温。', '系统管理员', '2026-04-01 15:05:00', '已启动低流量滴灌策略。', NULL, 0, NOW(), NOW()),
(7, 'WARN202604011425000007', 'DEVICE_MAINTENANCE', '东台番茄授粉机器人处于待机维护状态，建议花期前完成检修。', 1, 3, 7, 14, 7,
 'MANUAL_INPUT', '2026-04-01 14:25:00', '当前属于可控维护风险，需按计划闭环。', '建议检修后安排一次授粉作业模拟演练。', '系统管理员', '2026-04-01 16:00:00', '检修完成并通过联调测试。', '2026-04-01 16:30:00', 0, NOW(), NOW());

INSERT INTO `warning_process_log`
(`warning_id`, `action_type`, `status_from`, `status_to`, `operator_name`, `process_note`, `process_time`, `create_time`)
VALUES
(2, 'CONFIRM', 0, 0, '系统管理员', '确认低温预警并进入研判。', '2026-04-01 09:42:00', NOW()),
(2, 'START_PROCESS', 0, 1, '系统管理员', '开始处理低温预警。', '2026-04-01 09:55:00', NOW()),
(4, 'AUTO_GENERATE', NULL, 0, '系统规则引擎', '检测到设备离线自动生成预警。', '2026-04-01 10:20:00', NOW()),
(4, 'SUBMIT_RESULT', 1, 2, '系统管理员', '已完成重连和巡检。', '2026-04-01 10:40:00', NOW()),
(7, 'CLOSE_LOOP', 2, 3, '系统管理员', '授粉机器人维护完成并闭环。', '2026-04-01 16:30:00', NOW());

INSERT INTO `warning_ai_analysis`
(`warning_id`, `analysis_source`, `analysis_summary`, `suggestion_summary`, `risk_score`, `priority_level`, `raw_response`, `create_time`)
VALUES
(2, 'MOCK', '低温持续会影响草莓果实膨大与糖度形成。', '优先复核保温帘、补温设备和夜间温度回升。', 71, 2, 'mock-warning-analysis-2', NOW()),
(4, 'MOCK', 'AGV离线造成温室物流链路中断风险。', '建议检查电源网络并补跑中断任务。', 66, 2, 'mock-warning-analysis-4', NOW()),
(6, 'MOCK', '土壤水分下行趋势明显，需防止继续失水。', '建议分阶段灌溉并结合环境曲线复测。', 58, 2, 'mock-warning-analysis-6', NOW());

INSERT INTO `ai_chat_log`
(`id`, `admin_id`, `session_id`, `question`, `answer`, `related_module`, `related_record_id`, `model_name`, `context_snapshot`, `response_status`, `response_time_ms`, `create_time`, `update_time`)
VALUES
(1, 1, 'session-real-001', '寿光番茄温室土壤湿度偏低怎么办？', '建议先检查水肥一体化系统与滴灌阀门，再进行小流量补灌，30分钟后复测。', 'ENVIRONMENT', 2, 'mock-smart-agri-v1', '{"field":"寿光番茄温室","soilMoisture":31.2}', 1, 426, '2026-04-01 14:12:00', NOW()),
(2, 1, 'session-real-002', '东港草莓温室低温预警说明了什么？', '低温可能影响果实发育与糖度积累，建议优先排查保温措施并关注夜间温度。', 'WARNING', 2, 'mock-smart-agri-v1', '{"field":"东港草莓温室","temperature":9}', 1, 398, '2026-04-01 09:42:00', NOW()),
(3, 1, 'session-real-003', '武清葡萄棚湿度偏高有什么风险？', '湿度持续偏高会增加病害概率，建议加强通风并做病害巡查。', 'ENVIRONMENT', 5, 'mock-smart-agri-v1', '{"field":"武清葡萄示范区","humidity":80}', 1, 387, '2026-04-01 08:55:00', NOW()),
(4, 1, 'session-real-004', '昆山A+温室AGV离线会影响什么？', '会影响采收与转运效率，建议优先恢复供电与网络并补跑任务。', 'WARNING', 4, 'mock-smart-agri-v1', '{"field":"昆山A+温室","device":"AGV运输车"}', 1, 365, '2026-04-01 10:24:00', NOW()),
(5, 1, 'session-real-005', '昌宁樱桃番茄午后高光需要注意什么？', '需关注高光与高温叠加导致的品质波动，建议联动遮阳与通风。', 'ENVIRONMENT', 10, 'mock-smart-agri-v1', '{"field":"昌宁温室","light":35600}', 1, 401, '2026-04-01 14:18:00', NOW()),
(6, 1, 'session-real-006', '武汉串收番茄雾化系统主要作用是什么？', '在高温时段配合通风降低热负荷，稳定番茄生长环境。', 'GENERAL', 12, 'mock-smart-agri-v1', '{"field":"武汉智慧温室","device":"高压雾化降温系统"}', 1, 376, '2026-04-01 15:00:00', NOW()),
(7, 1, 'session-real-007', '东台授粉机器人维护时优先检查什么？', '优先检查供电、视觉识别和机械臂精度，确保花期前恢复稳定。', 'WARNING', 7, 'mock-smart-agri-v1', '{"field":"东台番茄工厂","device":"番茄授粉机器人"}', 1, 352, '2026-04-01 14:28:00', NOW());
