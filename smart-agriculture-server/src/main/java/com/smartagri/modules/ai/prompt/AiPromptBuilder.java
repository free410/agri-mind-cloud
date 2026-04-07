package com.smartagri.modules.ai.prompt;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartagri.common.exception.BusinessException;
import com.smartagri.common.result.ResultCode;
import com.smartagri.modules.ai.dto.AnalyzeRequest;
import com.smartagri.modules.ai.dto.ChatRequest;
import com.smartagri.modules.ai.enums.AiAnalysisTypeEnum;
import com.smartagri.modules.device.entity.Device;
import com.smartagri.modules.device.service.DeviceService;
import com.smartagri.modules.environment.entity.EnvironmentData;
import com.smartagri.modules.environment.service.EnvironmentDataService;
import com.smartagri.modules.field.entity.Field;
import com.smartagri.modules.field.service.FieldService;
import com.smartagri.modules.warning.entity.WarningRecord;
import com.smartagri.modules.warning.enums.WarningTypeEnum;
import com.smartagri.modules.warning.service.WarningService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class AiPromptBuilder {

    private static final String SYSTEM_PROMPT = """
            你是智慧农业管理系统中的农业智能助手。
            你的职责是结合农业知识、农田信息、设备状态、环境监测数据和预警信息，
            为管理员提供专业、简洁、可执行的分析建议。
            要求：
            1. 优先依据提供的业务数据，避免脱离上下文泛化回答；
            2. 输出要便于后台展示和答辩讲解；
            3. 当结论为推断时，明确说明“基于当前数据判断”；
            4. 输出可执行建议，避免空泛描述。
            """;

    private final FieldService fieldService;
    private final DeviceService deviceService;
    private final EnvironmentDataService environmentDataService;
    private final WarningService warningService;
    private final ObjectMapper objectMapper;

    public AiPromptBuilder(FieldService fieldService,
                           DeviceService deviceService,
                           EnvironmentDataService environmentDataService,
                           WarningService warningService,
                           ObjectMapper objectMapper) {
        this.fieldService = fieldService;
        this.deviceService = deviceService;
        this.environmentDataService = environmentDataService;
        this.warningService = warningService;
        this.objectMapper = objectMapper;
    }

    public AiPromptContext buildChatPrompt(ChatRequest request) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("question", request.getQuestion());
        metadata.put("relatedModule", StringUtils.defaultIfBlank(request.getRelatedModule(), "GENERAL"));
        metadata.put("relatedRecordId", request.getRelatedRecordId());

        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("管理员问题：").append(request.getQuestion()).append("\n");

        if ("WARNING".equalsIgnoreCase(request.getRelatedModule()) && request.getRelatedRecordId() != null) {
            WarningRecord warningRecord = warningService.getById(request.getRelatedRecordId());
            if (warningRecord != null) {
                Field field = warningRecord.getFarmlandId() == null ? null : fieldService.getById(warningRecord.getFarmlandId());
                Device device = warningRecord.getRelatedDeviceId() == null ? null : deviceService.getById(warningRecord.getRelatedDeviceId());
                metadata.put("warningType", warningRecord.getWarningType());
                metadata.put("warningContent", warningRecord.getContent());
                metadata.put("fieldName", field == null ? null : field.getFieldName());
                metadata.put("deviceName", device == null ? null : device.getDeviceName());
                promptBuilder.append("相关预警：").append(warningRecord.getContent()).append("\n");
            }
        }

        if ("ENVIRONMENT".equalsIgnoreCase(request.getRelatedModule()) && request.getRelatedRecordId() != null) {
            EnvironmentData environmentData = environmentDataService.getById(request.getRelatedRecordId());
            if (environmentData != null) {
                fillEnvironmentMetadata(metadata, environmentData);
                promptBuilder.append(buildEnvironmentPromptBlock(environmentData, metadata));
            }
        }

        promptBuilder.append("请结合以上信息，以智慧农业管理员易于理解的方式作答。");

        return AiPromptContext.builder()
                .scenario("CHAT")
                .systemPrompt(SYSTEM_PROMPT)
                .userPrompt(promptBuilder.toString())
                .relatedModule(StringUtils.defaultIfBlank(request.getRelatedModule(), "GENERAL"))
                .relatedRecordId(request.getRelatedRecordId())
                .displayQuestion(request.getQuestion())
                .metadata(metadata)
                .contextSnapshot(toJson(metadata))
                .title("农业知识问答")
                .build();
    }

    public AiPromptContext buildAnalyzePrompt(AnalyzeRequest request) {
        AiAnalysisTypeEnum analysisType = AiAnalysisTypeEnum.fromCode(request.getAnalysisType());
        return switch (analysisType) {
            case ENVIRONMENT -> buildEnvironmentAnalyzePrompt(request);
            case WARNING -> buildWarningAnalyzePrompt(request);
        };
    }

    public AiPromptContext buildWarningStructuredAnalyzePrompt(Long warningId) {
        WarningContext context = loadWarningContext(warningId);
        String prompt = """
                请对以下预警进行结构化分析，并只返回 JSON，不要返回任何额外说明。
                JSON 结构必须严格如下：
                {
                  "warningOverview":"string",
                  "possibleCauses":["string","string","string"],
                  "riskImpact":"string",
                  "recommendations":["string","string","string"],
                  "priorityLevel":1,
                  "manualReviewRequired":true
                }

                预警上下文：
                %s
                """.formatted(toJson(context.metadata()));

        return AiPromptContext.builder()
                .scenario("ANALYZE_WARNING_STRUCTURED")
                .systemPrompt(SYSTEM_PROMPT)
                .userPrompt(prompt)
                .relatedModule("WARNING")
                .relatedRecordId(warningId)
                .displayQuestion("请输出预警结构化分析")
                .metadata(context.metadata())
                .contextSnapshot(toJson(context.metadata()))
                .title("预警结构化分析")
                .build();
    }

    public AiPromptContext buildWarningDisposalPrompt(Long warningId) {
        WarningContext context = loadWarningContext(warningId);
        String prompt = """
                请生成预警处置建议，并只返回 JSON，不要返回任何额外说明。
                JSON 结构必须严格如下：
                {
                  "steps":["string","string","string"],
                  "checkTargets":["string","string","string"],
                  "needDeviceInspection":true,
                  "summary":"string"
                }

                预警上下文：
                %s
                """.formatted(toJson(context.metadata()));

        return AiPromptContext.builder()
                .scenario("ANALYZE_WARNING_DISPOSAL")
                .systemPrompt(SYSTEM_PROMPT)
                .userPrompt(prompt)
                .relatedModule("WARNING")
                .relatedRecordId(warningId)
                .displayQuestion("请输出预警处置建议")
                .metadata(context.metadata())
                .contextSnapshot(toJson(context.metadata()))
                .title("预警处置建议")
                .build();
    }

    private AiPromptContext buildEnvironmentAnalyzePrompt(AnalyzeRequest request) {
        EnvironmentData environmentData = resolveEnvironmentData(request);
        if (environmentData == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "未找到可分析的环境数据");
        }

        Map<String, Object> metadata = new LinkedHashMap<>();
        fillEnvironmentMetadata(metadata, environmentData);

        String prompt = """
                请对以下智慧农业环境数据进行分析：
                %s
                补充问题：%s
                请按“分析结论、影响说明、处理建议”三个部分输出。
                """.formatted(
                buildEnvironmentPromptBlock(environmentData, metadata),
                StringUtils.defaultIfBlank(request.getQuestion(), "请判断当前环境是否异常，并给出处置建议")
        );

        return AiPromptContext.builder()
                .scenario("ANALYZE_ENVIRONMENT")
                .systemPrompt(SYSTEM_PROMPT)
                .userPrompt(prompt)
                .relatedModule("ENVIRONMENT")
                .relatedRecordId(environmentData.getId())
                .displayQuestion(StringUtils.defaultIfBlank(request.getQuestion(), "请分析当前环境数据"))
                .metadata(metadata)
                .contextSnapshot(toJson(metadata))
                .title("环境数据分析")
                .build();
    }

    private AiPromptContext buildWarningAnalyzePrompt(AnalyzeRequest request) {
        if (request.getWarningId() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "预警分析必须传入 warningId");
        }

        WarningRecord warningRecord = warningService.getById(request.getWarningId());
        if (warningRecord == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "待分析的预警记录不存在");
        }

        Field field = warningRecord.getFarmlandId() == null ? null : fieldService.getById(warningRecord.getFarmlandId());
        Device device = warningRecord.getRelatedDeviceId() == null ? null : deviceService.getById(warningRecord.getRelatedDeviceId());

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("warningId", warningRecord.getId());
        metadata.put("warningType", warningRecord.getWarningType());
        metadata.put("warningTypeName", WarningTypeEnum.getDescriptionByCode(warningRecord.getWarningType()));
        metadata.put("warningContent", warningRecord.getContent());
        metadata.put("warningLevel", warningRecord.getLevel());
        metadata.put("warningStatus", warningRecord.getStatus());
        metadata.put("fieldName", field == null ? null : field.getFieldName());
        metadata.put("deviceName", device == null ? null : device.getDeviceName());
        metadata.put("createTime", warningRecord.getCreateTime());

        String prompt = """
                请对以下智慧农业预警信息进行解释并给出处置建议：
                农田名称：%s
                设备名称：%s
                预警类型：%s
                预警内容：%s
                预警等级：%s
                补充问题：%s
                请按“原因解释、农业影响、处理建议、后续观察”四个部分输出。
                """.formatted(
                field == null ? "未知农田" : field.getFieldName(),
                device == null ? "未知设备" : device.getDeviceName(),
                WarningTypeEnum.getDescriptionByCode(warningRecord.getWarningType()),
                warningRecord.getContent(),
                warningRecord.getLevel(),
                StringUtils.defaultIfBlank(request.getQuestion(), "请解释该预警并给出处置建议")
        );

        return AiPromptContext.builder()
                .scenario("ANALYZE_WARNING")
                .systemPrompt(SYSTEM_PROMPT)
                .userPrompt(prompt)
                .relatedModule("WARNING")
                .relatedRecordId(warningRecord.getId())
                .displayQuestion(StringUtils.defaultIfBlank(request.getQuestion(), "请分析该预警信息"))
                .metadata(metadata)
                .contextSnapshot(toJson(metadata))
                .title("预警分析")
                .build();
    }

    private WarningContext loadWarningContext(Long warningId) {
        WarningRecord warningRecord = warningService.getById(warningId);
        if (warningRecord == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "待分析的预警记录不存在");
        }

        Field field = warningRecord.getFarmlandId() == null ? null : fieldService.getById(warningRecord.getFarmlandId());
        Device device = warningRecord.getRelatedDeviceId() == null ? null : deviceService.getById(warningRecord.getRelatedDeviceId());
        EnvironmentData latestEnvironmentData = fetchLatestEnvironmentData(warningRecord.getFarmlandId(), warningRecord.getRelatedDeviceId());

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("warningId", warningRecord.getId());
        metadata.put("warningNo", warningRecord.getWarningNo());
        metadata.put("warningType", warningRecord.getWarningType());
        metadata.put("warningTypeName", WarningTypeEnum.getDescriptionByCode(warningRecord.getWarningType()));
        metadata.put("warningLevel", warningRecord.getLevel());
        metadata.put("warningStatus", warningRecord.getStatus());
        metadata.put("warningContent", warningRecord.getContent());
        metadata.put("triggerTime", warningRecord.getTriggerTime());
        metadata.put("fieldName", field == null ? null : field.getFieldName());
        metadata.put("deviceName", device == null ? null : device.getDeviceName());
        metadata.put("deviceOnlineStatus", device == null ? null : device.getOnlineStatus());
        metadata.put("deviceRunStatus", device == null ? null : device.getRunStatus());

        if (latestEnvironmentData != null) {
            metadata.put("latestTemperature", latestEnvironmentData.getTemperature());
            metadata.put("latestHumidity", latestEnvironmentData.getHumidity());
            metadata.put("latestSoilMoisture", latestEnvironmentData.getSoilMoisture());
            metadata.put("latestLightIntensity", latestEnvironmentData.getLightIntensity());
            metadata.put("latestPhValue", latestEnvironmentData.getPhValue());
            metadata.put("latestCollectTime", latestEnvironmentData.getCollectTime());
        }

        return new WarningContext(warningRecord, metadata);
    }

    private EnvironmentData resolveEnvironmentData(AnalyzeRequest request) {
        if (request.getEnvironmentDataId() != null) {
            return environmentDataService.getById(request.getEnvironmentDataId());
        }

        if (request.getFarmlandId() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "环境分析至少需要提供 environmentDataId 或 farmlandId");
        }

        LambdaQueryWrapper<EnvironmentData> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(EnvironmentData::getFarmlandId, request.getFarmlandId())
                .eq(request.getDeviceId() != null, EnvironmentData::getDeviceId, request.getDeviceId())
                .orderByDesc(EnvironmentData::getCollectTime)
                .last("LIMIT 1");
        return environmentDataService.getOne(queryWrapper);
    }

    private EnvironmentData fetchLatestEnvironmentData(Long farmlandId, Long deviceId) {
        if (farmlandId == null) {
            return null;
        }
        LambdaQueryWrapper<EnvironmentData> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(EnvironmentData::getFarmlandId, farmlandId)
                .eq(deviceId != null, EnvironmentData::getDeviceId, deviceId)
                .orderByDesc(EnvironmentData::getCollectTime)
                .last("LIMIT 1");
        return environmentDataService.getOne(queryWrapper);
    }

    private void fillEnvironmentMetadata(Map<String, Object> metadata, EnvironmentData environmentData) {
        Field field = fieldService.getById(environmentData.getFarmlandId());
        Device device = deviceService.getById(environmentData.getDeviceId());

        metadata.put("environmentDataId", environmentData.getId());
        metadata.put("fieldId", environmentData.getFarmlandId());
        metadata.put("fieldName", field == null ? null : field.getFieldName());
        metadata.put("deviceId", environmentData.getDeviceId());
        metadata.put("deviceName", device == null ? null : device.getDeviceName());
        metadata.put("temperature", environmentData.getTemperature());
        metadata.put("humidity", environmentData.getHumidity());
        metadata.put("soilMoisture", environmentData.getSoilMoisture());
        metadata.put("lightIntensity", environmentData.getLightIntensity());
        metadata.put("phValue", environmentData.getPhValue());
        metadata.put("collectTime", environmentData.getCollectTime());
    }

    private String buildEnvironmentPromptBlock(EnvironmentData environmentData, Map<String, Object> metadata) {
        return """
                农田名称：%s
                设备名称：%s
                采集时间：%s
                温度：%s℃
                湿度：%s%%
                土壤湿度：%s%%
                光照强度：%s lux
                PH 值：%s
                """.formatted(
                StringUtils.defaultIfBlank((String) metadata.get("fieldName"), "未知农田"),
                StringUtils.defaultIfBlank((String) metadata.get("deviceName"), "未知设备"),
                environmentData.getCollectTime(),
                environmentData.getTemperature(),
                environmentData.getHumidity(),
                environmentData.getSoilMoisture(),
                environmentData.getLightIntensity(),
                environmentData.getPhValue()
        );
    }

    private String toJson(Map<String, Object> metadata) {
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException exception) {
            return metadata.toString();
        }
    }

    private record WarningContext(WarningRecord warningRecord, Map<String, Object> metadata) {
    }
}
