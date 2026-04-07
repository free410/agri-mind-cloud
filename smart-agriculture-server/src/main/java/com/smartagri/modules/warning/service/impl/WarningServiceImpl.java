package com.smartagri.modules.warning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartagri.common.exception.BusinessException;
import com.smartagri.common.result.ResultCode;
import com.smartagri.modules.crop.entity.Crop;
import com.smartagri.modules.crop.service.CropService;
import com.smartagri.modules.device.entity.Device;
import com.smartagri.modules.device.service.DeviceService;
import com.smartagri.modules.environment.entity.EnvironmentData;
import com.smartagri.modules.environment.mapper.EnvironmentDataMapper;
import com.smartagri.modules.field.entity.Field;
import com.smartagri.modules.field.service.FieldService;
import com.smartagri.modules.warning.entity.WarningAiAnalysis;
import com.smartagri.modules.warning.dto.WarningFalseAlarmRequest;
import com.smartagri.modules.warning.dto.WarningFlowActionRequest;
import com.smartagri.modules.warning.dto.WarningHandleSubmitRequest;
import com.smartagri.modules.warning.dto.WarningPageQueryRequest;
import com.smartagri.modules.warning.dto.WarningStatusUpdateRequest;
import com.smartagri.modules.warning.entity.WarningProcessLog;
import com.smartagri.modules.warning.entity.WarningRecord;
import com.smartagri.modules.warning.enums.WarningLevelEnum;
import com.smartagri.modules.warning.enums.WarningProcessActionEnum;
import com.smartagri.modules.warning.enums.WarningStatusEnum;
import com.smartagri.modules.warning.enums.WarningTriggerSourceEnum;
import com.smartagri.modules.warning.enums.WarningTypeEnum;
import com.smartagri.modules.warning.mapper.WarningMapper;
import com.smartagri.modules.warning.mapper.WarningAiAnalysisMapper;
import com.smartagri.modules.warning.mapper.WarningProcessLogMapper;
import com.smartagri.modules.warning.rule.WarningRuleEngineService;
import com.smartagri.modules.warning.rule.WarningTriggerCommand;
import com.smartagri.modules.warning.service.WarningService;
import com.smartagri.modules.warning.vo.WarningDetailVO;
import com.smartagri.modules.warning.vo.WarningDisposalReportVO;
import com.smartagri.modules.warning.vo.WarningFlowVO;
import com.smartagri.modules.warning.vo.WarningListVO;
import com.smartagri.modules.warning.vo.WarningProcessLogVO;
import com.smartagri.modules.warning.vo.WarningReportEnvSnapshotVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class WarningServiceImpl extends ServiceImpl<WarningMapper, WarningRecord> implements WarningService {

    private static final DateTimeFormatter WARNING_NO_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final FieldService fieldService;
    private final CropService cropService;
    private final DeviceService deviceService;
    private final EnvironmentDataMapper environmentDataMapper;
    private final WarningAiAnalysisMapper warningAiAnalysisMapper;
    private final WarningProcessLogMapper warningProcessLogMapper;
    private final WarningRuleEngineService warningRuleEngineService;

    public WarningServiceImpl(FieldService fieldService,
                              CropService cropService,
                              DeviceService deviceService,
                              EnvironmentDataMapper environmentDataMapper,
                              WarningAiAnalysisMapper warningAiAnalysisMapper,
                              WarningProcessLogMapper warningProcessLogMapper,
                              WarningRuleEngineService warningRuleEngineService) {
        this.fieldService = fieldService;
        this.cropService = cropService;
        this.deviceService = deviceService;
        this.environmentDataMapper = environmentDataMapper;
        this.warningAiAnalysisMapper = warningAiAnalysisMapper;
        this.warningProcessLogMapper = warningProcessLogMapper;
        this.warningRuleEngineService = warningRuleEngineService;
    }

    @Override
    public IPage<WarningListVO> pageWarnings(WarningPageQueryRequest request) {
        validateQueryParams(request.getLevel(), request.getStatus(), request.getTriggerSource());

        Page<WarningRecord> page = new Page<>(request.getCurrent(), request.getSize());
        LambdaQueryWrapper<WarningRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(request.getLevel() != null, WarningRecord::getLevel, request.getLevel())
                .eq(request.getStatus() != null, WarningRecord::getStatus, request.getStatus())
                .eq(request.getFarmlandId() != null, WarningRecord::getFarmlandId, request.getFarmlandId())
                .eq(StringUtils.hasText(request.getTriggerSource()), WarningRecord::getTriggerSource, request.getTriggerSource())
                .ge(request.getTriggerStartTime() != null, WarningRecord::getTriggerTime, request.getTriggerStartTime())
                .le(request.getTriggerEndTime() != null, WarningRecord::getTriggerTime, request.getTriggerEndTime())
                .orderByDesc(WarningRecord::getTriggerTime)
                .orderByDesc(WarningRecord::getCreateTime);

        Page<WarningRecord> warningPage = page(page, queryWrapper);
        Map<Long, Field> fieldMap = loadFieldMap(warningPage.getRecords());
        Map<Long, Device> deviceMap = loadDeviceMap(warningPage.getRecords());

        Page<WarningListVO> resultPage = new Page<>(warningPage.getCurrent(), warningPage.getSize(), warningPage.getTotal());
        resultPage.setRecords(warningPage.getRecords().stream()
                .map(item -> buildListVO(item, fieldMap.get(item.getFarmlandId()), deviceMap.get(item.getRelatedDeviceId())))
                .toList());
        return resultPage;
    }

    @Override
    public WarningDetailVO getWarningDetail(Long id) {
        WarningRecord warning = getWarningOrThrow(id);
        Field field = warning.getFarmlandId() == null ? null : fieldService.getById(warning.getFarmlandId());
        Device device = warning.getRelatedDeviceId() == null ? null : deviceService.getById(warning.getRelatedDeviceId());
        return buildDetailVO(warning, field, device);
    }

    @Override
    public void updateWarningStatus(WarningStatusUpdateRequest request) {
        if (!WarningStatusEnum.contains(request.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "预警状态取值不合法");
        }

        WarningRecord warning = getWarningOrThrow(request.getId());
        String operatorName = resolveOperatorName(request.getHandlerName());
        String remark = StringUtils.trimWhitespace(request.getHandleRemark());

        if (Integer.valueOf(1).equals(request.getIsFalseAlarm())) {
            markFalseAlarmInternal(warning, operatorName, StringUtils.hasText(remark) ? remark : "管理员标记误报");
            return;
        }

        applyStatusTransition(warning, request.getStatus(), WarningProcessActionEnum.STATUS_UPDATE.getCode(),
                operatorName, remark, false);
    }

    @Override
    public void confirmWarning(Long id, WarningFlowActionRequest request) {
        WarningRecord warning = getWarningOrThrow(id);
        if (!Objects.equals(warning.getStatus(), WarningStatusEnum.TO_CONFIRM.getCode())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "仅待确认状态允许执行确认操作");
        }

        String operator = resolveOperatorName(request.getOperatorName());
        warning.setHandlerName(operator);
        updateById(warning);

        saveProcessLog(warning.getId(), WarningProcessActionEnum.CONFIRM.getCode(),
                warning.getStatus(), warning.getStatus(), operator, request.getProcessNote(), LocalDateTime.now());
    }

    @Override
    public void markProcessing(Long id, WarningFlowActionRequest request) {
        WarningRecord warning = getWarningOrThrow(id);
        applyStatusTransition(warning, WarningStatusEnum.PROCESSING.getCode(),
                WarningProcessActionEnum.START_PROCESS.getCode(),
                resolveOperatorName(request.getOperatorName()),
                request.getProcessNote(), false);
    }

    @Override
    public void submitHandleResult(Long id, WarningHandleSubmitRequest request) {
        WarningRecord warning = getWarningOrThrow(id);
        applyStatusTransition(warning, WarningStatusEnum.HANDLED.getCode(),
                WarningProcessActionEnum.SUBMIT_RESULT.getCode(),
                resolveOperatorName(request.getOperatorName()),
                request.getHandleRemark(), false);
    }

    @Override
    public void closeWarning(Long id, WarningFlowActionRequest request) {
        WarningRecord warning = getWarningOrThrow(id);
        applyStatusTransition(warning, WarningStatusEnum.CLOSED_LOOP.getCode(),
                WarningProcessActionEnum.CLOSE_LOOP.getCode(),
                resolveOperatorName(request.getOperatorName()),
                request.getProcessNote(), false);
    }

    @Override
    public void markFalseAlarm(Long id, WarningFalseAlarmRequest request) {
        WarningRecord warning = getWarningOrThrow(id);
        markFalseAlarmInternal(warning, resolveOperatorName(request.getOperatorName()), request.getFalseAlarmReason());
    }

    @Override
    public WarningFlowVO getWarningFlow(Long id) {
        WarningRecord warning = getWarningOrThrow(id);
        List<WarningProcessLog> logs = warningProcessLogMapper.selectList(new LambdaQueryWrapper<WarningProcessLog>()
                .eq(WarningProcessLog::getWarningId, id)
                .orderByAsc(WarningProcessLog::getProcessTime)
                .orderByAsc(WarningProcessLog::getId));

        List<WarningProcessLogVO> logVOS = logs.stream()
                .map(this::toProcessLogVO)
                .toList();

        return WarningFlowVO.builder()
                .warningId(warning.getId())
                .warningNo(warning.getWarningNo())
                .currentStatus(warning.getStatus())
                .currentStatusName(WarningStatusEnum.getDescriptionByCode(warning.getStatus()))
                .isFalseAlarm(warning.getIsFalseAlarm())
                .handlerName(warning.getHandlerName())
                .handleTime(warning.getHandleTime())
                .closeTime(warning.getCloseTime())
                .processLogs(logVOS)
                .build();
    }

    @Override
    public WarningDisposalReportVO generateDisposalReport(Long id) {
        WarningRecord warning = getWarningOrThrow(id);
        Field field = warning.getFarmlandId() == null ? null : fieldService.getById(warning.getFarmlandId());
        Device device = warning.getRelatedDeviceId() == null ? null : deviceService.getById(warning.getRelatedDeviceId());
        Crop crop = warning.getRelatedCropId() == null ? null : cropService.getById(warning.getRelatedCropId());

        List<WarningProcessLog> logs = warningProcessLogMapper.selectList(new LambdaQueryWrapper<WarningProcessLog>()
                .eq(WarningProcessLog::getWarningId, id)
                .orderByAsc(WarningProcessLog::getProcessTime)
                .orderByAsc(WarningProcessLog::getId));
        List<WarningProcessLogVO> processLogVOS = logs.stream()
                .map(this::toProcessLogVO)
                .toList();

        WarningReportEnvSnapshotVO envSnapshot = buildEnvironmentSnapshot(warning);
        String aiAnalysisSummary = resolveAiAnalysisSummary(warning);
        String aiSuggestionSummary = resolveAiSuggestionSummary(warning);
        String processRecordSummary = buildProcessSummary(logs);
        boolean closedLoop = Objects.equals(warning.getStatus(), WarningStatusEnum.CLOSED_LOOP.getCode());
        LocalDateTime reportTime = LocalDateTime.now();

        return WarningDisposalReportVO.builder()
                .warningId(warning.getId())
                .warningNo(warning.getWarningNo())
                .warningType(warning.getWarningType())
                .warningTypeName(WarningTypeEnum.getDescriptionByCode(warning.getWarningType()))
                .warningLevel(warning.getLevel())
                .warningLevelName(WarningLevelEnum.getDescriptionByCode(warning.getLevel()))
                .triggerSource(warning.getTriggerSource())
                .triggerSourceName(WarningTriggerSourceEnum.getDescriptionByCode(warning.getTriggerSource()))
                .farmlandId(warning.getFarmlandId())
                .farmlandName(field == null ? null : field.getFieldName())
                .relatedDeviceId(warning.getRelatedDeviceId())
                .relatedDeviceName(device == null ? null : device.getDeviceName())
                .relatedCropId(warning.getRelatedCropId())
                .relatedCropName(crop == null ? null : crop.getCropName())
                .triggerTime(warning.getTriggerTime() == null ? warning.getCreateTime() : warning.getTriggerTime())
                .environmentSnapshot(envSnapshot)
                .aiAnalysisSummary(aiAnalysisSummary)
                .aiSuggestionSummary(aiSuggestionSummary)
                .currentStatus(warning.getStatus())
                .currentStatusName(WarningStatusEnum.getDescriptionByCode(warning.getStatus()))
                .processRecordSummary(processRecordSummary)
                .processLogs(processLogVOS)
                .closedLoop(closedLoop)
                .isFalseAlarm(warning.getIsFalseAlarm())
                .closeTime(warning.getCloseTime())
                .reportSummary(buildReportSummary(warning, aiAnalysisSummary, aiSuggestionSummary, processRecordSummary))
                .reportGenerateTime(reportTime)
                .build();
    }

    @Override
    public void generateWarningsForEnvironmentData(EnvironmentData environmentData) {
        List<WarningTriggerCommand> commands = warningRuleEngineService.evaluateEnvironmentData(environmentData);
        commands.forEach(this::createWarningIfNeeded);
    }

    @Override
    public void generateDeviceOfflineWarning(Long deviceId) {
        Device device = deviceService.getById(deviceId);
        if (device == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "设备不存在");
        }
        List<WarningTriggerCommand> commands = warningRuleEngineService.evaluateDeviceStatus(device);
        commands.forEach(this::createWarningIfNeeded);
    }

    private void createWarningIfNeeded(WarningTriggerCommand command) {
        if (command == null || hasActiveDuplicateWarning(command)) {
            return;
        }

        WarningRecord warning = new WarningRecord();
        warning.setWarningNo(generateWarningNo());
        warning.setWarningType(command.getWarningType());
        warning.setContent(command.getContent());
        warning.setLevel(command.getWarningLevel());
        warning.setStatus(WarningStatusEnum.TO_CONFIRM.getCode());
        warning.setFarmlandId(command.getFarmlandId());
        warning.setRelatedDeviceId(command.getRelatedDeviceId());
        warning.setRelatedCropId(command.getRelatedCropId());
        warning.setTriggerSource(command.getTriggerSource());
        warning.setTriggerTime(command.getTriggerTime() == null ? LocalDateTime.now() : command.getTriggerTime());
        warning.setIsFalseAlarm(0);
        save(warning);

        saveProcessLog(warning.getId(), WarningProcessActionEnum.AUTO_GENERATE.getCode(),
                null, warning.getStatus(), "系统规则引擎", "系统自动识别异常并生成预警。", LocalDateTime.now());
    }

    private boolean hasActiveDuplicateWarning(WarningTriggerCommand command) {
        // 规则中的抑制时间窗口优先，其次保留“未处理状态去重”兜底，避免短时间重复刷屏。
        Integer deduplicateMinutes = command.getDeduplicateMinutes();
        if (deduplicateMinutes != null && deduplicateMinutes > 0) {
            LocalDateTime startTime = LocalDateTime.now().minusMinutes(deduplicateMinutes);
            LambdaQueryWrapper<WarningRecord> windowWrapper = buildDuplicateKeyWrapper(command)
                    .ge(WarningRecord::getTriggerTime, startTime);
            if (count(windowWrapper) > 0) {
                return true;
            }
        }

        LambdaQueryWrapper<WarningRecord> activeWrapper = buildDuplicateKeyWrapper(command)
                .in(WarningRecord::getStatus, WarningStatusEnum.TO_CONFIRM.getCode(), WarningStatusEnum.PROCESSING.getCode());
        return count(activeWrapper) > 0;
    }

    private LambdaQueryWrapper<WarningRecord> buildDuplicateKeyWrapper(WarningTriggerCommand command) {
        LambdaQueryWrapper<WarningRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WarningRecord::getWarningType, command.getWarningType())
                .eq(WarningRecord::getIsFalseAlarm, 0);

        if (command.getFarmlandId() == null) {
            queryWrapper.isNull(WarningRecord::getFarmlandId);
        } else {
            queryWrapper.eq(WarningRecord::getFarmlandId, command.getFarmlandId());
        }
        if (command.getRelatedDeviceId() == null) {
            queryWrapper.isNull(WarningRecord::getRelatedDeviceId);
        } else {
            queryWrapper.eq(WarningRecord::getRelatedDeviceId, command.getRelatedDeviceId());
        }
        if (command.getRelatedCropId() == null) {
            queryWrapper.isNull(WarningRecord::getRelatedCropId);
        } else {
            queryWrapper.eq(WarningRecord::getRelatedCropId, command.getRelatedCropId());
        }
        return queryWrapper;
    }

    private void markFalseAlarmInternal(WarningRecord warning, String operatorName, String reason) {
        if (Objects.equals(warning.getStatus(), WarningStatusEnum.CLOSED_LOOP.getCode())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "已闭环预警不允许再次标记误报");
        }
        if (!List.of(
                WarningStatusEnum.TO_CONFIRM.getCode(),
                WarningStatusEnum.PROCESSING.getCode(),
                WarningStatusEnum.HANDLED.getCode()).contains(warning.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "当前状态不允许标记误报");
        }

        Integer beforeStatus = warning.getStatus();
        LocalDateTime now = LocalDateTime.now();
        warning.setStatus(WarningStatusEnum.CLOSED_LOOP.getCode());
        warning.setIsFalseAlarm(1);
        warning.setHandlerName(operatorName);
        warning.setHandleRemark(reason);
        if (warning.getHandleTime() == null) {
            warning.setHandleTime(now);
        }
        warning.setCloseTime(now);
        updateById(warning);

        saveProcessLog(warning.getId(), WarningProcessActionEnum.MARK_FALSE_ALARM.getCode(),
                beforeStatus, warning.getStatus(), operatorName, reason, now);
    }

    private void applyStatusTransition(WarningRecord warning,
                                       Integer targetStatus,
                                       String actionType,
                                       String operatorName,
                                       String note,
                                       boolean skipValidation) {
        Integer beforeStatus = warning.getStatus();
        if (!skipValidation && !isAllowedTransition(beforeStatus, targetStatus)) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "状态流转不合法，当前状态为“" + WarningStatusEnum.getDescriptionByCode(beforeStatus)
                            + "”，目标状态为“" + WarningStatusEnum.getDescriptionByCode(targetStatus) + "”");
        }

        LocalDateTime now = LocalDateTime.now();
        warning.setStatus(targetStatus);
        warning.setHandlerName(operatorName);
        if (StringUtils.hasText(note)) {
            warning.setHandleRemark(note);
        }

        if (Objects.equals(targetStatus, WarningStatusEnum.HANDLED.getCode()) && warning.getHandleTime() == null) {
            warning.setHandleTime(now);
        }
        if (Objects.equals(targetStatus, WarningStatusEnum.CLOSED_LOOP.getCode())) {
            if (warning.getHandleTime() == null) {
                warning.setHandleTime(now);
            }
            warning.setCloseTime(now);
        }
        updateById(warning);

        saveProcessLog(warning.getId(), actionType, beforeStatus, targetStatus, operatorName, note, now);
    }

    private boolean isAllowedTransition(Integer fromStatus, Integer toStatus) {
        if (Objects.equals(fromStatus, toStatus)) {
            return true;
        }
        if (Objects.equals(fromStatus, WarningStatusEnum.TO_CONFIRM.getCode())
                && Objects.equals(toStatus, WarningStatusEnum.PROCESSING.getCode())) {
            return true;
        }
        if (Objects.equals(fromStatus, WarningStatusEnum.PROCESSING.getCode())
                && Objects.equals(toStatus, WarningStatusEnum.HANDLED.getCode())) {
            return true;
        }
        return Objects.equals(fromStatus, WarningStatusEnum.HANDLED.getCode())
                && Objects.equals(toStatus, WarningStatusEnum.CLOSED_LOOP.getCode());
    }

    private WarningRecord getWarningOrThrow(Long id) {
        WarningRecord warning = getById(id);
        if (warning == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "预警不存在");
        }
        return warning;
    }

    private void validateQueryParams(Integer level, Integer status, String triggerSource) {
        if (level != null && !WarningLevelEnum.contains(level)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "预警等级取值不合法");
        }
        if (status != null && !WarningStatusEnum.contains(status)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "预警状态取值不合法");
        }
        if (StringUtils.hasText(triggerSource) && !WarningTriggerSourceEnum.contains(triggerSource)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "触发来源取值不合法");
        }
    }

    private Map<Long, Field> loadFieldMap(List<WarningRecord> warningList) {
        if (warningList == null || warningList.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> fieldIds = warningList.stream()
                .map(WarningRecord::getFarmlandId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (fieldIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return fieldService.listByIds(fieldIds).stream()
                .collect(Collectors.toMap(Field::getId, Function.identity()));
    }

    private Map<Long, Device> loadDeviceMap(List<WarningRecord> warningList) {
        if (warningList == null || warningList.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> deviceIds = warningList.stream()
                .map(WarningRecord::getRelatedDeviceId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (deviceIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return deviceService.listByIds(deviceIds).stream()
                .collect(Collectors.toMap(Device::getId, Function.identity()));
    }

    private WarningListVO buildListVO(WarningRecord warning, Field field, Device device) {
        return WarningListVO.builder()
                .id(warning.getId())
                .warningNo(warning.getWarningNo())
                .warningType(warning.getWarningType())
                .warningTypeName(WarningTypeEnum.getDescriptionByCode(warning.getWarningType()))
                .content(warning.getContent())
                .level(warning.getLevel())
                .levelName(WarningLevelEnum.getDescriptionByCode(warning.getLevel()))
                .status(warning.getStatus())
                .statusName(WarningStatusEnum.getDescriptionByCode(warning.getStatus()))
                .triggerSource(warning.getTriggerSource())
                .triggerSourceName(WarningTriggerSourceEnum.getDescriptionByCode(warning.getTriggerSource()))
                .triggerTime(warning.getTriggerTime() == null ? warning.getCreateTime() : warning.getTriggerTime())
                .farmlandId(warning.getFarmlandId())
                .farmlandName(field == null ? null : field.getFieldName())
                .relatedDeviceId(warning.getRelatedDeviceId())
                .relatedDeviceName(device == null ? null : device.getDeviceName())
                .handlerName(warning.getHandlerName())
                .handleTime(warning.getHandleTime())
                .isFalseAlarm(warning.getIsFalseAlarm())
                .createTime(warning.getCreateTime())
                .build();
    }

    private WarningDetailVO buildDetailVO(WarningRecord warning, Field field, Device device) {
        return WarningDetailVO.builder()
                .id(warning.getId())
                .warningNo(warning.getWarningNo())
                .warningType(warning.getWarningType())
                .warningTypeName(WarningTypeEnum.getDescriptionByCode(warning.getWarningType()))
                .content(warning.getContent())
                .level(warning.getLevel())
                .levelName(WarningLevelEnum.getDescriptionByCode(warning.getLevel()))
                .status(warning.getStatus())
                .statusName(WarningStatusEnum.getDescriptionByCode(warning.getStatus()))
                .farmlandId(warning.getFarmlandId())
                .farmlandName(field == null ? null : field.getFieldName())
                .relatedDeviceId(warning.getRelatedDeviceId())
                .relatedDeviceName(device == null ? null : device.getDeviceName())
                .relatedCropId(warning.getRelatedCropId())
                .triggerSource(warning.getTriggerSource())
                .triggerSourceName(WarningTriggerSourceEnum.getDescriptionByCode(warning.getTriggerSource()))
                .triggerTime(warning.getTriggerTime() == null ? warning.getCreateTime() : warning.getTriggerTime())
                .aiAnalysisSummary(warning.getAiAnalysisSummary())
                .aiSuggestionSummary(warning.getAiSuggestionSummary())
                .handlerName(warning.getHandlerName())
                .handleTime(warning.getHandleTime())
                .handleRemark(warning.getHandleRemark())
                .closeTime(warning.getCloseTime())
                .isFalseAlarm(warning.getIsFalseAlarm())
                .createTime(warning.getCreateTime())
                .updateTime(warning.getUpdateTime())
                .build();
    }

    private WarningProcessLogVO toProcessLogVO(WarningProcessLog log) {
        return WarningProcessLogVO.builder()
                .id(log.getId())
                .actionType(log.getActionType())
                .actionTypeName(WarningProcessActionEnum.getDescriptionByCode(log.getActionType()))
                .statusFrom(log.getStatusFrom())
                .statusFromName(log.getStatusFrom() == null ? "-" : WarningStatusEnum.getDescriptionByCode(log.getStatusFrom()))
                .statusTo(log.getStatusTo())
                .statusToName(log.getStatusTo() == null ? "-" : WarningStatusEnum.getDescriptionByCode(log.getStatusTo()))
                .operatorName(log.getOperatorName())
                .processNote(log.getProcessNote())
                .processTime(log.getProcessTime())
                .build();
    }

    private WarningReportEnvSnapshotVO buildEnvironmentSnapshot(WarningRecord warning) {
        LambdaQueryWrapper<EnvironmentData> primaryWrapper = new LambdaQueryWrapper<>();
        primaryWrapper.eq(warning.getFarmlandId() != null, EnvironmentData::getFarmlandId, warning.getFarmlandId())
                .eq(warning.getRelatedDeviceId() != null, EnvironmentData::getDeviceId, warning.getRelatedDeviceId())
                .le(warning.getTriggerTime() != null, EnvironmentData::getCollectTime, warning.getTriggerTime())
                .orderByDesc(EnvironmentData::getCollectTime)
                .orderByDesc(EnvironmentData::getId)
                .last("limit 1");

        EnvironmentData snapshot = environmentDataMapper.selectOne(primaryWrapper);
        if (snapshot == null && warning.getFarmlandId() != null) {
            snapshot = environmentDataMapper.selectOne(new LambdaQueryWrapper<EnvironmentData>()
                    .eq(EnvironmentData::getFarmlandId, warning.getFarmlandId())
                    .orderByDesc(EnvironmentData::getCollectTime)
                    .orderByDesc(EnvironmentData::getId)
                    .last("limit 1"));
        }
        if (snapshot == null && warning.getRelatedDeviceId() != null) {
            snapshot = environmentDataMapper.selectOne(new LambdaQueryWrapper<EnvironmentData>()
                    .eq(EnvironmentData::getDeviceId, warning.getRelatedDeviceId())
                    .orderByDesc(EnvironmentData::getCollectTime)
                    .orderByDesc(EnvironmentData::getId)
                    .last("limit 1"));
        }
        if (snapshot == null) {
            return null;
        }
        return WarningReportEnvSnapshotVO.builder()
                .farmlandId(snapshot.getFarmlandId())
                .deviceId(snapshot.getDeviceId())
                .temperature(snapshot.getTemperature())
                .humidity(snapshot.getHumidity())
                .soilMoisture(snapshot.getSoilMoisture())
                .lightIntensity(snapshot.getLightIntensity())
                .phValue(snapshot.getPhValue())
                .collectTime(snapshot.getCollectTime())
                .build();
    }

    private String resolveAiAnalysisSummary(WarningRecord warning) {
        String fromRecord = trimToNull(warning.getAiAnalysisSummary());
        if (fromRecord != null) {
            return fromRecord;
        }
        List<WarningAiAnalysis> aiRecords = warningAiAnalysisMapper.selectList(new LambdaQueryWrapper<WarningAiAnalysis>()
                .eq(WarningAiAnalysis::getWarningId, warning.getId())
                .isNotNull(WarningAiAnalysis::getAnalysisSummary)
                .orderByDesc(WarningAiAnalysis::getCreateTime)
                .orderByDesc(WarningAiAnalysis::getId)
                .last("limit 1"));
        if (!aiRecords.isEmpty()) {
            String value = aiRecords.get(0).getAnalysisSummary();
            return StringUtils.hasText(value) ? value : "暂无 AI 原因分析";
        }
        return "暂无 AI 原因分析";
    }

    private String resolveAiSuggestionSummary(WarningRecord warning) {
        String fromRecord = trimToNull(warning.getAiSuggestionSummary());
        if (fromRecord != null) {
            return fromRecord;
        }
        List<WarningAiAnalysis> aiRecords = warningAiAnalysisMapper.selectList(new LambdaQueryWrapper<WarningAiAnalysis>()
                .eq(WarningAiAnalysis::getWarningId, warning.getId())
                .isNotNull(WarningAiAnalysis::getSuggestionSummary)
                .orderByDesc(WarningAiAnalysis::getCreateTime)
                .orderByDesc(WarningAiAnalysis::getId)
                .last("limit 1"));
        if (!aiRecords.isEmpty()) {
            String value = aiRecords.get(0).getSuggestionSummary();
            return StringUtils.hasText(value) ? value : "暂无 AI 处置建议";
        }
        return "暂无 AI 处置建议";
    }

    private String buildProcessSummary(List<WarningProcessLog> logs) {
        if (logs == null || logs.isEmpty()) {
            return "暂无处理记录，建议先确认预警并进入处置流程。";
        }
        WarningProcessLog latestLog = logs.get(logs.size() - 1);
        String latestAction = WarningProcessActionEnum.getDescriptionByCode(latestLog.getActionType());
        String latestTime = latestLog.getProcessTime() == null ? "--" : latestLog.getProcessTime().toString();
        return String.format(Locale.ROOT, "共 %d 条处理记录，最近动作为“%s”（%s）。", logs.size(), latestAction, latestTime);
    }

    private String buildReportSummary(WarningRecord warning,
                                      String aiAnalysisSummary,
                                      String aiSuggestionSummary,
                                      String processRecordSummary) {
        String warningLevelName = WarningLevelEnum.getDescriptionByCode(warning.getLevel());
        String statusName = WarningStatusEnum.getDescriptionByCode(warning.getStatus());
        return "【风险概述】"
                + warningLevelName + "风险，当前状态：" + statusName
                + "；【AI分析】" + abbreviateText(aiAnalysisSummary, 70)
                + "；【处置建议】" + abbreviateText(aiSuggestionSummary, 70)
                + "；【处理进度】" + abbreviateText(processRecordSummary, 60);
    }

    private String abbreviateText(String text, int maxLength) {
        if (!StringUtils.hasText(text)) {
            return "-";
        }
        String cleanText = text.trim();
        if (cleanText.length() <= maxLength) {
            return cleanText;
        }
        return cleanText.substring(0, maxLength) + "...";
    }

    private String trimToNull(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void saveProcessLog(Long warningId,
                                String actionType,
                                Integer statusFrom,
                                Integer statusTo,
                                String operatorName,
                                String processNote,
                                LocalDateTime processTime) {
        WarningProcessLog log = new WarningProcessLog();
        log.setWarningId(warningId);
        log.setActionType(actionType);
        log.setStatusFrom(statusFrom);
        log.setStatusTo(statusTo);
        log.setOperatorName(operatorName);
        log.setProcessNote(processNote);
        log.setProcessTime(processTime == null ? LocalDateTime.now() : processTime);
        warningProcessLogMapper.insert(log);
    }

    private String generateWarningNo() {
        return "WARN" + LocalDateTime.now().format(WARNING_NO_TIME_FORMAT)
                + ThreadLocalRandom.current().nextInt(1000, 10000);
    }

    private String resolveOperatorName(String operatorName) {
        return StringUtils.hasText(operatorName) ? operatorName : "系统管理员";
    }
}
