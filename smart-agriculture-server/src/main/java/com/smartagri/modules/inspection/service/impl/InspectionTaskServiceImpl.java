package com.smartagri.modules.inspection.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartagri.common.properties.InspectionTaskProperties;
import com.smartagri.modules.device.entity.Device;
import com.smartagri.modules.device.enums.DeviceOnlineStatusEnum;
import com.smartagri.modules.device.service.DeviceService;
import com.smartagri.modules.environment.entity.EnvironmentData;
import com.smartagri.modules.environment.mapper.EnvironmentDataMapper;
import com.smartagri.modules.inspection.entity.InspectionTaskLog;
import com.smartagri.modules.inspection.enums.InspectionTaskStatusEnum;
import com.smartagri.modules.inspection.mapper.InspectionTaskLogMapper;
import com.smartagri.modules.inspection.service.InspectionTaskService;
import com.smartagri.modules.inspection.vo.InspectionTaskLogVO;
import com.smartagri.modules.inspection.vo.InspectionTaskSummaryVO;
import com.smartagri.modules.warning.service.WarningService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class InspectionTaskServiceImpl implements InspectionTaskService {

    private static final int SUMMARY_MAX_LENGTH = 500;
    private static final int ERROR_MAX_LENGTH = 1000;

    private final InspectionTaskProperties inspectionTaskProperties;
    private final InspectionTaskLogMapper inspectionTaskLogMapper;
    private final EnvironmentDataMapper environmentDataMapper;
    private final DeviceService deviceService;
    private final WarningService warningService;
    private final AtomicBoolean runningLock = new AtomicBoolean(false);

    public InspectionTaskServiceImpl(InspectionTaskProperties inspectionTaskProperties,
                                     InspectionTaskLogMapper inspectionTaskLogMapper,
                                     EnvironmentDataMapper environmentDataMapper,
                                     DeviceService deviceService,
                                     WarningService warningService) {
        this.inspectionTaskProperties = inspectionTaskProperties;
        this.inspectionTaskLogMapper = inspectionTaskLogMapper;
        this.environmentDataMapper = environmentDataMapper;
        this.deviceService = deviceService;
        this.warningService = warningService;
    }

    @Override
    public void executeScheduledInspection() {
        if (!inspectionTaskProperties.isEnabled()) {
            return;
        }
        executeInspectionInternal("SCHEDULED", "系统定时任务");
    }

    @Override
    public void executeManualInspection(String operatorName) {
        executeInspectionInternal("MANUAL", operatorName == null || operatorName.isBlank() ? "系统管理员" : operatorName);
    }

    @Override
    public InspectionTaskSummaryVO getInspectionSummary(int recentLimit) {
        int safeLimit = Math.max(1, Math.min(recentLimit, 20));
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime now = LocalDateTime.now();

        Long todayInspectionCount = inspectionTaskLogMapper.selectCount(new LambdaQueryWrapper<InspectionTaskLog>()
                .ge(InspectionTaskLog::getInspectionTime, todayStart)
                .le(InspectionTaskLog::getInspectionTime, now));

        List<InspectionTaskLog> todayLogs = inspectionTaskLogMapper.selectList(new LambdaQueryWrapper<InspectionTaskLog>()
                .ge(InspectionTaskLog::getInspectionTime, todayStart)
                .le(InspectionTaskLog::getInspectionTime, now));
        long todayTriggeredWarnings = todayLogs.stream()
                .map(InspectionTaskLog::getTriggeredWarningCount)
                .filter(item -> item != null && item > 0)
                .mapToLong(Integer::longValue)
                .sum();

        List<InspectionTaskLog> recentLogs = inspectionTaskLogMapper.selectList(new LambdaQueryWrapper<InspectionTaskLog>()
                .orderByDesc(InspectionTaskLog::getInspectionTime)
                .last("LIMIT " + safeLimit));
        List<InspectionTaskLogVO> recentLogVOS = recentLogs.stream().map(this::toLogVO).toList();
        LocalDateTime latestInspectionTime = recentLogs.stream()
                .findFirst()
                .map(InspectionTaskLog::getInspectionTime)
                .orElse(null);

        return InspectionTaskSummaryVO.builder()
                .latestInspectionTime(latestInspectionTime)
                .todayInspectionCount(todayInspectionCount == null ? 0 : todayInspectionCount)
                .todayTriggeredWarningCount(todayTriggeredWarnings)
                .taskStatus(inspectionTaskProperties.isEnabled() ? "RUNNING" : "STOPPED")
                .executing(runningLock.get())
                .enabled(inspectionTaskProperties.isEnabled())
                .fixedDelayMs(inspectionTaskProperties.getFixedDelayMs())
                .recentLogs(recentLogVOS)
                .build();
    }

    private void executeInspectionInternal(String triggerMode, String operatorName) {
        if (!runningLock.compareAndSet(false, true)) {
            return;
        }

        LocalDateTime inspectionTime = LocalDateTime.now();
        int envCount = 0;
        int deviceCount = 0;
        int detectCount = 0;
        int triggeredWarningCount = 0;
        String summary;
        String errorMessage = null;
        String status = InspectionTaskStatusEnum.SUCCESS.getCode();

        try {
            long warningBefore = warningService.count();

            List<EnvironmentData> envDataList = loadInspectionEnvironmentData();
            envCount = envDataList.size();
            for (EnvironmentData data : envDataList) {
                warningService.generateWarningsForEnvironmentData(data);
            }

            List<Device> offlineDevices = loadOfflineDevices();
            deviceCount = offlineDevices.size();
            for (Device device : offlineDevices) {
                warningService.generateDeviceOfflineWarning(device.getId());
            }

            detectCount = envCount + deviceCount;
            long warningAfter = warningService.count();
            triggeredWarningCount = (int) Math.max(0, warningAfter - warningBefore);

            summary = String.format("巡检完成：环境数据%d条，离线设备%d台，新增预警%d条（触发方式：%s）。",
                    envCount, deviceCount, triggeredWarningCount, triggerMode);
            log.info("巡检任务执行成功，operator={}, {}", operatorName, summary);
        } catch (Exception ex) {
            status = InspectionTaskStatusEnum.FAILED.getCode();
            summary = "巡检执行失败，请检查日志定位原因。";
            errorMessage = truncate(ex.getMessage(), ERROR_MAX_LENGTH);
            log.error("巡检任务执行失败，operator={}", operatorName, ex);
        } finally {
            runningLock.set(false);
        }

        saveInspectionLog(inspectionTime, envCount, deviceCount, detectCount,
                triggeredWarningCount, status, summary, errorMessage, triggerMode);
    }

    private List<EnvironmentData> loadInspectionEnvironmentData() {
        LocalDateTime startTime = LocalDateTime.now().minusMinutes(Math.max(1, inspectionTaskProperties.getEnvLookbackMinutes()));
        int limit = Math.max(1, inspectionTaskProperties.getEnvBatchSize());
        List<EnvironmentData> dataList = environmentDataMapper.selectList(new LambdaQueryWrapper<EnvironmentData>()
                .ge(EnvironmentData::getCollectTime, startTime)
                .orderByDesc(EnvironmentData::getCollectTime)
                .last("LIMIT " + limit));
        return dataList == null ? Collections.emptyList() : dataList;
    }

    private List<Device> loadOfflineDevices() {
        int limit = Math.max(1, inspectionTaskProperties.getDeviceBatchSize());
        List<Device> devices = deviceService.list(new LambdaQueryWrapper<Device>()
                .eq(Device::getOnlineStatus, DeviceOnlineStatusEnum.OFFLINE.getCode())
                .orderByDesc(Device::getUpdateTime)
                .orderByDesc(Device::getId)
                .last("LIMIT " + limit));
        return devices == null ? Collections.emptyList() : devices;
    }

    private void saveInspectionLog(LocalDateTime inspectionTime,
                                   int envCount,
                                   int deviceCount,
                                   int detectCount,
                                   int triggeredWarningCount,
                                   String status,
                                   String summary,
                                   String errorMessage,
                                   String triggerMode) {
        InspectionTaskLog logEntity = new InspectionTaskLog();
        logEntity.setInspectionTime(inspectionTime);
        logEntity.setInspectEnvCount(envCount);
        logEntity.setInspectDeviceCount(deviceCount);
        logEntity.setDetectDataCount(detectCount);
        logEntity.setTriggeredWarningCount(triggeredWarningCount);
        logEntity.setStatus(status);
        logEntity.setSummary(truncate(summary, SUMMARY_MAX_LENGTH));
        logEntity.setErrorMessage(truncate(errorMessage, ERROR_MAX_LENGTH));
        logEntity.setTriggerMode(triggerMode);
        inspectionTaskLogMapper.insert(logEntity);
    }

    private InspectionTaskLogVO toLogVO(InspectionTaskLog log) {
        return InspectionTaskLogVO.builder()
                .id(log.getId())
                .inspectionTime(log.getInspectionTime())
                .inspectEnvCount(log.getInspectEnvCount())
                .inspectDeviceCount(log.getInspectDeviceCount())
                .detectDataCount(log.getDetectDataCount())
                .triggeredWarningCount(log.getTriggeredWarningCount())
                .status(log.getStatus())
                .statusName(InspectionTaskStatusEnum.getDescriptionByCode(log.getStatus()))
                .summary(log.getSummary())
                .triggerMode(log.getTriggerMode())
                .build();
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}

