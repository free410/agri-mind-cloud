package com.smartagri.modules.ai.adapter;

import com.smartagri.common.properties.AiProperties;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class MockAiModelAdapter implements AiModelAdapter {

    private final AiProperties aiProperties;

    public MockAiModelAdapter(AiProperties aiProperties) {
        this.aiProperties = aiProperties;
    }

    @Override
    public AiModelResponse chat(AiModelRequest request) {
        String answer = switch (request.getScenario()) {
            case "ANALYZE_ENVIRONMENT" -> buildEnvironmentAnswer(request.getMetadata());
            case "ANALYZE_WARNING" -> buildWarningAnswer(request.getMetadata());
            case "ANALYZE_WARNING_STRUCTURED" -> buildWarningStructuredJson(request.getMetadata());
            case "ANALYZE_WARNING_DISPOSAL" -> buildWarningDisposalJson(request.getMetadata());
            default -> buildKnowledgeAnswer(request.getUserPrompt());
        };

        return AiModelResponse.builder()
                .modelName("mock-smart-agri-v1")
                .content(answer)
                .build();
    }

    private String buildKnowledgeAnswer(String userPrompt) {
        if (StringUtils.containsAny(userPrompt, "番茄", "西红柿")) {
            return """
                    分析结论：番茄在生长期对水分与通风较为敏感。
                    影响说明：如果土壤湿度持续偏低，容易出现根系吸水不足、叶片萎蔫和坐果率下降。
                    处理建议：建议先查看最近 24 小时土壤湿度趋势，再结合温度情况适度滴灌，并在灌溉后复测环境数据。""";
        }
        if (StringUtils.containsAny(userPrompt, "湿度", "土壤")) {
            return """
                    分析结论：农业生产中的湿度管理需要与作物阶段联动判断。
                    影响说明：空气湿度过高容易诱发病害，土壤湿度过低则会影响根系吸收。
                    处理建议：建议结合具体作物、生长阶段和当前监测数据综合判断，再安排灌溉、通风或遮阳措施。""";
        }
        return """
                分析结论：当前问题属于农业知识问答场景。
                影响说明：建议结合具体作物类型、生长阶段和环境数据进行综合判断。
                处理建议：如果你愿意，可以继续提供农田、作物或预警信息，我可以给出更贴合业务数据的分析建议。""";
    }

    private String buildEnvironmentAnswer(Map<String, Object> metadata) {
        BigDecimal temperature = decimalValue(metadata.get("temperature"));
        BigDecimal humidity = decimalValue(metadata.get("humidity"));
        BigDecimal soilMoisture = decimalValue(metadata.get("soilMoisture"));
        BigDecimal lightIntensity = decimalValue(metadata.get("lightIntensity"));
        String fieldName = stringValue(metadata.get("fieldName"));
        String deviceName = stringValue(metadata.get("deviceName"));

        StringBuilder builder = new StringBuilder();
        builder.append("分析结论：");
        if (temperature != null && temperature.compareTo(new BigDecimal("32")) > 0) {
            builder.append(fieldName).append("当前存在高温风险；");
        }
        if (soilMoisture != null && soilMoisture.compareTo(new BigDecimal("30")) < 0) {
            builder.append("土壤湿度偏低；");
        }
        if (lightIntensity != null && lightIntensity.compareTo(new BigDecimal("50000")) > 0) {
            builder.append("光照强度偏高；");
        }
        if (builder.toString().endsWith("：")) {
            builder.append(fieldName).append("当前整体环境基本稳定；");
        }

        builder.append("\n影响说明：");
        builder.append("设备 ").append(StringUtils.defaultIfBlank(deviceName, "环境监测设备"))
                .append(" 上报的数据表明，高温或低墒情可能影响作物蒸腾平衡、根系吸水和果实品质。");

        builder.append("\n处理建议：");
        if (soilMoisture != null && soilMoisture.compareTo(new BigDecimal("30")) < 0) {
            builder.append("建议立即安排滴灌补水；");
        }
        if (temperature != null && temperature.compareTo(new BigDecimal("32")) > 0) {
            builder.append("在午后开启通风或喷雾降温；");
        }
        if (humidity != null && humidity.compareTo(new BigDecimal("85")) > 0) {
            builder.append("适当增强通风，降低病害风险；");
        }
        builder.append("30 分钟后再次采集环境数据，观察指标是否回落。");

        return builder.toString();
    }

    private String buildWarningAnswer(Map<String, Object> metadata) {
        String warningType = stringValue(metadata.get("warningType"));
        String warningContent = stringValue(metadata.get("warningContent"));
        String fieldName = stringValue(metadata.get("fieldName"));
        String deviceName = stringValue(metadata.get("deviceName"));

        return """
                分析结论：当前预警属于 %s 场景，已提示 %s 存在异常风险。
                原因解释：结合预警内容“%s”，可以判断该异常已经对农田环境稳定性或设备运行可靠性产生影响。
                处理建议：建议先核查 %s 的实时状态，再结合 %s 的最新环境数据确认异常是否持续；若异常仍在，及时执行灌溉、通风、遮阳或设备排查等措施。
                后续建议：处理后应继续观察至少 2 到 3 个采样周期，确保预警指标恢复正常。"""
                .formatted(
                        StringUtils.defaultIfBlank(warningType, "农业预警"),
                        StringUtils.defaultIfBlank(fieldName, "当前农田"),
                        StringUtils.defaultIfBlank(warningContent, "暂无详细描述"),
                        StringUtils.defaultIfBlank(deviceName, "相关设备"),
                        StringUtils.defaultIfBlank(fieldName, "当前农田")
                );
    }

    private String buildWarningStructuredJson(Map<String, Object> metadata) {
        String warningTypeName = stringValue(metadata.get("warningTypeName"));
        String warningContent = stringValue(metadata.get("warningContent"));
        Integer level = integerValue(metadata.get("warningLevel"));
        BigDecimal soilMoisture = decimalValue(metadata.get("latestSoilMoisture"));
        BigDecimal temperature = decimalValue(metadata.get("latestTemperature"));
        boolean manualReview = level != null && level >= 3;

        String possibleCause1 = "监测指标短时波动或灌溉/通风策略未及时联动。";
        String possibleCause2 = "设备采集周期与现场变化存在时间差。";
        String possibleCause3 = "当前农田处于敏感生长期，对环境阈值更敏感。";
        if (soilMoisture != null && soilMoisture.compareTo(new BigDecimal("30")) < 0) {
            possibleCause1 = "土壤含水率偏低，灌溉补水频次不足。";
        }
        if (temperature != null && temperature.compareTo(new BigDecimal("32")) > 0) {
            possibleCause2 = "棚内热积累明显，通风或降温动作触发不及时。";
        }

        int priority = level == null ? 2 : Math.max(1, Math.min(level, 3));
        return """
                {
                  "warningOverview":"%s：%s",
                  "possibleCauses":["%s","%s","%s"],
                  "riskImpact":"若不及时处理，可能导致作物生理胁迫、产量下降或病害风险上升（基于当前数据判断）。",
                  "recommendations":["先核查现场设备状态与最新采样数据一致性","优先处理与该预警直接相关的灌溉/通风/遮阳动作","处理后30分钟内复测并观察是否恢复到安全区间"],
                  "priorityLevel":%d,
                  "manualReviewRequired":%s
                }
                """.formatted(
                StringUtils.defaultIfBlank(warningTypeName, "预警"),
                StringUtils.defaultIfBlank(warningContent, "当前预警已触发"),
                possibleCause1,
                possibleCause2,
                possibleCause3,
                priority,
                manualReview
        );
    }

    private String buildWarningDisposalJson(Map<String, Object> metadata) {
        String deviceName = stringValue(metadata.get("deviceName"));
        Integer onlineStatus = integerValue(metadata.get("deviceOnlineStatus"));
        boolean needInspection = onlineStatus != null && onlineStatus == 0;

        return """
                {
                  "steps":[
                    "步骤1：先确认预警指标是否持续异常，并复核最近两次监测数据",
                    "步骤2：按预警类型执行现场处置（灌溉、通风、遮阳或设备复位）",
                    "步骤3：处置后补采一条环境数据，确认风险是否解除"
                  ],
                  "checkTargets":[
                    "关联农田环境传感器",
                    "关键执行设备（风机/灌溉阀门/补光设备）",
                    "%s"
                  ],
                  "needDeviceInspection":%s,
                  "summary":"建议按“确认异常-执行处置-复测闭环”流程处理，确保本次预警可追踪、可复盘。"
                }
                """.formatted(
                StringUtils.defaultIfBlank(deviceName, "现场设备连接状态"),
                needInspection
        );
    }

    private BigDecimal decimalValue(Object value) {
        if (value instanceof BigDecimal decimalValue) {
            return decimalValue;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        if (value == null) {
            return null;
        }
        return new BigDecimal(value.toString());
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private Integer integerValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(value.toString());
    }
}
