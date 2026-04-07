package com.smartagri.modules.dashboard.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartagri.modules.crop.entity.Crop;
import com.smartagri.modules.crop.enums.CropGrowthStageEnum;
import com.smartagri.modules.crop.service.CropService;
import com.smartagri.modules.dashboard.service.DashboardService;
import com.smartagri.modules.dashboard.vo.DashboardChartSeriesVO;
import com.smartagri.modules.dashboard.vo.DashboardChartVO;
import com.smartagri.modules.dashboard.vo.DashboardHomeVO;
import com.smartagri.modules.dashboard.vo.DashboardNameValueVO;
import com.smartagri.modules.dashboard.vo.DashboardSummaryVO;
import com.smartagri.modules.dashboard.vo.DashboardWarningQueueVO;
import com.smartagri.modules.device.entity.Device;
import com.smartagri.modules.device.service.DeviceService;
import com.smartagri.modules.environment.entity.EnvironmentData;
import com.smartagri.modules.environment.service.EnvironmentDataService;
import com.smartagri.modules.field.entity.Field;
import com.smartagri.modules.field.service.FieldService;
import com.smartagri.modules.inspection.service.InspectionTaskService;
import com.smartagri.modules.inspection.vo.InspectionTaskSummaryVO;
import com.smartagri.modules.warning.entity.WarningRecord;
import com.smartagri.modules.warning.enums.WarningLevelEnum;
import com.smartagri.modules.warning.enums.WarningStatusEnum;
import com.smartagri.modules.warning.service.WarningService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DashboardServiceImpl implements DashboardService {

    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("MM-dd");

    private final FieldService fieldService;
    private final CropService cropService;
    private final DeviceService deviceService;
    private final EnvironmentDataService environmentDataService;
    private final WarningService warningService;
    private final InspectionTaskService inspectionTaskService;

    public DashboardServiceImpl(FieldService fieldService,
                                CropService cropService,
                                DeviceService deviceService,
                                EnvironmentDataService environmentDataService,
                                WarningService warningService,
                                InspectionTaskService inspectionTaskService) {
        this.fieldService = fieldService;
        this.cropService = cropService;
        this.deviceService = deviceService;
        this.environmentDataService = environmentDataService;
        this.warningService = warningService;
        this.inspectionTaskService = inspectionTaskService;
    }

    @Override
    public DashboardHomeVO getHomeDashboard() {
        return DashboardHomeVO.builder()
                .summary(getSummary())
                .farmlandStatusList(getFarmlandStatusStats())
                .cropGrowthStageList(getCropGrowthStageStats())
                .environmentTrend(getEnvironmentTrend())
                .warningLevelList(getWarningLevelStats())
                .warningStatusList(getWarningStatusStats())
                .highRiskWarningQueue(getHighRiskWarningQueue())
                .aiSuggestionList(getTodayAiSuggestionList())
                .inspectionSummary(getInspectionSummary())
                .closureTrend(getClosureTrend())
                .build();
    }

    @Override
    public DashboardSummaryVO getSummary() {
        List<WarningRecord> warningList = listWarningsByLatestOrder();
        LocalDate latestBizDate = warningList.isEmpty() ? LocalDate.now() : resolveWarningDate(warningList.get(0));
        List<WarningRecord> todayWarnings = warningList.stream()
                .filter(item -> latestBizDate.equals(resolveWarningDate(item)))
                .toList();

        long todayWarningCount = todayWarnings.size();
        long highRiskWarningCount = todayWarnings.stream()
                .filter(item -> Objects.equals(item.getLevel(), WarningLevelEnum.HIGH.getCode()))
                .count();
        long pendingWarningCount = todayWarnings.stream()
                .filter(item -> Objects.equals(item.getStatus(), WarningStatusEnum.TO_CONFIRM.getCode())
                        || Objects.equals(item.getStatus(), WarningStatusEnum.PROCESSING.getCode()))
                .count();
        long closedWarningCount = todayWarnings.stream()
                .filter(item -> Objects.equals(item.getStatus(), WarningStatusEnum.CLOSED_LOOP.getCode()))
                .count();

        BigDecimal closureRate = BigDecimal.ZERO;
        if (todayWarningCount > 0) {
            closureRate = BigDecimal.valueOf(closedWarningCount * 100.0 / todayWarningCount)
                    .setScale(1, RoundingMode.HALF_UP);
        }

        long avgClosureMinutes = calculateAvgClosureMinutes(warningList);

        return DashboardSummaryVO.builder()
                .farmlandCount(fieldService.count())
                .cropCount(cropService.count())
                .deviceCount(deviceService.count())
                .todayWarningCount(todayWarningCount)
                .highRiskWarningCount(highRiskWarningCount)
                .pendingWarningCount(pendingWarningCount)
                .closedWarningCount(closedWarningCount)
                .closureRate(closureRate)
                .avgClosureMinutes(avgClosureMinutes)
                .build();
    }

    @Override
    public List<DashboardNameValueVO> getFarmlandStatusStats() {
        Map<Integer, Long> countMap = fieldService.list().stream()
                .collect(Collectors.groupingBy(Field::getStatus, Collectors.counting()));

        return List.of(
                new DashboardNameValueVO("闲置", countMap.getOrDefault(0, 0L)),
                new DashboardNameValueVO("种植中", countMap.getOrDefault(1, 0L)),
                new DashboardNameValueVO("维护中", countMap.getOrDefault(2, 0L))
        );
    }

    @Override
    public List<DashboardNameValueVO> getCropGrowthStageStats() {
        Map<String, Long> countMap = cropService.list().stream()
                .collect(Collectors.groupingBy(Crop::getGrowthStage, Collectors.counting()));

        return countMap.entrySet().stream()
                .map(entry -> new DashboardNameValueVO(
                        CropGrowthStageEnum.getDescriptionByCode(entry.getKey()),
                        entry.getValue()))
                .sorted((left, right) -> left.getName().compareTo(right.getName()))
                .toList();
    }

    @Override
    public DashboardChartVO getEnvironmentTrend() {
        LocalDate latestDate = getLatestEnvironmentDate();
        List<LocalDate> dateRange = buildRecentDays(latestDate, 7);

        List<EnvironmentData> dataList = environmentDataService.list(new LambdaQueryWrapper<EnvironmentData>()
                .ge(EnvironmentData::getCollectTime, dateRange.get(0).atStartOfDay())
                .lt(EnvironmentData::getCollectTime, latestDate.plusDays(1).atStartOfDay())
                .orderByAsc(EnvironmentData::getCollectTime));

        Map<LocalDate, List<EnvironmentData>> groupMap = dataList.stream()
                .collect(Collectors.groupingBy(item -> item.getCollectTime().toLocalDate()));

        List<String> xAxis = new ArrayList<>();
        List<BigDecimal> temperatureData = new ArrayList<>();
        List<BigDecimal> humidityData = new ArrayList<>();
        List<BigDecimal> soilMoistureData = new ArrayList<>();

        for (LocalDate date : dateRange) {
            xAxis.add(date.format(DAY_FORMATTER));
            List<EnvironmentData> dailyList = groupMap.getOrDefault(date, List.of());
            temperatureData.add(avg(dailyList.stream().map(EnvironmentData::getTemperature).filter(Objects::nonNull).toList()));
            humidityData.add(avg(dailyList.stream().map(EnvironmentData::getHumidity).filter(Objects::nonNull).toList()));
            soilMoistureData.add(avg(dailyList.stream().map(EnvironmentData::getSoilMoisture).filter(Objects::nonNull).toList()));
        }

        return DashboardChartVO.builder()
                .xAxis(xAxis)
                .series(List.of(
                        DashboardChartSeriesVO.builder().name("平均温度").type("line").data(temperatureData).build(),
                        DashboardChartSeriesVO.builder().name("平均湿度").type("line").data(humidityData).build(),
                        DashboardChartSeriesVO.builder().name("平均土壤湿度").type("line").data(soilMoistureData).build()
                ))
                .build();
    }

    @Override
    public List<DashboardNameValueVO> getWarningLevelStats() {
        Map<Integer, Long> countMap = warningService.list().stream()
                .collect(Collectors.groupingBy(WarningRecord::getLevel, Collectors.counting()));

        return List.of(
                new DashboardNameValueVO("低", countMap.getOrDefault(WarningLevelEnum.LOW.getCode(), 0L)),
                new DashboardNameValueVO("中", countMap.getOrDefault(WarningLevelEnum.MEDIUM.getCode(), 0L)),
                new DashboardNameValueVO("高", countMap.getOrDefault(WarningLevelEnum.HIGH.getCode(), 0L))
        );
    }

    @Override
    public List<DashboardNameValueVO> getWarningStatusStats() {
        Map<Integer, Long> countMap = warningService.list().stream()
                .collect(Collectors.groupingBy(WarningRecord::getStatus, Collectors.counting()));
        return List.of(
                new DashboardNameValueVO("待确认", countMap.getOrDefault(WarningStatusEnum.TO_CONFIRM.getCode(), 0L)),
                new DashboardNameValueVO("处理中", countMap.getOrDefault(WarningStatusEnum.PROCESSING.getCode(), 0L)),
                new DashboardNameValueVO("已处理", countMap.getOrDefault(WarningStatusEnum.HANDLED.getCode(), 0L)),
                new DashboardNameValueVO("已闭环", countMap.getOrDefault(WarningStatusEnum.CLOSED_LOOP.getCode(), 0L))
        );
    }

    @Override
    public List<DashboardWarningQueueVO> getHighRiskWarningQueue() {
        List<WarningRecord> highRiskList = warningService.list(new LambdaQueryWrapper<WarningRecord>()
                .eq(WarningRecord::getLevel, WarningLevelEnum.HIGH.getCode())
                .ne(WarningRecord::getStatus, WarningStatusEnum.CLOSED_LOOP.getCode())
                .orderByDesc(WarningRecord::getTriggerTime)
                .orderByDesc(WarningRecord::getCreateTime)
                .last("LIMIT 3"));

        if (highRiskList.isEmpty()) {
            highRiskList = warningService.list(new LambdaQueryWrapper<WarningRecord>()
                    .eq(WarningRecord::getLevel, WarningLevelEnum.HIGH.getCode())
                    .orderByDesc(WarningRecord::getTriggerTime)
                    .orderByDesc(WarningRecord::getCreateTime)
                    .last("LIMIT 3"));
        }

        Map<Long, Field> fieldMap = loadFieldMap(highRiskList);
        Map<Long, Device> deviceMap = loadDeviceMap(highRiskList);

        return highRiskList.stream().map(item -> DashboardWarningQueueVO.builder()
                        .id(item.getId())
                        .warningNo(item.getWarningNo())
                        .warningTypeName(resolveWarningTypeName(item.getWarningType()))
                        .levelName(resolveWarningLevelName(item.getLevel()))
                        .statusName(resolveWarningStatusName(item.getStatus()))
                        .farmlandName(fieldMap.get(item.getFarmlandId()) == null ? "-" : fieldMap.get(item.getFarmlandId()).getFieldName())
                        .relatedDeviceName(deviceMap.get(item.getRelatedDeviceId()) == null ? "-" : deviceMap.get(item.getRelatedDeviceId()).getDeviceName())
                        .triggerTime(item.getTriggerTime() == null ? item.getCreateTime() : item.getTriggerTime())
                        .build())
                .toList();
    }

    @Override
    public List<String> getTodayAiSuggestionList() {
        DashboardSummaryVO summary = getSummary();
        List<String> suggestions = new ArrayList<>();
        suggestions.add("优先处置高风险预警（当前 " + summary.getHighRiskWarningCount() + " 条），并记录处理动作。");
        suggestions.add("待处理预警 " + summary.getPendingWarningCount() + " 条，建议按“确认 -> 处理中 -> 已处理 -> 已闭环”推进。");
        if (summary.getAvgClosureMinutes() != null && summary.getAvgClosureMinutes() > 0) {
            suggestions.add("当前平均闭环时长约 " + summary.getAvgClosureMinutes() + " 分钟，可作为今日处置效率基线。");
        } else {
            suggestions.add("建议在处理后及时提交结果与闭环时间，提升闭环分析准确性。");
        }
        return suggestions;
    }

    @Override
    public InspectionTaskSummaryVO getInspectionSummary() {
        try {
            return inspectionTaskService.getInspectionSummary(5);
        } catch (Exception ex) {
            return InspectionTaskSummaryVO.builder()
                    .latestInspectionTime(null)
                    .todayInspectionCount(0L)
                    .todayTriggeredWarningCount(0L)
                    .taskStatus("STOPPED")
                    .executing(false)
                    .enabled(false)
                    .fixedDelayMs(0L)
                    .recentLogs(Collections.emptyList())
                    .build();
        }
    }

    @Override
    public DashboardChartVO getClosureTrend() {
        List<WarningRecord> warningList = listWarningsByLatestOrder();
        LocalDate endDate = warningList.isEmpty() ? LocalDate.now() : resolveWarningDate(warningList.get(0));
        List<LocalDate> dateRange = buildRecentDays(endDate, 7);

        Map<LocalDate, List<WarningRecord>> triggerGroup = warningList.stream()
                .collect(Collectors.groupingBy(this::resolveWarningDate));
        Map<LocalDate, Long> closedGroup = warningList.stream()
                .filter(item -> Objects.equals(item.getStatus(), WarningStatusEnum.CLOSED_LOOP.getCode()) && item.getCloseTime() != null)
                .collect(Collectors.groupingBy(item -> item.getCloseTime().toLocalDate(), Collectors.counting()));

        List<String> xAxis = new ArrayList<>();
        List<BigDecimal> triggerCount = new ArrayList<>();
        List<BigDecimal> closedCount = new ArrayList<>();
        List<BigDecimal> closureRate = new ArrayList<>();

        for (LocalDate day : dateRange) {
            xAxis.add(day.format(DAY_FORMATTER));
            long trigger = triggerGroup.getOrDefault(day, List.of()).size();
            long closed = closedGroup.getOrDefault(day, 0L);
            BigDecimal rate = trigger == 0
                    ? BigDecimal.ZERO
                    : BigDecimal.valueOf(closed * 100.0 / trigger).setScale(1, RoundingMode.HALF_UP);
            triggerCount.add(BigDecimal.valueOf(trigger));
            closedCount.add(BigDecimal.valueOf(closed));
            closureRate.add(rate);
        }

        return DashboardChartVO.builder()
                .xAxis(xAxis)
                .series(List.of(
                        DashboardChartSeriesVO.builder().name("新增预警").type("line").data(triggerCount).build(),
                        DashboardChartSeriesVO.builder().name("闭环数量").type("line").data(closedCount).build(),
                        DashboardChartSeriesVO.builder().name("闭环率").type("line").data(closureRate).build()
                ))
                .build();
    }

    private List<WarningRecord> listWarningsByLatestOrder() {
        return warningService.list(new LambdaQueryWrapper<WarningRecord>()
                .orderByDesc(WarningRecord::getTriggerTime)
                .orderByDesc(WarningRecord::getCreateTime)
                .orderByDesc(WarningRecord::getId));
    }

    private long calculateAvgClosureMinutes(List<WarningRecord> warningList) {
        List<Long> durations = warningList.stream()
                .filter(item -> Objects.equals(item.getStatus(), WarningStatusEnum.CLOSED_LOOP.getCode()))
                .filter(item -> item.getTriggerTime() != null && item.getCloseTime() != null)
                .map(item -> Math.max(0, Duration.between(item.getTriggerTime(), item.getCloseTime()).toMinutes()))
                .toList();
        if (durations.isEmpty()) {
            return 0L;
        }
        long total = durations.stream().mapToLong(Long::longValue).sum();
        return Math.round(total * 1.0 / durations.size());
    }

    private LocalDate resolveWarningDate(WarningRecord warningRecord) {
        if (warningRecord.getTriggerTime() != null) {
            return warningRecord.getTriggerTime().toLocalDate();
        }
        if (warningRecord.getCreateTime() != null) {
            return warningRecord.getCreateTime().toLocalDate();
        }
        return LocalDate.now();
    }

    private LocalDate getLatestEnvironmentDate() {
        return environmentDataService.list(new LambdaQueryWrapper<EnvironmentData>()
                        .orderByDesc(EnvironmentData::getCollectTime)
                        .last("LIMIT 1"))
                .stream()
                .findFirst()
                .map(item -> item.getCollectTime().toLocalDate())
                .orElse(LocalDate.now());
    }

    private List<LocalDate> buildRecentDays(LocalDate latestDate, int days) {
        List<LocalDate> dates = new ArrayList<>();
        int safeDays = Math.max(1, days);
        for (int i = safeDays - 1; i >= 0; i--) {
            dates.add(latestDate.minusDays(i));
        }
        return dates;
    }

    private BigDecimal avg(List<BigDecimal> values) {
        if (values == null || values.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
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
                .collect(Collectors.toMap(Field::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
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
                .collect(Collectors.toMap(Device::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
    }

    private String resolveWarningLevelName(Integer level) {
        if (Objects.equals(level, WarningLevelEnum.HIGH.getCode())) {
            return "高";
        }
        if (Objects.equals(level, WarningLevelEnum.MEDIUM.getCode())) {
            return "中";
        }
        if (Objects.equals(level, WarningLevelEnum.LOW.getCode())) {
            return "低";
        }
        return "-";
    }

    private String resolveWarningStatusName(Integer status) {
        if (Objects.equals(status, WarningStatusEnum.TO_CONFIRM.getCode())) {
            return "待确认";
        }
        if (Objects.equals(status, WarningStatusEnum.PROCESSING.getCode())) {
            return "处理中";
        }
        if (Objects.equals(status, WarningStatusEnum.HANDLED.getCode())) {
            return "已处理";
        }
        if (Objects.equals(status, WarningStatusEnum.CLOSED_LOOP.getCode())) {
            return "已闭环";
        }
        return "-";
    }

    private String resolveWarningTypeName(String warningType) {
        if (warningType == null) {
            return "-";
        }
        return switch (warningType) {
            case "SOIL_MOISTURE_LOW" -> "土壤湿度过低";
            case "TEMPERATURE_HIGH" -> "温度过高";
            case "TEMPERATURE_LOW" -> "温度过低";
            case "LIGHT_ABNORMAL" -> "光照异常";
            case "LIGHT_INTENSITY_HIGH" -> "光照强度过高";
            case "DEVICE_OFFLINE" -> "设备离线";
            case "DEVICE_MAINTENANCE" -> "设备维护";
            case "HUMIDITY_HIGH" -> "湿度过高";
            default -> warningType;
        };
    }
}
