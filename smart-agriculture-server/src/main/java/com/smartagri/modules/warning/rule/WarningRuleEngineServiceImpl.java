package com.smartagri.modules.warning.rule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartagri.modules.crop.entity.Crop;
import com.smartagri.modules.crop.service.CropService;
import com.smartagri.modules.device.entity.Device;
import com.smartagri.modules.device.enums.DeviceOnlineStatusEnum;
import com.smartagri.modules.environment.entity.EnvironmentData;
import com.smartagri.modules.environment.mapper.EnvironmentDataMapper;
import com.smartagri.modules.warning.enums.WarningLevelEnum;
import com.smartagri.modules.warning.enums.WarningTriggerSourceEnum;
import com.smartagri.modules.warning.enums.WarningTypeEnum;
import com.smartagri.modules.warning.rulecenter.entity.WarningRuleConfig;
import com.smartagri.modules.warning.rulecenter.enums.WarningRuleCompareOperatorEnum;
import com.smartagri.modules.warning.rulecenter.enums.WarningRuleMetricKeyEnum;
import com.smartagri.modules.warning.rulecenter.service.WarningRuleConfigService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class WarningRuleEngineServiceImpl implements WarningRuleEngineService {

    private final WarningRuleConfigService warningRuleConfigService;
    private final EnvironmentDataMapper environmentDataMapper;
    private final CropService cropService;

    public WarningRuleEngineServiceImpl(WarningRuleConfigService warningRuleConfigService,
                                        EnvironmentDataMapper environmentDataMapper,
                                        CropService cropService) {
        this.warningRuleConfigService = warningRuleConfigService;
        this.environmentDataMapper = environmentDataMapper;
        this.cropService = cropService;
    }

    @Override
    public List<WarningTriggerCommand> evaluateEnvironmentData(EnvironmentData environmentData) {
        if (environmentData == null) {
            return List.of();
        }

        List<WarningTriggerCommand> commands = new ArrayList<>();
        LocalDateTime triggerTime = environmentData.getCollectTime() == null ? LocalDateTime.now() : environmentData.getCollectTime();
        Long farmlandId = environmentData.getFarmlandId();
        Long deviceId = environmentData.getDeviceId();
        Long cropId = resolveCurrentCropId(farmlandId);

        boolean hasConfiguredRules = false;
        hasConfiguredRules |= appendMetricCommands(commands, environmentData, triggerTime,
                WarningRuleMetricKeyEnum.TEMPERATURE.getCode(), environmentData.getTemperature(), farmlandId, deviceId, cropId, "℃");
        hasConfiguredRules |= appendMetricCommands(commands, environmentData, triggerTime,
                WarningRuleMetricKeyEnum.HUMIDITY.getCode(), environmentData.getHumidity(), farmlandId, deviceId, cropId, "%");
        hasConfiguredRules |= appendMetricCommands(commands, environmentData, triggerTime,
                WarningRuleMetricKeyEnum.SOIL_MOISTURE.getCode(), environmentData.getSoilMoisture(), farmlandId, deviceId, cropId, "%");
        hasConfiguredRules |= appendMetricCommands(commands, environmentData, triggerTime,
                WarningRuleMetricKeyEnum.LIGHT.getCode(), environmentData.getLightIntensity(), farmlandId, deviceId, cropId, "lux");

        if (!hasConfiguredRules) {
            // 兼容老版本：当规则中心尚未初始化时，保留默认兜底策略，避免比赛演示“无预警”。
            commands.addAll(fallbackEnvironmentRules(environmentData, cropId, triggerTime));
        }

        return commands;
    }

    @Override
    public List<WarningTriggerCommand> evaluateDeviceStatus(Device device) {
        if (device == null || !Objects.equals(device.getOnlineStatus(), DeviceOnlineStatusEnum.OFFLINE.getCode())) {
            return List.of();
        }

        Long farmlandId = device.getFarmlandId();
        Long cropId = resolveCurrentCropId(farmlandId);
        List<WarningRuleConfig> rules = warningRuleConfigService.listMatchedEnabledRules(
                WarningRuleMetricKeyEnum.DEVICE_OFFLINE.getCode(), farmlandId, cropId);

        if (rules.isEmpty()) {
            return List.of(WarningTriggerCommand.builder()
                    .warningType(WarningTypeEnum.DEVICE_OFFLINE.getCode())
                    .warningLevel(WarningLevelEnum.MEDIUM.getCode())
                    .triggerSource(WarningTriggerSourceEnum.DEVICE_STATUS.getCode())
                    .farmlandId(device.getFarmlandId())
                    .relatedDeviceId(device.getId())
                    .relatedCropId(cropId)
                    .triggerTime(LocalDateTime.now())
                    .deduplicateMinutes(20)
                    .content("设备“" + device.getDeviceName() + "”离线，请尽快巡检。")
                    .build());
        }

        List<WarningTriggerCommand> commands = new ArrayList<>();
        BigDecimal metricValue = BigDecimal.ONE;
        for (WarningRuleConfig rule : rules) {
            if (!WarningRuleCompareOperatorEnum.evaluate(rule.getCompareOperator(), metricValue, rule.getThresholdValue())) {
                continue;
            }
            commands.add(WarningTriggerCommand.builder()
                    .warningType(rule.getWarningType())
                    .warningLevel(rule.getWarningLevel())
                    .triggerSource(WarningTriggerSourceEnum.DEVICE_STATUS.getCode())
                    .farmlandId(device.getFarmlandId())
                    .relatedDeviceId(device.getId())
                    .relatedCropId(cropId)
                    .triggerTime(LocalDateTime.now())
                    .deduplicateMinutes(rule.getDeduplicateMinutes())
                    .ruleId(rule.getId())
                    .ruleCode(rule.getRuleCode())
                    .ruleName(rule.getRuleName())
                    .content("命中规则【" + rule.getRuleName() + "】：设备“" + device.getDeviceName() + "”当前离线。")
                    .build());
        }
        return commands;
    }

    private boolean appendMetricCommands(List<WarningTriggerCommand> commands,
                                         EnvironmentData environmentData,
                                         LocalDateTime triggerTime,
                                         String metricKey,
                                         BigDecimal metricValue,
                                         Long farmlandId,
                                         Long deviceId,
                                         Long cropId,
                                         String unit) {
        if (metricValue == null) {
            return false;
        }

        List<WarningRuleConfig> rules = warningRuleConfigService.listMatchedEnabledRules(metricKey, farmlandId, cropId);
        if (rules.isEmpty()) {
            return false;
        }

        for (WarningRuleConfig rule : rules) {
            if (!WarningRuleCompareOperatorEnum.evaluate(rule.getCompareOperator(), metricValue, rule.getThresholdValue())) {
                continue;
            }
            if (!isContinuousSatisfied(rule, environmentData)) {
                continue;
            }

            commands.add(WarningTriggerCommand.builder()
                    .warningType(rule.getWarningType())
                    .warningLevel(rule.getWarningLevel())
                    .triggerSource(WarningTriggerSourceEnum.RULE_ENGINE.getCode())
                    .farmlandId(farmlandId)
                    .relatedDeviceId(deviceId)
                    .relatedCropId(cropId)
                    .triggerTime(triggerTime)
                    .deduplicateMinutes(rule.getDeduplicateMinutes())
                    .ruleId(rule.getId())
                    .ruleCode(rule.getRuleCode())
                    .ruleName(rule.getRuleName())
                    .content(buildRuleHitContent(rule, metricKey, metricValue, unit))
                    .build());
        }
        return true;
    }

    private String buildRuleHitContent(WarningRuleConfig rule, String metricKey, BigDecimal metricValue, String unit) {
        String metricName = WarningRuleMetricKeyEnum.getDescriptionByCode(metricKey);
        return "命中规则【" + rule.getRuleName() + "】："
                + metricName + "=" + metricValue + unit
                + "，条件为 " + metricName + " " + rule.getCompareOperator() + " " + rule.getThresholdValue();
    }

    private boolean isContinuousSatisfied(WarningRuleConfig rule, EnvironmentData latestData) {
        int continuousCount = rule.getContinuousCount() == null ? 1 : rule.getContinuousCount();
        if (continuousCount <= 1) {
            return true;
        }

        LambdaQueryWrapper<EnvironmentData> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(EnvironmentData::getFarmlandId, latestData.getFarmlandId())
                .eq(latestData.getDeviceId() != null, EnvironmentData::getDeviceId, latestData.getDeviceId())
                .le(EnvironmentData::getCollectTime, latestData.getCollectTime())
                .orderByDesc(EnvironmentData::getCollectTime)
                .last("LIMIT " + continuousCount);

        List<EnvironmentData> recentList = environmentDataMapper.selectList(queryWrapper);
        if (recentList.size() < continuousCount) {
            return false;
        }

        for (EnvironmentData item : recentList) {
            BigDecimal value = extractMetricValue(item, rule.getMetricKey());
            if (!WarningRuleCompareOperatorEnum.evaluate(rule.getCompareOperator(), value, rule.getThresholdValue())) {
                return false;
            }
        }
        return true;
    }

    private BigDecimal extractMetricValue(EnvironmentData data, String metricKey) {
        return switch (metricKey) {
            case "temperature" -> data.getTemperature();
            case "humidity" -> data.getHumidity();
            case "soil_moisture" -> data.getSoilMoisture();
            case "light" -> data.getLightIntensity();
            default -> null;
        };
    }

    private Long resolveCurrentCropId(Long farmlandId) {
        if (farmlandId == null) {
            return null;
        }
        return cropService.list(new LambdaQueryWrapper<Crop>()
                        .eq(Crop::getFarmlandId, farmlandId)
                        .orderByDesc(Crop::getUpdateTime)
                        .orderByDesc(Crop::getId)
                        .last("LIMIT 1"))
                .stream()
                .findFirst()
                .map(Crop::getId)
                .orElse(null);
    }

    private List<WarningTriggerCommand> fallbackEnvironmentRules(EnvironmentData environmentData,
                                                                 Long cropId,
                                                                 LocalDateTime triggerTime) {
        List<WarningTriggerCommand> commands = new ArrayList<>();

        if (environmentData.getTemperature() != null && environmentData.getTemperature().compareTo(new BigDecimal("32")) > 0) {
            commands.add(buildFallbackCommand(WarningTypeEnum.TEMPERATURE_HIGH.getCode(), WarningLevelEnum.HIGH.getCode(),
                    environmentData, cropId, triggerTime, "温度过高"));
        }
        if (environmentData.getTemperature() != null && environmentData.getTemperature().compareTo(new BigDecimal("10")) < 0) {
            commands.add(buildFallbackCommand(WarningTypeEnum.TEMPERATURE_LOW.getCode(), WarningLevelEnum.MEDIUM.getCode(),
                    environmentData, cropId, triggerTime, "温度过低"));
        }
        if (environmentData.getSoilMoisture() != null && environmentData.getSoilMoisture().compareTo(new BigDecimal("30")) < 0) {
            commands.add(buildFallbackCommand(WarningTypeEnum.SOIL_MOISTURE_LOW.getCode(), WarningLevelEnum.HIGH.getCode(),
                    environmentData, cropId, triggerTime, "土壤湿度过低"));
        }
        if (environmentData.getLightIntensity() != null
                && (environmentData.getLightIntensity().compareTo(new BigDecimal("5000")) < 0
                || environmentData.getLightIntensity().compareTo(new BigDecimal("50000")) > 0)) {
            commands.add(buildFallbackCommand(WarningTypeEnum.LIGHT_ABNORMAL.getCode(), WarningLevelEnum.MEDIUM.getCode(),
                    environmentData, cropId, triggerTime, "光照异常"));
        }

        return commands;
    }

    private WarningTriggerCommand buildFallbackCommand(String warningType,
                                                       Integer warningLevel,
                                                       EnvironmentData environmentData,
                                                       Long cropId,
                                                       LocalDateTime triggerTime,
                                                       String title) {
        return WarningTriggerCommand.builder()
                .warningType(warningType)
                .warningLevel(warningLevel)
                .triggerSource(WarningTriggerSourceEnum.RULE_ENGINE.getCode())
                .farmlandId(environmentData.getFarmlandId())
                .relatedDeviceId(environmentData.getDeviceId())
                .relatedCropId(cropId)
                .triggerTime(triggerTime)
                .deduplicateMinutes(20)
                .content("命中默认规则：" + title + "，建议优先核查现场指标。")
                .build();
    }
}

