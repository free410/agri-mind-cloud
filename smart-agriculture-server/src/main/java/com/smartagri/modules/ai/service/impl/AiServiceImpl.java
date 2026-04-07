package com.smartagri.modules.ai.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartagri.common.exception.BusinessException;
import com.smartagri.common.properties.AiProperties;
import com.smartagri.common.result.ResultCode;
import com.smartagri.modules.ai.adapter.AiModelAdapter;
import com.smartagri.modules.ai.adapter.AiModelRequest;
import com.smartagri.modules.ai.adapter.AiModelResponse;
import com.smartagri.modules.ai.dto.AnalyzeRequest;
import com.smartagri.modules.ai.dto.ChatRequest;
import com.smartagri.modules.ai.entity.AiChatLog;
import com.smartagri.modules.ai.mapper.AiChatLogMapper;
import com.smartagri.modules.ai.prompt.AiPromptBuilder;
import com.smartagri.modules.ai.prompt.AiPromptContext;
import com.smartagri.modules.ai.service.AiService;
import com.smartagri.modules.ai.vo.AnalyzeResponse;
import com.smartagri.modules.ai.vo.ChatResponse;
import com.smartagri.modules.ai.vo.WarningAiAnalysisVO;
import com.smartagri.modules.ai.vo.WarningDisposalSuggestionVO;
import com.smartagri.modules.ai.vo.WarningQuickSummaryVO;
import com.smartagri.modules.warning.entity.WarningAiAnalysis;
import com.smartagri.modules.warning.entity.WarningRecord;
import com.smartagri.modules.warning.enums.WarningLevelEnum;
import com.smartagri.modules.warning.enums.WarningStatusEnum;
import com.smartagri.modules.warning.mapper.WarningAiAnalysisMapper;
import com.smartagri.modules.warning.service.WarningService;
import com.smartagri.security.LoginAdminContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
public class AiServiceImpl extends ServiceImpl<AiChatLogMapper, AiChatLog> implements AiService {

    private static final DateTimeFormatter SESSION_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final AiModelAdapter aiModelAdapter;
    private final AiPromptBuilder aiPromptBuilder;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;
    private final WarningService warningService;
    private final WarningAiAnalysisMapper warningAiAnalysisMapper;

