package com.smartagri.modules.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartagri.common.exception.BusinessException;
import com.smartagri.common.result.ResultCode;
import com.smartagri.modules.device.dto.DeviceCreateRequest;
import com.smartagri.modules.device.dto.DevicePageQueryRequest;
import com.smartagri.modules.device.dto.DeviceUpdateRequest;
import com.smartagri.modules.device.entity.Device;
import com.smartagri.modules.device.enums.DeviceOnlineStatusEnum;
import com.smartagri.modules.device.enums.DeviceRunStatusEnum;
import com.smartagri.modules.device.mapper.DeviceMapper;
import com.smartagri.modules.device.service.DeviceService;
import com.smartagri.modules.device.vo.DeviceDetailVO;
import com.smartagri.modules.device.vo.DeviceListVO;
import com.smartagri.modules.field.entity.Field;
import com.smartagri.modules.field.service.FieldService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DeviceServiceImpl extends ServiceImpl<DeviceMapper, Device> implements DeviceService {

    private final FieldService fieldService;

    public DeviceServiceImpl(FieldService fieldService) {
        this.fieldService = fieldService;
    }

    @Override
    public Long createDevice(DeviceCreateRequest request) {
        validateFarmlandExists(request.getFarmlandId());
        validateStatus(request.getOnlineStatus(), request.getRunStatus());
        validateInstallTime(request.getInstallTime());
        checkDeviceCodeUnique(request.getDeviceCode(), null);

        Device device = new Device();
        device.setDeviceCode(request.getDeviceCode().trim());
        device.setDeviceName(request.getDeviceName().trim());
        device.setDeviceType(request.getDeviceType().trim());
        device.setFarmlandId(request.getFarmlandId());
        device.setInstallTime(request.getInstallTime());
        device.setOnlineStatus(request.getOnlineStatus());
        device.setRunStatus(request.getRunStatus());
        device.setRemark(StringUtils.trimToNull(request.getRemark()));
        save(device);
        return device.getId();
    }

    @Override
    public void updateDevice(DeviceUpdateRequest request) {
        Device device = getDeviceOrThrow(request.getId());
        validateFarmlandExists(request.getFarmlandId());
        validateStatus(request.getOnlineStatus(), request.getRunStatus());
        validateInstallTime(request.getInstallTime());
        checkDeviceCodeUnique(request.getDeviceCode(), request.getId());

        device.setDeviceCode(request.getDeviceCode().trim());
        device.setDeviceName(request.getDeviceName().trim());
        device.setDeviceType(request.getDeviceType().trim());
        device.setFarmlandId(request.getFarmlandId());
        device.setInstallTime(request.getInstallTime());
        device.setOnlineStatus(request.getOnlineStatus());
        device.setRunStatus(request.getRunStatus());
        device.setRemark(StringUtils.trimToNull(request.getRemark()));
        updateById(device);
    }

    @Override
    public void deleteDevice(Long id) {
        Device device = getDeviceOrThrow(id);
        removeById(device.getId());
    }

    @Override
    public DeviceDetailVO getDeviceDetail(Long id) {
        Device device = getDeviceOrThrow(id);
        Field field = fieldService.getById(device.getFarmlandId());
        return buildDetailVO(device, field);
    }

    @Override
    public IPage<DeviceListVO> pageDevices(DevicePageQueryRequest request) {
        String deviceType = StringUtils.trimToNull(request.getDeviceType());

        Page<Device> page = new Page<>(request.getCurrent(), request.getSize());
        LambdaQueryWrapper<Device> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(StringUtils.isNotBlank(deviceType), Device::getDeviceType, deviceType)
                .eq(request.getFarmlandId() != null, Device::getFarmlandId, request.getFarmlandId())
                .eq(request.getOnlineStatus() != null, Device::getOnlineStatus, request.getOnlineStatus())
                .orderByDesc(Device::getCreateTime);

        Page<Device> devicePage = page(page, queryWrapper);
        Map<Long, Field> fieldMap = loadFieldMap(devicePage.getRecords());

        Page<DeviceListVO> resultPage = new Page<>(devicePage.getCurrent(), devicePage.getSize(), devicePage.getTotal());
        resultPage.setRecords(devicePage.getRecords().stream()
                .map(device -> buildListVO(device, fieldMap.get(device.getFarmlandId())))
                .toList());
        return resultPage;
    }

    private Device getDeviceOrThrow(Long id) {
        Device device = getById(id);
        if (device == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "设备不存在");
        }
        return device;
    }

    private void validateFarmlandExists(Long farmlandId) {
        if (fieldService.getById(farmlandId) == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "所属农田不存在");
        }
    }

    private void validateStatus(Integer onlineStatus, Integer runStatus) {
        if (!DeviceOnlineStatusEnum.contains(onlineStatus)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "设备在线状态取值不合法");
        }
        if (!DeviceRunStatusEnum.contains(runStatus)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "设备运行状态取值不合法");
        }
    }

    private void validateInstallTime(LocalDateTime installTime) {
        if (installTime.isAfter(LocalDateTime.now().plusYears(1))) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "设备安装时间不能晚于当前时间过多");
        }
    }

    private void checkDeviceCodeUnique(String deviceCode, Long excludeId) {
        LambdaQueryWrapper<Device> queryWrapper = new LambdaQueryWrapper<Device>()
                .eq(Device::getDeviceCode, deviceCode.trim());
        if (excludeId != null) {
            queryWrapper.ne(Device::getId, excludeId);
        }

        if (count(queryWrapper) > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "设备编号已存在");
        }
    }

    private Map<Long, Field> loadFieldMap(List<Device> devices) {
        if (devices == null || devices.isEmpty()) {
            return Collections.emptyMap();
        }

        Set<Long> fieldIds = devices.stream()
                .map(Device::getFarmlandId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return fieldService.listByIds(fieldIds).stream()
                .collect(Collectors.toMap(Field::getId, Function.identity()));
    }

    private DeviceListVO buildListVO(Device device, Field field) {
        return DeviceListVO.builder()
                .id(device.getId())
                .deviceCode(device.getDeviceCode())
                .deviceName(device.getDeviceName())
                .deviceType(device.getDeviceType())
                .farmlandId(device.getFarmlandId())
                .farmlandName(field == null ? null : field.getFieldName())
                .installTime(device.getInstallTime())
                .onlineStatus(device.getOnlineStatus())
                .onlineStatusName(DeviceOnlineStatusEnum.getDescriptionByCode(device.getOnlineStatus()))
                .runStatus(device.getRunStatus())
                .runStatusName(DeviceRunStatusEnum.getDescriptionByCode(device.getRunStatus()))
                .remark(device.getRemark())
                .createTime(device.getCreateTime())
                .build();
    }

    private DeviceDetailVO buildDetailVO(Device device, Field field) {
        return DeviceDetailVO.builder()
                .id(device.getId())
                .deviceCode(device.getDeviceCode())
                .deviceName(device.getDeviceName())
                .deviceType(device.getDeviceType())
                .farmlandId(device.getFarmlandId())
                .farmlandName(field == null ? null : field.getFieldName())
                .installTime(device.getInstallTime())
                .onlineStatus(device.getOnlineStatus())
                .onlineStatusName(DeviceOnlineStatusEnum.getDescriptionByCode(device.getOnlineStatus()))
                .runStatus(device.getRunStatus())
                .runStatusName(DeviceRunStatusEnum.getDescriptionByCode(device.getRunStatus()))
                .remark(device.getRemark())
                .createTime(device.getCreateTime())
                .updateTime(device.getUpdateTime())
                .build();
    }
}
