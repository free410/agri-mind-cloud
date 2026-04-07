package com.smartagri.modules.environment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartagri.common.exception.BusinessException;
import com.smartagri.common.result.ResultCode;
import com.smartagri.modules.device.entity.Device;
import com.smartagri.modules.device.service.DeviceService;
import com.smartagri.modules.environment.dto.EnvironmentDataCreateRequest;
import com.smartagri.modules.environment.dto.EnvironmentDataPageQueryRequest;
import com.smartagri.modules.environment.dto.EnvironmentTrendQueryRequest;
import com.smartagri.modules.environment.entity.EnvironmentData;
import com.smartagri.modules.environment.mapper.EnvironmentDataMapper;
import com.smartagri.modules.environment.service.EnvironmentDataService;
import com.smartagri.modules.environment.vo.EnvironmentChartSeriesVO;
import com.smartagri.modules.environment.vo.EnvironmentDataListVO;
import com.smartagri.modules.environment.vo.EnvironmentTrendVO;
import com.smartagri.modules.field.entity.Field;
import com.smartagri.modules.field.service.FieldService;
import com.smartagri.modules.warning.service.WarningService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class EnvironmentDataServiceImpl extends ServiceImpl<EnvironmentDataMapper, EnvironmentData> implements EnvironmentDataService {

    private static final DateTimeFormatter TREND_TIME_FORMATTER = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    private final FieldService fieldService;
    private final DeviceService deviceService;
    private final WarningService warningService;

    public EnvironmentDataServiceImpl(FieldService fieldService,
                                      DeviceService deviceService,
                                      WarningService warningService) {
        this.fieldService = fieldService;
        this.deviceService = deviceService;
        this.warningService = warningService;
    }

    @Override
    public Long createEnvironmentData(EnvironmentDataCreateRequest request) {
        validateFarmlandAndDevice(request.getFarmlandId(), request.getDeviceId());
        validateCollectTime(request.getCollectTime());

        EnvironmentData environmentData = new EnvironmentData();
        environmentData.setFarmlandId(request.getFarmlandId());
        environmentData.setDeviceId(request.getDeviceId());
        environmentData.setTemperature(request.getTemperature());
        environmentData.setHumidity(request.getHumidity());
        environmentData.setSoilMoisture(request.getSoilMoisture());
        environmentData.setLightIntensity(request.getLightIntensity());
        environmentData.setPhValue(request.getPhValue());
        environmentData.setCollectTime(request.getCollectTime());
        save(environmentData);

        try {
            // 环境数据入库后触发自动预警规则
            warningService.generateWarningsForEnvironmentData(environmentData);
        } catch (Exception ex) {
            // 不阻塞主流程，避免因为规则异常导致采集数据无法入库
            log.error("环境数据触发自动预警失败，envDataId={}", environmentData.getId(), ex);
        }
        return environmentData.getId();
    }

    @Override
    public IPage<EnvironmentDataListVO> pageEnvironmentData(EnvironmentDataPageQueryRequest request) {
        validateTimeRange(request.getStartTime(), request.getEndTime());

        Page<EnvironmentData> page = new Page<>(request.getCurrent(), request.getSize());
        LambdaQueryWrapper<EnvironmentData> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(request.getFarmlandId() != null, EnvironmentData::getFarmlandId, request.getFarmlandId())
                .eq(request.getDeviceId() != null, EnvironmentData::getDeviceId, request.getDeviceId())
                .ge(request.getStartTime() != null, EnvironmentData::getCollectTime, request.getStartTime())
                .le(request.getEndTime() != null, EnvironmentData::getCollectTime, request.getEndTime())
                .orderByDesc(EnvironmentData::getCollectTime);

        Page<EnvironmentData> dataPage = page(page, queryWrapper);
        Map<Long, Field> fieldMap = loadFieldMap(dataPage.getRecords());
        Map<Long, Device> deviceMap = loadDeviceMap(dataPage.getRecords());

        Page<EnvironmentDataListVO> resultPage = new Page<>(dataPage.getCurrent(), dataPage.getSize(), dataPage.getTotal());
        resultPage.setRecords(dataPage.getRecords().stream()
                .map(item -> buildListVO(item, fieldMap.get(item.getFarmlandId()), deviceMap.get(item.getDeviceId())))
                .toList());
        return resultPage;
    }

    @Override
    public EnvironmentTrendVO getTrendData(EnvironmentTrendQueryRequest request) {
        validateTimeRange(request.getStartTime(), request.getEndTime());
        validateFarmlandExists(request.getFarmlandId());
        if (request.getDeviceId() != null) {
            validateFarmlandAndDevice(request.getFarmlandId(), request.getDeviceId());
        }

        LambdaQueryWrapper<EnvironmentData> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(EnvironmentData::getFarmlandId, request.getFarmlandId())
                .eq(request.getDeviceId() != null, EnvironmentData::getDeviceId, request.getDeviceId())
                .ge(request.getStartTime() != null, EnvironmentData::getCollectTime, request.getStartTime())
                .le(request.getEndTime() != null, EnvironmentData::getCollectTime, request.getEndTime())
                .orderByDesc(EnvironmentData::getCollectTime)
                .last("LIMIT " + request.getLimit());

        List<EnvironmentData> reverseList = list(queryWrapper);
        Collections.reverse(reverseList);

        Field field = fieldService.getById(request.getFarmlandId());
        Device device = request.getDeviceId() == null ? null : deviceService.getById(request.getDeviceId());

        return EnvironmentTrendVO.builder()
                .farmlandId(request.getFarmlandId())
                .farmlandName(field == null ? null : field.getFieldName())
                .deviceId(request.getDeviceId())
                .deviceName(device == null ? null : device.getDeviceName())
                .xAxis(reverseList.stream()
                        .map(item -> item.getCollectTime().format(TREND_TIME_FORMATTER))
                        .toList())
                .series(List.of(
                        buildSeries("温度", reverseList.stream().map(EnvironmentData::getTemperature).toList()),
                        buildSeries("湿度", reverseList.stream().map(EnvironmentData::getHumidity).toList()),
                        buildSeries("土壤湿度", reverseList.stream().map(EnvironmentData::getSoilMoisture).toList()),
                        buildSeries("光照强度", reverseList.stream().map(EnvironmentData::getLightIntensity).toList()),
                        buildSeries("PH值", reverseList.stream().map(EnvironmentData::getPhValue).toList())
                ))
                .build();
    }

    private void validateFarmlandAndDevice(Long farmlandId, Long deviceId) {
        Field field = fieldService.getById(farmlandId);
        if (field == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "所属农田不存在");
        }

        Device device = deviceService.getById(deviceId);
        if (device == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "所属设备不存在");
        }

        if (!Objects.equals(device.getFarmlandId(), farmlandId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "设备与农田归属不匹配");
        }
    }

    private void validateFarmlandExists(Long farmlandId) {
        if (fieldService.getById(farmlandId) == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "所属农田不存在");
        }
    }

    private void validateCollectTime(LocalDateTime collectTime) {
        if (collectTime.isAfter(LocalDateTime.now().plusDays(1))) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "采集时间不能晚于当前时间过多");
        }
    }

    private void validateTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime != null && endTime != null && endTime.isBefore(startTime)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "结束时间不能早于开始时间");
        }
    }

    private Map<Long, Field> loadFieldMap(List<EnvironmentData> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            return Collections.emptyMap();
        }

        Set<Long> fieldIds = dataList.stream()
                .map(EnvironmentData::getFarmlandId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return fieldService.listByIds(fieldIds).stream()
                .collect(Collectors.toMap(Field::getId, Function.identity()));
    }

    private Map<Long, Device> loadDeviceMap(List<EnvironmentData> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            return Collections.emptyMap();
        }

        Set<Long> deviceIds = dataList.stream()
                .map(EnvironmentData::getDeviceId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return deviceService.listByIds(deviceIds).stream()
                .collect(Collectors.toMap(Device::getId, Function.identity()));
    }

    private EnvironmentDataListVO buildListVO(EnvironmentData data, Field field, Device device) {
        return EnvironmentDataListVO.builder()
                .id(data.getId())
                .farmlandId(data.getFarmlandId())
                .farmlandName(field == null ? null : field.getFieldName())
                .deviceId(data.getDeviceId())
                .deviceName(device == null ? null : device.getDeviceName())
                .deviceType(device == null ? null : device.getDeviceType())
                .temperature(data.getTemperature())
                .humidity(data.getHumidity())
                .soilMoisture(data.getSoilMoisture())
                .lightIntensity(data.getLightIntensity())
                .phValue(data.getPhValue())
                .collectTime(data.getCollectTime())
                .createTime(data.getCreateTime())
                .build();
    }

    private EnvironmentChartSeriesVO buildSeries(String name, List<BigDecimal> data) {
        return EnvironmentChartSeriesVO.builder()
                .name(name)
                .type("line")
                .data(data)
                .build();
    }
}