    public AiServiceImpl(AiModelAdapter aiModelAdapter,
                         AiPromptBuilder aiPromptBuilder,
                         AiProperties aiProperties,
                         ObjectMapper objectMapper,
                         WarningService warningService,
                         WarningAiAnalysisMapper warningAiAnalysisMapper) {
        this.aiModelAdapter = aiModelAdapter;
        this.aiPromptBuilder = aiPromptBuilder;
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;
        this.warningService = warningService;
        this.warningAiAnalysisMapper = warningAiAnalysisMapper;
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        String sessionId = resolveSessionId(request.getSessionId());
        LocalDateTime startTime = LocalDateTime.now();
        AiPromptContext promptContext = aiPromptBuilder.buildChatPrompt(request);

        try {
            AiModelResponse response = aiModelAdapter.chat(buildModelRequest(promptContext));
            LocalDateTime createTime = LocalDateTime.now();
            saveChatLog(sessionId, promptContext, response.getContent(), response.getModelName(), startTime, createTime, 1);

            return ChatResponse.builder()
                    .sessionId(sessionId)
                    .question(promptContext.getDisplayQuestion())
                    .answer(response.getContent())
                    .relatedModule(promptContext.getRelatedModule())
                    .relatedRecordId(promptContext.getRelatedRecordId())
                    .modelName(response.getModelName())
                    .createTime(createTime)
                    .build();
        } catch (Exception exception) {
            log.error("AI 对话调用失败", exception);
            saveChatLog(sessionId, promptContext, "AI 服务调用失败：" + exception.getMessage(),
                    aiProperties.getModelName(), startTime, LocalDateTime.now(), 0);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "AI 对话服务暂时不可用，请稍后重试");
        }
    }

    @Override
    public AnalyzeResponse analyze(AnalyzeRequest request) {
        String sessionId = resolveSessionId(request.getSessionId());
        LocalDateTime startTime = LocalDateTime.now();
        AiPromptContext promptContext = aiPromptBuilder.buildAnalyzePrompt(request);

        try {
            AiModelResponse response = aiModelAdapter.chat(buildModelRequest(promptContext));
            LocalDateTime createTime = LocalDateTime.now();
            saveChatLog(sessionId, promptContext, response.getContent(), response.getModelName(), startTime, createTime, 1);

            return AnalyzeResponse.builder()
                    .sessionId(sessionId)
                    .analysisType(promptContext.getRelatedModule())
                    .title(promptContext.getTitle())
                    .answer(response.getContent())
                    .referencedData(promptContext.getMetadata())
                    .modelName(response.getModelName())
                    .createTime(createTime)
                    .build();
        } catch (Exception exception) {
            log.error("AI 分析调用失败", exception);
            saveChatLog(sessionId, promptContext, "AI 服务调用失败：" + exception.getMessage(),
                    aiProperties.getModelName(), startTime, LocalDateTime.now(), 0);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "AI 分析服务暂时不可用，请稍后重试");
        }
    }

    @Override
    public WarningAiAnalysisVO analyzeWarning(Long warningId) {
        AiPromptContext promptContext = aiPromptBuilder.buildWarningStructuredAnalyzePrompt(warningId);
        LocalDateTime createTime = LocalDateTime.now();

        try {
            AiModelResponse response = aiModelAdapter.chat(buildModelRequest(promptContext));
            WarningAiAnalysisVO vo = parseWarningAnalysisResponse(warningId, response.getContent(), response.getModelName(), promptContext);
            persistWarningAiAnalysis(warningId, response.getModelName(), vo, response.getContent(), null);
            saveChatLog(resolveSessionId(null), promptContext, response.getContent(), response.getModelName(), createTime, createTime, 1);
            return vo;
        } catch (Exception exception) {
            log.warn("预警AI分析调用失败，warningId={}, 将使用本地mock兜底。原因={}", warningId, exception.getMessage());
            WarningAiAnalysisVO fallback = buildFallbackWarningAnalysis(warningId, promptContext);
            persistWarningAiAnalysis(warningId, "local-mock-fallback", fallback, "fallback", null);
            return fallback;
        }
    }

    @Override
    public WarningDisposalSuggestionVO suggestWarningDisposal(Long warningId) {
        AiPromptContext promptContext = aiPromptBuilder.buildWarningDisposalPrompt(warningId);
        LocalDateTime createTime = LocalDateTime.now();

        try {
            AiModelResponse response = aiModelAdapter.chat(buildModelRequest(promptContext));
            WarningDisposalSuggestionVO vo = parseWarningDisposalResponse(warningId, response.getContent(), response.getModelName(), promptContext);
            persistWarningAiAnalysis(warningId, response.getModelName(), null, response.getContent(), vo);
            saveChatLog(resolveSessionId(null), promptContext, response.getContent(), response.getModelName(), createTime, createTime, 1);
            return vo;
        } catch (Exception exception) {
            log.warn("预警处置建议调用失败，warningId={}, 将使用本地mock兜底。原因={}", warningId, exception.getMessage());
            WarningDisposalSuggestionVO fallback = buildFallbackWarningDisposal(warningId, promptContext);
            persistWarningAiAnalysis(warningId, "local-mock-fallback", null, "fallback", fallback);
            return fallback;
        }
    }

    @Override
    public WarningQuickSummaryVO getWarningQuickSummary(String mode) {
        String safeMode = StringUtils.defaultIfBlank(mode, "abnormal").toLowerCase();
        if (!"abnormal".equals(safeMode) && !"closure".equals(safeMode)) {
            safeMode = "abnormal";
        }

        List<WarningRecord> warningList = warningService.list();
        LocalDate latestDate = warningList.stream()
                .map(this::resolveWarningDate)
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElse(LocalDate.now());
        List<WarningRecord> todayWarnings = warningList.stream()
                .filter(item -> latestDate.equals(resolveWarningDate(item)))
                .toList();

        long total = todayWarnings.size();
        long high = todayWarnings.stream()
                .filter(item -> Objects.equals(item.getLevel(), WarningLevelEnum.HIGH.getCode()))
                .count();
        long pending = todayWarnings.stream()
                .filter(item -> Objects.equals(item.getStatus(), WarningStatusEnum.TO_CONFIRM.getCode())
                        || Objects.equals(item.getStatus(), WarningStatusEnum.PROCESSING.getCode()))
                .count();
        long closed = todayWarnings.stream()
                .filter(item -> Objects.equals(item.getStatus(), WarningStatusEnum.CLOSED_LOOP.getCode()))
                .count();
        double closureRate = total == 0 ? 0D : (closed * 100.0 / total);
        long avgClosureMinutes = calculateAvgClosureMinutes(todayWarnings);

        WarningRecord relatedWarning = todayWarnings.stream()
                .filter(item -> Objects.equals(item.getLevel(), WarningLevelEnum.HIGH.getCode()))
                .findFirst()
                .orElse(todayWarnings.stream().findFirst().orElse(null));

        if ("closure".equals(safeMode)) {
            return WarningQuickSummaryVO.builder()
                    .mode("closure")
                    .title("今日闭环情况总结")
                    .conclusion(buildClosureConclusion(total, closed, closureRate, avgClosureMinutes))
                    .possibleCauses(buildClosurePossibleCauses(pending, closureRate, avgClosureMinutes))
                    .suggestions(buildClosureSuggestions(pending, closureRate, avgClosureMinutes))
                    .priorityLevel(resolveClosurePriority(closureRate, pending))
                    .manualReviewRequired(closureRate < 60 || pending > 0)
                    .relatedWarningId(relatedWarning == null ? null : relatedWarning.getId())
                    .totalWarnings(total)
                    .highRiskWarnings(high)
                    .pendingWarnings(pending)
                    .closedWarnings(closed)
                    .closureRate(roundOneDecimal(closureRate))
                    .avgClosureMinutes(avgClosureMinutes)
                    .generateTime(LocalDateTime.now())
                    .build();
        }

        return WarningQuickSummaryVO.builder()
                .mode("abnormal")
                .title("今日异常简报")
                .conclusion(buildAbnormalConclusion(total, high, pending, closed))
                .possibleCauses(buildAbnormalPossibleCauses(high, pending))
                .suggestions(buildAbnormalSuggestions(high, pending))
                .priorityLevel(resolveAbnormalPriority(high, pending))
                .manualReviewRequired(high > 0)
                .relatedWarningId(relatedWarning == null ? null : relatedWarning.getId())
                .totalWarnings(total)
                .highRiskWarnings(high)
                .pendingWarnings(pending)
                .closedWarnings(closed)
                .closureRate(roundOneDecimal(closureRate))
                .avgClosureMinutes(avgClosureMinutes)
                .generateTime(LocalDateTime.now())
                .build();
    }

    private AiModelRequest buildModelRequest(AiPromptContext promptContext) {
        return AiModelRequest.builder()
                .scenario(promptContext.getScenario())
                .systemPrompt(promptContext.getSystemPrompt())
                .userPrompt(promptContext.getUserPrompt())
                .metadata(promptContext.getMetadata())
                .build();
    }

    private WarningAiAnalysisVO parseWarningAnalysisResponse(Long warningId,
                                                             String rawContent,
                                                             String modelName,
                                                             AiPromptContext promptContext) {
        JsonNode jsonNode = parseJsonNode(rawContent);
        if (jsonNode == null) {
            return buildFallbackWarningAnalysis(warningId, promptContext);
        }

        List<String> possibleCauses = toStringList(jsonNode.get("possibleCauses"));
        List<String> recommendations = toStringList(jsonNode.get("recommendations"));
        Integer priorityLevel = jsonNode.has("priorityLevel") ? jsonNode.get("priorityLevel").asInt(2) : 2;
        boolean manualReviewRequired = jsonNode.has("manualReviewRequired") && jsonNode.get("manualReviewRequired").asBoolean(false);

        return WarningAiAnalysisVO.builder()
                .warningId(warningId)
                .warningOverview(readText(jsonNode, "warningOverview", "当前预警需要重点关注。"))
                .possibleCauses(defaultList(possibleCauses, List.of("监测指标异常波动", "设备状态与环境变化耦合")))
                .riskImpact(readText(jsonNode, "riskImpact", "若不及时处理，可能对作物健康和产量造成影响。"))
                .recommendations(defaultList(recommendations, List.of("先核查现场状态", "执行处置后复测确认")))
                .priorityLevel(priorityLevel)
                .manualReviewRequired(manualReviewRequired)
                .modelName(StringUtils.defaultIfBlank(modelName, aiProperties.getModelName()))
                .createTime(LocalDateTime.now())
                .build();
    }

    private WarningDisposalSuggestionVO parseWarningDisposalResponse(Long warningId,
                                                                     String rawContent,
                                                                     String modelName,
                                                                     AiPromptContext promptContext) {
        JsonNode jsonNode = parseJsonNode(rawContent);
        if (jsonNode == null) {
            return buildFallbackWarningDisposal(warningId, promptContext);
        }

        List<String> steps = toStringList(jsonNode.get("steps"));
        List<String> checkTargets = toStringList(jsonNode.get("checkTargets"));
        boolean needDeviceInspection = jsonNode.has("needDeviceInspection")
                && jsonNode.get("needDeviceInspection").asBoolean(false);

        return WarningDisposalSuggestionVO.builder()
                .warningId(warningId)
                .steps(defaultList(steps, List.of("确认异常是否持续", "执行对应处置动作", "复测并闭环记录")))
                .checkTargets(defaultList(checkTargets, List.of("关联农田环境指标", "关键设备运行状态")))
                .needDeviceInspection(needDeviceInspection)
                .summary(readText(jsonNode, "summary", "建议按“确认异常-执行处置-复测闭环”的流程处理。"))
                .modelName(StringUtils.defaultIfBlank(modelName, aiProperties.getModelName()))
                .createTime(LocalDateTime.now())
                .build();
    }

    private WarningAiAnalysisVO buildFallbackWarningAnalysis(Long warningId, AiPromptContext promptContext) {
        WarningRecord warningRecord = warningService.getById(warningId);
        int priority = warningRecord == null || warningRecord.getLevel() == null ? 2 : warningRecord.getLevel();
        return WarningAiAnalysisVO.builder()
                .warningId(warningId)
                .warningOverview("基于当前预警信息，系统识别到该风险需尽快处理。")
                .possibleCauses(List.of("监测指标持续异常", "设备运行状态可能波动", "现场管理动作响应不及时"))
                .riskImpact("若处置延迟，可能导致作物生长受限并提高后续预警频次（基于当前数据判断）。")
                .recommendations(List.of("优先确认异常是否持续", "执行对应设备/农事处置", "30分钟内复测关键指标"))
                .priorityLevel(priority)
                .manualReviewRequired(priority >= 3)
                .modelName("local-mock-fallback")
                .createTime(LocalDateTime.now())
                .build();
    }

    private WarningDisposalSuggestionVO buildFallbackWarningDisposal(Long warningId, AiPromptContext promptContext) {
        String warningType = promptContext.getMetadata() == null ? null : String.valueOf(promptContext.getMetadata().get("warningType"));
        boolean needInspection = StringUtils.containsIgnoreCase(warningType, "DEVICE");
        return WarningDisposalSuggestionVO.builder()
                .warningId(warningId)
                .steps(List.of(
                        "步骤1：复核最近两条监测数据并确认异常持续",
                        "步骤2：按预警类型执行处置（灌溉/通风/设备复位）",
                        "步骤3：处置后补采数据并更新预警状态"
                ))
                .checkTargets(List.of("农田实时环境数据", "关联设备在线状态", "相关执行设备动作记录"))
                .needDeviceInspection(needInspection)
                .summary("建议按标准闭环流程处理并保留处置记录，便于赛场演示闭环结果。")
                .modelName("local-mock-fallback")
                .createTime(LocalDateTime.now())
                .build();
    }

    private void persistWarningAiAnalysis(Long warningId,
                                          String analysisSource,
                                          WarningAiAnalysisVO analysisVO,
                                          String rawResponse,
                                          WarningDisposalSuggestionVO disposalVO) {
        WarningAiAnalysis record = new WarningAiAnalysis();
        record.setWarningId(warningId);
        record.setAnalysisSource(StringUtils.defaultIfBlank(analysisSource, aiProperties.getProvider()));
        if (analysisVO != null) {
            record.setAnalysisSummary(buildAnalysisSummary(analysisVO));
            record.setSuggestionSummary(String.join("；", analysisVO.getRecommendations()));
            record.setRiskScore(toRiskScore(analysisVO.getPriorityLevel()));
            record.setPriorityLevel(analysisVO.getPriorityLevel());
        } else if (disposalVO != null) {
            record.setAnalysisSummary("处置建议生成");
            record.setSuggestionSummary(disposalVO.getSummary() + " | 步骤：" + String.join(" -> ", disposalVO.getSteps()));
            record.setPriorityLevel(2);
        }
        record.setRawResponse(rawResponse);
        warningAiAnalysisMapper.insert(record);

        WarningRecord warningRecord = warningService.getById(warningId);
        if (warningRecord != null) {
            if (analysisVO != null) {
                warningRecord.setAiAnalysisSummary(buildAnalysisSummary(analysisVO));
                warningRecord.setAiSuggestionSummary(String.join("；", analysisVO.getRecommendations()));
            } else if (disposalVO != null) {
                warningRecord.setAiSuggestionSummary(disposalVO.getSummary());
            }
            warningService.updateById(warningRecord);
        }
    }

    private String buildAnalysisSummary(WarningAiAnalysisVO analysisVO) {
        return analysisVO.getWarningOverview() + " | 风险影响：" + analysisVO.getRiskImpact();
    }

    private int toRiskScore(Integer priorityLevel) {
        if (priorityLevel == null) {
            return 60;
        }
        return switch (priorityLevel) {
            case 1 -> 45;
            case 2 -> 70;
            case 3 -> 90;
            default -> 60;
        };
    }

    private JsonNode parseJsonNode(String rawContent) {
        if (StringUtils.isBlank(rawContent)) {
            return null;
        }
        String jsonText = extractJsonText(rawContent);
        try {
            return objectMapper.readTree(jsonText);
        } catch (Exception exception) {
            return null;
        }
    }

    private String extractJsonText(String rawContent) {
        String trimmed = rawContent.trim();
        int firstBrace = trimmed.indexOf('{');
        int lastBrace = trimmed.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            return trimmed.substring(firstBrace, lastBrace + 1);
        }
        return trimmed;
    }

    private String readText(JsonNode node, String field, String defaultValue) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return defaultValue;
        }
        String value = node.get(field).asText();
        return StringUtils.defaultIfBlank(value, defaultValue);
    }

    private List<String> toStringList(JsonNode node) {
        if (node == null || !node.isArray()) {
            return Collections.emptyList();
        }
        List<String> values = new ArrayList<>();
        node.forEach(item -> {
            if (item != null && !item.isNull() && StringUtils.isNotBlank(item.asText())) {
                values.add(item.asText());
            }
        });
        return values;
    }

    private List<String> defaultList(List<String> values, List<String> defaults) {
        return values == null || values.isEmpty() ? defaults : values;
    }

    private LocalDate resolveWarningDate(WarningRecord warningRecord) {
        if (warningRecord == null) {
            return null;
        }
        if (warningRecord.getTriggerTime() != null) {
            return warningRecord.getTriggerTime().toLocalDate();
        }
        if (warningRecord.getCreateTime() != null) {
            return warningRecord.getCreateTime().toLocalDate();
        }
        return LocalDate.now();
    }

    private long calculateAvgClosureMinutes(List<WarningRecord> warningList) {
        if (warningList == null || warningList.isEmpty()) {
            return 0L;
        }
        List<Long> durations = warningList.stream()
                .filter(item -> Objects.equals(item.getStatus(), WarningStatusEnum.CLOSED_LOOP.getCode()))
                .filter(item -> item.getCloseTime() != null)
                .map(item -> {
                    LocalDateTime start = item.getTriggerTime() == null ? item.getCreateTime() : item.getTriggerTime();
                    if (start == null) {
                        return 0L;
                    }
                    return Math.max(0L, Duration.between(start, item.getCloseTime()).toMinutes());
                })
                .toList();
        if (durations.isEmpty()) {
            return 0L;
        }
        long total = durations.stream().mapToLong(Long::longValue).sum();
        return Math.round(total * 1.0 / durations.size());
    }

    private String buildAbnormalConclusion(long total, long high, long pending, long closed) {
        return String.format("今日识别预警 %d 条，其中高风险 %d 条，待处理 %d 条，已闭环 %d 条。", total, high, pending, closed);
    }

    private List<String> buildAbnormalPossibleCauses(long high, long pending) {
        List<String> causes = new ArrayList<>();
        if (high > 0) {
            causes.add("高风险预警仍然存在，说明关键风险点尚未完全处置。");
        }
        if (pending > 0) {
            causes.add("待处理预警数量较高，处置链路推进速度需要提升。");
        }
        causes.add("巡检触发与环境波动可能叠加，建议结合设备状态复核异常源。");
        return causes;
    }

    private List<String> buildAbnormalSuggestions(long high, long pending) {
        List<String> suggestions = new ArrayList<>();
        suggestions.add("优先处理高风险预警并补充处理说明，确保可追溯。");
        if (pending > 0) {
            suggestions.add("按“待确认 -> 处理中 -> 已处理 -> 已闭环”推进状态流转。");
        }
        suggestions.add("必要时联动设备巡检，避免同类预警重复触发。");
        return suggestions;
    }

    private Integer resolveAbnormalPriority(long high, long pending) {
        if (high > 0) {
            return 3;
        }
        if (pending > 2) {
            return 2;
        }
        return 1;
    }

    private String buildClosureConclusion(long total, long closed, double closureRate, long avgClosureMinutes) {
        return String.format("今日闭环 %d/%d 条，闭环率 %.1f%%，平均闭环时长约 %d 分钟。", closed, total, closureRate, avgClosureMinutes);
    }

    private List<String> buildClosurePossibleCauses(long pending, double closureRate, long avgClosureMinutes) {
        List<String> causes = new ArrayList<>();
        if (closureRate < 60) {
            causes.add("闭环率偏低，可能存在处理中预警未及时提交结果。");
        }
        if (pending > 0) {
            causes.add("仍有待处理预警，闭环动作未完成。");
        }
        if (avgClosureMinutes > 180) {
            causes.add("平均闭环时长偏长，说明处置协同效率仍有提升空间。");
        }
        if (causes.isEmpty()) {
            causes.add("当前闭环节奏整体稳定，可继续保持。");
        }
        return causes;
    }

    private List<String> buildClosureSuggestions(long pending, double closureRate, long avgClosureMinutes) {
        List<String> suggestions = new ArrayList<>();
        if (pending > 0) {
            suggestions.add("优先清理待处理预警，确保当日事件尽量当日闭环。");
        }
        if (closureRate < 70) {
            suggestions.add("对高频预警类型建立标准处置模板，缩短操作路径。");
        }
        if (avgClosureMinutes > 180) {
            suggestions.add("针对长耗时预警增加巡检联动和责任人提醒机制。");
        }
        if (suggestions.isEmpty()) {
            suggestions.add("维持当前闭环流程，并持续记录处理日志用于复盘。");
        }
        return suggestions;
    }

    private Integer resolveClosurePriority(double closureRate, long pending) {
        if (closureRate < 50 || pending > 2) {
            return 3;
        }
        if (closureRate < 80 || pending > 0) {
            return 2;
        }
        return 1;
    }

    private double roundOneDecimal(double value) {
        return Math.round(value * 10D) / 10D;
    }

    private void saveChatLog(String sessionId,
                             AiPromptContext promptContext,
                             String answer,
                             String modelName,
                             LocalDateTime startTime,
                             LocalDateTime createTime,
                             Integer responseStatus) {
        AiChatLog aiChatLog = new AiChatLog();
        aiChatLog.setAdminId(resolveAdminId());
        aiChatLog.setSessionId(sessionId);
        aiChatLog.setQuestion(promptContext.getDisplayQuestion());
        aiChatLog.setAnswer(answer);
        aiChatLog.setRelatedModule(promptContext.getRelatedModule());
        aiChatLog.setRelatedRecordId(promptContext.getRelatedRecordId());
        aiChatLog.setModelName(StringUtils.defaultIfBlank(modelName, aiProperties.getModelName()));
        aiChatLog.setContextSnapshot(promptContext.getContextSnapshot());
        aiChatLog.setResponseStatus(responseStatus);
        aiChatLog.setResponseTimeMs((int) Duration.between(startTime, createTime).toMillis());
        save(aiChatLog);
    }

    private Long resolveAdminId() {
        return LoginAdminContext.getAdminId() == null ? 1L : LoginAdminContext.getAdminId();
    }

    private String resolveSessionId(String sessionId) {
        if (StringUtils.isNotBlank(sessionId)) {
            return sessionId;
        }
        return "session-" + LocalDateTime.now().format(SESSION_TIME_FORMATTER)
                + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
