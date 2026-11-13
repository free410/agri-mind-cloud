package com.smartagri.modules.ai.rag.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartagri.common.properties.RagProperties;
import com.smartagri.modules.ai.rag.client.RagClient;
import com.smartagri.modules.ai.rag.client.dto.RagAnswerClientRequest;
import com.smartagri.modules.ai.rag.client.dto.RagAnswerClientResponse;
import com.smartagri.modules.ai.rag.dto.RagChatRequest;
import com.smartagri.modules.ai.rag.service.AiRagService;
import com.smartagri.modules.ai.rag.service.KnowledgeGraphRagContextProvider;
import com.smartagri.modules.ai.rag.service.KnowledgeGraphRagContextProvider.KnowledgeGraphRagSnippet;
import com.smartagri.modules.ai.rag.vo.RagBusinessCitationVO;
import com.smartagri.modules.ai.rag.vo.RagChatResponse;
import com.smartagri.modules.ai.rag.vo.RagGraphCitationVO;
import com.smartagri.modules.ai.rag.vo.RagKnowledgeCitationVO;
import com.smartagri.modules.crop.entity.Crop;
import com.smartagri.modules.crop.mapper.CropMapper;
import com.smartagri.modules.device.entity.Device;
import com.smartagri.modules.device.mapper.DeviceMapper;
import com.smartagri.modules.environment.entity.EnvironmentData;
import com.smartagri.modules.environment.mapper.EnvironmentDataMapper;
import com.smartagri.modules.field.entity.Field;
import com.smartagri.modules.field.mapper.FieldMapper;
import com.smartagri.modules.warning.entity.WarningAiAnalysis;
import com.smartagri.modules.warning.entity.WarningProcessLog;
import com.smartagri.modules.warning.entity.WarningRecord;
import com.smartagri.modules.warning.enums.WarningLevelEnum;
import com.smartagri.modules.warning.enums.WarningStatusEnum;
import com.smartagri.modules.warning.mapper.WarningAiAnalysisMapper;
import com.smartagri.modules.warning.mapper.WarningMapper;
import com.smartagri.modules.warning.mapper.WarningProcessLogMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
public class AiRagServiceImpl implements AiRagService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RagProperties ragProperties;
    private final RagClient ragClient;
    private final WarningMapper warningMapper;
    private final EnvironmentDataMapper environmentDataMapper;
    private final DeviceMapper deviceMapper;
    private final FieldMapper fieldMapper;
    private final CropMapper cropMapper;
    private final WarningProcessLogMapper warningProcessLogMapper;
    private final WarningAiAnalysisMapper warningAiAnalysisMapper;
    private final KnowledgeGraphRagContextProvider knowledgeGraphRagContextProvider;

    public AiRagServiceImpl(RagProperties ragProperties,
                            RagClient ragClient,
                            WarningMapper warningMapper,
                            EnvironmentDataMapper environmentDataMapper,
                            DeviceMapper deviceMapper,
                            FieldMapper fieldMapper,
                            CropMapper cropMapper,
                            WarningProcessLogMapper warningProcessLogMapper,
                            WarningAiAnalysisMapper warningAiAnalysisMapper,
                            KnowledgeGraphRagContextProvider knowledgeGraphRagContextProvider) {
        this.ragProperties = ragProperties;
        this.ragClient = ragClient;
        this.warningMapper = warningMapper;
        this.environmentDataMapper = environmentDataMapper;
        this.deviceMapper = deviceMapper;
        this.fieldMapper = fieldMapper;
        this.cropMapper = cropMapper;
        this.warningProcessLogMapper = warningProcessLogMapper;
        this.warningAiAnalysisMapper = warningAiAnalysisMapper;
        this.knowledgeGraphRagContextProvider = knowledgeGraphRagContextProvider;
    }

    @Override
    public RagChatResponse chat(RagChatRequest request) {
        Map<String, Object> businessContext = buildBusinessContext(request);
        KnowledgeGraphRagSnippet kgSnippet = knowledgeGraphRagContextProvider.build(request.getQuery());

        if (!ragProperties.isEnabled()) {
            log.info("RAG服务已禁用，使用本地兜底");
            return buildFallbackResponse(request, businessContext, "RAG 服务已禁用，已使用本地兜底结果", kgSnippet.citations());
        }

        try {
            List<Map<String, Object>> citedKgData = buildCitedKnowledgeGraph(kgSnippet.citations());

            RagAnswerClientRequest req = RagAnswerClientRequest.builder()
                    .query(request.getQuery())
                    .mode(StringUtils.defaultIfBlank(request.getMode(), "auto"))
                    .businessContext(businessContext)
                    .relatedWarningId(request.getRelatedWarningId())
                    .relatedDeviceId(request.getRelatedDeviceId())
                    .relatedFarmlandId(request.getRelatedFarmlandId())
                    .topK(request.getTopK() == null ? ragProperties.getDefaultTopK() : request.getTopK())
                    .citedKnowledgeGraph(citedKgData)
                    .build();
            if (StringUtils.isNotBlank(kgSnippet.contextText())) {
                req.setKnowledgeGraphContext(kgSnippet.contextText());
            }

            RagAnswerClientResponse response = ragClient.answer(req);
            return mapResponse(response, kgSnippet.citations());
        } catch (Exception exception) {
            log.warn("RAG 调用失败: {}", exception.getMessage());
            if (!ragProperties.isFallbackEnabled()) {
                throw exception;
            }
            return buildFallbackResponse(request, businessContext, exception.getMessage(), kgSnippet.citations());
        }
    }

    /**
     * 将图谱引用列表转换为前端需要的格式，包含 subgraph 数据用于迷你图谱可视化。
     * 字段名与前端期望一致: id, title, kind, subgraph
     */
    private List<Map<String, Object>> buildCitedKnowledgeGraph(List<RagGraphCitationVO> citations) {
        if (citations == null || citations.isEmpty()) {
            return null;
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (RagGraphCitationVO c : citations) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", c.getId());
            item.put("title", c.getTitle());
            item.put("kind", c.getKind());
            if (c.getSubgraph() != null) {
                item.put("subgraph", c.getSubgraph());
            }
            result.add(item);
        }
        return result;
    }

    private RagChatResponse mapResponse(RagAnswerClientResponse response, List<RagGraphCitationVO> citedKnowledgeGraph) {
        RagBusinessCitationVO businessCitation = null;
        if (response.getCitedBusinessContext() != null) {
            RagAnswerClientResponse.RelatedIds relatedIds =
                    response.getCitedBusinessContext().getRelatedIds();
            businessCitation = RagBusinessCitationVO.builder()
                    .summaryBlocks(response.getCitedBusinessContext().getSummaryBlocks())
                    .riskSignals(response.getCitedBusinessContext().getRiskSignals())
                    .relatedWarningId(relatedIds == null ? null : relatedIds.getRelatedWarningId())
                    .relatedDeviceId(relatedIds == null ? null : relatedIds.getRelatedDeviceId())
                    .relatedFarmlandId(relatedIds == null ? null : relatedIds.getRelatedFarmlandId())
                    .build();
        }

        log.info("mapResponse: FastAPI返回的citedKnowledgeSources数量={}",
            response.getCitedKnowledgeSources() != null ? response.getCitedKnowledgeSources().size() : 0);
        if (response.getCitedKnowledgeSources() != null && !response.getCitedKnowledgeSources().isEmpty()) {
            log.info("mapResponse: 第一条知识来源原始score={}, 类型={}",
                response.getCitedKnowledgeSources().get(0).getScore(),
                response.getCitedKnowledgeSources().get(0).getScore() != null ?
                    response.getCitedKnowledgeSources().get(0).getScore().getClass().getSimpleName() : "null");
        }

        List<RagKnowledgeCitationVO> knowledgeSources = response.getCitedKnowledgeSources() == null
                ? List.of()
                : response.getCitedKnowledgeSources().stream()
                .map(item -> {
                    Double score = item.getScore();
                    log.info("mapResponse: 转换前score={}, 转换后score={}", score, score);
                    return RagKnowledgeCitationVO.builder()
                            .id(item.getId())
                            .title(item.getTitle())
                            .source(item.getSource())
                            .score(score)
                            .topic(item.getTopic())
                            .content(item.getContent())
                            .rerankScore(item.getRerankScore())
                            .domain(item.getDomain())
                            .metadata(item.getMetadata())
                            .build();
                })
                .toList();

        // 优先使用 FastAPI 返回的图谱引用（含 subgraph）；兜底使用本地检索结果
        List<RagGraphCitationVO> graphCitations = convertResponseKgToCitations(
                response.getCitedKnowledgeGraph(), citedKnowledgeGraph);

        log.info("mapResponse: FastAPI返回的citedKnowledgeGraph数量={}, 转换后graphCitations数量={}",
            response.getCitedKnowledgeGraph() != null ? response.getCitedKnowledgeGraph().size() : 0,
            graphCitations.size());
        if (graphCitations.isEmpty() && citedKnowledgeGraph != null && !citedKnowledgeGraph.isEmpty()) {
            log.info("mapResponse: FastAPI返回为空，使用fallback数据");
        }

        String fallbackReason = response.getFallbackReason();
        return RagChatResponse.builder()
                .sessionId(StringUtils.defaultIfBlank(response.getSessionId(), "rag-" + UUID.randomUUID()))
                .mode(response.getMode())
                .questionType(response.getQuestionType())
                .answerSummary(response.getAnswerSummary())
                .conclusion(response.getConclusion())
                .possibleCauses(defaultList(response.getPossibleCauses()))
                .recommendations(defaultList(response.getRecommendations()))
                .priority(response.getPriority())
                .shouldManualReview(Boolean.TRUE.equals(response.getShouldManualReview()))
                .citedBusinessContext(businessCitation)
                .citedKnowledgeSources(knowledgeSources)
                .citedKnowledgeGraph(graphCitations)
                .fallback(StringUtils.isNotBlank(fallbackReason))
                .fallbackReason(fallbackReason)
                .build();
    }

    /**
     * 将 FastAPI 返回的图谱引用（含 subgraph）转换为前端 VO 列表。
     * 如果 FastAPI 返回空，则降级使用本地 kgSnippet.citations()。
     */
    @SuppressWarnings("unchecked")
    private List<RagGraphCitationVO> convertResponseKgToCitations(
            List<Map<String, Object>> responseKg,
            List<RagGraphCitationVO> fallback) {
        if (responseKg != null && !responseKg.isEmpty()) {
            return responseKg.stream().map(item -> {
                Object subgraphRaw = item.get("subgraph");
                Map<String, Object> subgraph = null;
                if (subgraphRaw instanceof Map<?, ?>) {
                    subgraph = (Map<String, Object>) subgraphRaw;
                }
                return RagGraphCitationVO.builder()
                        .id(String.valueOf(item.getOrDefault("id", "")))
                        .title(String.valueOf(item.getOrDefault("title", "")))
                        .kind(String.valueOf(item.getOrDefault("kind", "concept")))
                        .subgraph(subgraph)
                        .build();
            }).toList();
        }

        if (fallback != null && !fallback.isEmpty()) {
            return new ArrayList<>(fallback);
        }

        return new ArrayList<>();
    }

    private RagChatResponse buildFallbackResponse(RagChatRequest request,
                                                  Map<String, Object> businessContext,
                                                  String reason,
                                                  List<RagGraphCitationVO> citedKnowledgeGraph) {
        Map<String, Object> ctx = businessContext == null ? Map.of() : businessContext;
        Map<String, Object> currentWarning = castMap(ctx.get("current_warning"));
        List<String> summaryBlocks = extractStringList(ctx.get("summary_blocks"));
        List<String> riskSignals = extractStringList(ctx.get("risk_signals"));

        String warningType = currentWarning == null ? null : stringValue(currentWarning.get("warning_type"));
        String conclusion = StringUtils.isNotBlank(warningType)
                ? "已结合当前预警和业务上下文完成兜底分析，重点关注：" + warningType
                : "已根据当前提问生成本地兜底分析，建议结合最新预警与环境数据复核。";

        List<String> possibleCauses = new ArrayList<>();
        if (!riskSignals.isEmpty()) {
            possibleCauses.addAll(riskSignals.stream().limit(3).toList());
        } else {
            possibleCauses.add("当前业务上下文不足，可能需要进一步补充预警或设备关联信息");
            possibleCauses.add("建议结合最新环境监测数据确认异常是否持续");
        }

        List<String> recommendations = List.of(
                "优先核查当前关联农田与设备的最新状态",
                "结合知识库 SOP 执行标准处置步骤",
                "完成处置后及时更新预警状态并记录闭环结果"
        );

        RagBusinessCitationVO businessCitation = RagBusinessCitationVO.builder()
                .summaryBlocks(summaryBlocks)
                .riskSignals(riskSignals)
                .relatedWarningId(request.getRelatedWarningId())
                .relatedDeviceId(request.getRelatedDeviceId())
                .relatedFarmlandId(request.getRelatedFarmlandId())
                .build();

        return RagChatResponse.builder()
                .sessionId("fallback-" + UUID.randomUUID().toString().substring(0, 8))
                .mode(StringUtils.defaultIfBlank(request.getMode(), "auto"))
                .questionType(resolveFallbackQuestionType(request))
                .answerSummary("FastAPI RAG 服务暂不可用，已切换为 Spring Boot 本地兜底分析")
                .conclusion(conclusion)
                .possibleCauses(possibleCauses)
                .recommendations(recommendations)
                .priority(resolveFallbackPriority(currentWarning))
                .shouldManualReview(Boolean.TRUE)
                .citedBusinessContext(businessCitation)
                .citedKnowledgeSources(List.of())
                .citedKnowledgeGraph(citedKnowledgeGraph == null ? List.of() : citedKnowledgeGraph)
                .fallback(true)
                .fallbackReason(reason)
                .build();
    }

    private String resolveFallbackQuestionType(RagChatRequest request) {
        if ("business".equalsIgnoreCase(request.getMode())) {
            return "warning";
        }
        if ("knowledge".equalsIgnoreCase(request.getMode())) {
            return "knowledge";
        }
        if ("hybrid".equalsIgnoreCase(request.getMode())) {
            return "hybrid";
        }
        return request.getRelatedWarningId() != null ? "warning" : "hybrid";
    }

    private String resolveFallbackPriority(Map<String, Object> currentWarning) {
        if (currentWarning == null) {
            return "中";
        }
        String level = stringValue(currentWarning.get("level"));
        if (StringUtils.containsAny(level, "高", "HIGH")) {
            return "高";
        }
        if (StringUtils.containsAny(level, "低", "LOW")) {
            return "低";
        }
        return "中";
    }

    private Map<String, Object> buildBusinessContext(RagChatRequest request) {
        // 纯知识问答：前端不传关联预警/设备/农田时，不再用「库中最新预警」填充，避免答非所问
        if (isKnowledgeWithoutPinnedContext(request)) {
            return null;
        }
        Map<String, Object> context = new LinkedHashMap<>();
        WarningRecord currentWarning = resolveCurrentWarning(request);
        Long farmlandId = resolveFarmlandId(request, currentWarning);
        Long deviceId = resolveDeviceId(request, currentWarning);

        context.put("current_warning", buildWarningItem(currentWarning));
        context.put("recent_warnings", buildRecentWarnings(farmlandId));
        context.put("recent_env_data", buildRecentEnvData(farmlandId, deviceId));
        context.put("related_devices", buildRelatedDevices(farmlandId, deviceId));
        context.put("related_farmland", buildFarmlandItem(farmlandId));
        context.put("related_crops", buildRelatedCrops(farmlandId));
        context.put("process_logs", buildProcessLogs(currentWarning == null ? null : currentWarning.getId()));
        context.put("ai_analyses", buildAiAnalyses(currentWarning == null ? null : currentWarning.getId()));
        context.put("extra", buildExtraContext(request, currentWarning));
        context.put("summary_blocks", buildSummaryBlocks(context));
        context.put("risk_signals", buildRiskSignals(context));
        return context;
    }

    /**
     * 知识模式且未显式指定业务锚点时，不向 RAG 注入业务快照（否则易套用无关 DEVICE_OFFLINE 等记录）。
     */
    private boolean isKnowledgeWithoutPinnedContext(RagChatRequest request) {
        if (!"knowledge".equalsIgnoreCase(StringUtils.defaultString(request.getMode()))) {
            return false;
        }
        return request.getRelatedWarningId() == null
                && request.getRelatedDeviceId() == null
                && request.getRelatedFarmlandId() == null;
    }

    private WarningRecord resolveCurrentWarning(RagChatRequest request) {
        if (request.getRelatedWarningId() != null) {
            return warningMapper.selectById(request.getRelatedWarningId());
        }
        return warningMapper.selectOne(new LambdaQueryWrapper<WarningRecord>()
                .orderByDesc(WarningRecord::getTriggerTime)
                .orderByDesc(WarningRecord::getCreateTime)
                .last("limit 1"));
    }

    private Long resolveFarmlandId(RagChatRequest request, WarningRecord currentWarning) {
        if (request.getRelatedFarmlandId() != null) {
            return request.getRelatedFarmlandId();
        }
        if (currentWarning != null && currentWarning.getFarmlandId() != null) {
            return currentWarning.getFarmlandId();
        }
        if (request.getRelatedDeviceId() != null) {
            Device device = deviceMapper.selectById(request.getRelatedDeviceId());
            return device == null ? null : device.getFarmlandId();
        }
        return null;
    }

    private Long resolveDeviceId(RagChatRequest request, WarningRecord currentWarning) {
        if (request.getRelatedDeviceId() != null) {
            return request.getRelatedDeviceId();
        }
        return currentWarning == null ? null : currentWarning.getRelatedDeviceId();
    }

    private Map<String, Object> buildWarningItem(WarningRecord warning) {
        if (warning == null) {
            return null;
        }
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", warning.getId());
        item.put("warning_no", warning.getWarningNo());
        item.put("warning_type", warning.getWarningType());
        item.put("level", WarningLevelEnum.getDescriptionByCode(warning.getLevel()));
        item.put("status", WarningStatusEnum.getDescriptionByCode(warning.getStatus()));
        item.put("content", warning.getContent());
        item.put("trigger_time", formatDateTime(warning.getTriggerTime() == null ? warning.getCreateTime() : warning.getTriggerTime()));
        item.put("farmland_id", warning.getFarmlandId());
        item.put("related_device_id", warning.getRelatedDeviceId());
        item.put("related_crop_id", warning.getRelatedCropId());
        return item;
    }

    private List<Map<String, Object>> buildRecentWarnings(Long farmlandId) {
        return warningMapper.selectList(new LambdaQueryWrapper<WarningRecord>()
                        .eq(farmlandId != null, WarningRecord::getFarmlandId, farmlandId)
                        .orderByDesc(WarningRecord::getTriggerTime)
                        .orderByDesc(WarningRecord::getCreateTime)
                        .last("limit 5"))
                .stream()
                .map(this::buildWarningItem)
                .filter(Objects::nonNull)
                .toList();
    }

    private List<Map<String, Object>> buildRecentEnvData(Long farmlandId, Long deviceId) {
        return environmentDataMapper.selectList(new LambdaQueryWrapper<EnvironmentData>()
                        .eq(farmlandId != null, EnvironmentData::getFarmlandId, farmlandId)
                        .eq(deviceId != null, EnvironmentData::getDeviceId, deviceId)
                        .orderByDesc(EnvironmentData::getCollectTime)
                        .orderByDesc(EnvironmentData::getId)
                        .last("limit 5"))
                .stream()
                .map(item -> {
                    Map<String, Object> env = new LinkedHashMap<>();
                    env.put("id", item.getId());
                    env.put("farmland_id", item.getFarmlandId());
                    env.put("device_id", item.getDeviceId());
                    env.put("temperature", toDouble(item.getTemperature()));
                    env.put("humidity", toDouble(item.getHumidity()));
                    env.put("soil_moisture", toDouble(item.getSoilMoisture()));
                    env.put("light_intensity", toDouble(item.getLightIntensity()));
                    env.put("ph_value", toDouble(item.getPhValue()));
                    env.put("collect_time", formatDateTime(item.getCollectTime()));
                    return env;
                })
                .toList();
    }

    private List<Map<String, Object>> buildRelatedDevices(Long farmlandId, Long deviceId) {
        return deviceMapper.selectList(new LambdaQueryWrapper<Device>()
                        .eq(deviceId != null, Device::getId, deviceId)
                        .eq(deviceId == null && farmlandId != null, Device::getFarmlandId, farmlandId)
                        .orderByDesc(Device::getUpdateTime)
                        .last("limit 5"))
                .stream()
                .map(item -> {
                    Map<String, Object> device = new LinkedHashMap<>();
                    device.put("id", item.getId());
                    device.put("device_code", item.getDeviceCode());
                    device.put("device_name", item.getDeviceName());
                    device.put("device_type", item.getDeviceType());
                    device.put("online_status", item.getOnlineStatus());
                    device.put("run_status", item.getRunStatus());
                    device.put("farmland_id", item.getFarmlandId());
                    return device;
                })
                .toList();
    }

    private Map<String, Object> buildFarmlandItem(Long farmlandId) {
        if (farmlandId == null) {
            return null;
        }
        Field field = fieldMapper.selectById(farmlandId);
        if (field == null) {
            return null;
        }
        Map<String, Object> farmland = new LinkedHashMap<>();
        farmland.put("id", field.getId());
        farmland.put("field_name", field.getFieldName());
        farmland.put("location", field.getLocation());
        farmland.put("soil_type", field.getSoilType());
        farmland.put("status", field.getStatus());
        return farmland;
    }

    private List<Map<String, Object>> buildRelatedCrops(Long farmlandId) {
        if (farmlandId == null) {
            return List.of();
        }
        return cropMapper.selectList(new LambdaQueryWrapper<Crop>()
                        .eq(Crop::getFarmlandId, farmlandId)
                        .orderByDesc(Crop::getUpdateTime)
                        .last("limit 5"))
                .stream()
                .map(item -> {
                    Map<String, Object> crop = new LinkedHashMap<>();
                    crop.put("id", item.getId());
                    crop.put("crop_name", item.getCropName());
                    crop.put("crop_type", item.getCropType());
                    crop.put("growth_stage", item.getGrowthStage());
                    crop.put("health_status", item.getHealthStatus());
                    crop.put("farmland_id", item.getFarmlandId());
                    return crop;
                })
                .toList();
    }

    private List<Map<String, Object>> buildProcessLogs(Long warningId) {
        if (warningId == null) {
            return List.of();
        }
        return warningProcessLogMapper.selectList(new LambdaQueryWrapper<WarningProcessLog>()
                        .eq(WarningProcessLog::getWarningId, warningId)
                        .orderByDesc(WarningProcessLog::getProcessTime)
                        .orderByDesc(WarningProcessLog::getId)
                        .last("limit 6"))
                .stream()
                .map(item -> {
                    Map<String, Object> log = new LinkedHashMap<>();
                    log.put("id", item.getId());
                    log.put("warning_id", item.getWarningId());
                    log.put("action", item.getActionType());
                    log.put("operator_name", item.getOperatorName());
                    log.put("operation_time", formatDateTime(item.getProcessTime()));
                    log.put("remark", item.getProcessNote());
                    log.put("from_status", item.getStatusFrom() == null ? null : WarningStatusEnum.getDescriptionByCode(item.getStatusFrom()));
                    log.put("to_status", item.getStatusTo() == null ? null : WarningStatusEnum.getDescriptionByCode(item.getStatusTo()));
                    return log;
                })
                .toList();
    }

    private List<Map<String, Object>> buildAiAnalyses(Long warningId) {
        if (warningId == null) {
            return List.of();
        }
        return warningAiAnalysisMapper.selectList(new LambdaQueryWrapper<WarningAiAnalysis>()
                        .eq(WarningAiAnalysis::getWarningId, warningId)
                        .orderByDesc(WarningAiAnalysis::getCreateTime)
                        .orderByDesc(WarningAiAnalysis::getId)
                        .last("limit 3"))
                .stream()
                .map(item -> {
                    Map<String, Object> analysis = new LinkedHashMap<>();
                    analysis.put("id", item.getId());
                    analysis.put("warning_id", item.getWarningId());
                    analysis.put("summary", item.getAnalysisSummary());
                    analysis.put("suggestions", item.getSuggestionSummary());
                    analysis.put("priority", item.getPriorityLevel());
                    analysis.put("need_manual_review", item.getPriorityLevel() != null && item.getPriorityLevel() >= 3);
                    analysis.put("created_time", formatDateTime(item.getCreateTime()));
                    return analysis;
                })
                .toList();
    }

    private Map<String, Object> buildExtraContext(RagChatRequest request, WarningRecord currentWarning) {
        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("query", request.getQuery());
        extra.put("mode", request.getMode());
        extra.put("request_time", formatDateTime(LocalDateTime.now()));
        if (currentWarning != null) {
            extra.put("current_warning_status", WarningStatusEnum.getDescriptionByCode(currentWarning.getStatus()));
            extra.put("current_warning_level", WarningLevelEnum.getDescriptionByCode(currentWarning.getLevel()));
        }
        return extra;
    }

    private List<String> buildSummaryBlocks(Map<String, Object> context) {
        List<String> summaryBlocks = new ArrayList<>();
        Map<String, Object> currentWarning = castMap(context.get("current_warning"));
        if (currentWarning != null) {
            summaryBlocks.add("当前预警：" + stringValue(currentWarning.get("warning_type"))
                    + " / " + stringValue(currentWarning.get("level"))
                    + " / " + stringValue(currentWarning.get("status")));
        }
        List<Map<String, Object>> recentEnvData = castMapList(context.get("recent_env_data"));
        if (!recentEnvData.isEmpty()) {
            Map<String, Object> latestEnv = recentEnvData.get(0);
            summaryBlocks.add(String.format(Locale.ROOT, "环境快照：温度%s℃，湿度%s%%，土壤湿度%s%%",
                    safeMetric(latestEnv.get("temperature")),
                    safeMetric(latestEnv.get("humidity")),
                    safeMetric(latestEnv.get("soil_moisture"))));
        }
        List<Map<String, Object>> relatedDevices = castMapList(context.get("related_devices"));
        if (!relatedDevices.isEmpty()) {
            long offlineCount = relatedDevices.stream()
                    .filter(item -> Objects.equals(item.get("online_status"), 0))
                    .count();
            summaryBlocks.add("关联设备：" + relatedDevices.size() + " 台，离线 " + offlineCount + " 台");
        }
        Map<String, Object> farmland = castMap(context.get("related_farmland"));
        if (farmland != null) {
            summaryBlocks.add("所属农田：" + stringValue(farmland.get("field_name")));
        }
        return summaryBlocks;
    }

    private List<String> buildRiskSignals(Map<String, Object> context) {
        List<String> riskSignals = new ArrayList<>();
        Map<String, Object> currentWarning = castMap(context.get("current_warning"));
        if (currentWarning != null && StringUtils.containsAny(stringValue(currentWarning.get("level")), "高")) {
            riskSignals.add("存在高等级预警，需要优先处理");
        }
        List<Map<String, Object>> recentEnvData = castMapList(context.get("recent_env_data"));
        if (!recentEnvData.isEmpty()) {
            Map<String, Object> latestEnv = recentEnvData.get(0);
            Double soilMoisture = toDoubleValue(latestEnv.get("soil_moisture"));
            Double temperature = toDoubleValue(latestEnv.get("temperature"));
            if (soilMoisture != null && soilMoisture < 35D) {
                riskSignals.add("土壤湿度偏低");
            }
            if (temperature != null && temperature > 35D) {
                riskSignals.add("温度偏高");
            }
            if (temperature != null && temperature < 5D) {
                riskSignals.add("温度偏低");
            }
        }
        List<Map<String, Object>> relatedDevices = castMapList(context.get("related_devices"));
        long offlineCount = relatedDevices.stream()
                .filter(item -> Objects.equals(item.get("online_status"), 0))
                .count();
        if (offlineCount > 0) {
            riskSignals.add("存在离线设备，建议联动巡检");
        }
        return riskSignals;
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? null : value.format(DATE_TIME_FORMATTER);
    }

    private Double toDouble(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }

    private String safeMetric(Object value) {
        Double numeric = toDoubleValue(value);
        return numeric == null ? "-" : String.format(Locale.ROOT, "%.1f", numeric);
    }

    private Double toDoubleValue(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text && StringUtils.isNotBlank(text)) {
            try {
                return Double.parseDouble(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : null;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castMapList(Object value) {
        return value instanceof List<?> list ? (List<Map<String, Object>>) list : List.of();
    }

    private List<String> extractStringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream().filter(Objects::nonNull).map(String::valueOf).toList();
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private List<String> defaultList(List<String> values) {
        return values == null ? List.of() : values;
    }
}
