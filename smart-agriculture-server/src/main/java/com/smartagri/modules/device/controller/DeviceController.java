package com.smartagri.modules.device.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.smartagri.common.result.Result;
import com.smartagri.modules.device.dto.DeviceCreateRequest;
import com.smartagri.modules.device.dto.DevicePageQueryRequest;
import com.smartagri.modules.device.dto.DeviceUpdateRequest;
import com.smartagri.modules.device.enums.DeviceOnlineStatusEnum;
import com.smartagri.modules.device.service.DeviceService;
import com.smartagri.modules.device.vo.DeviceDetailVO;
import com.smartagri.modules.device.vo.DeviceListVO;
import com.smartagri.modules.warning.service.WarningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@Tag(name = "设备管理")
@RestController
@RequestMapping("/devices")
public class DeviceController {

    private final DeviceService deviceService;
    private final WarningService warningService;

    public DeviceController(DeviceService deviceService, WarningService warningService) {
        this.deviceService = deviceService;
        this.warningService = warningService;
    }

    @Operation(summary = "新增设备")
    @PostMapping
    public Result<Long> createDevice(@Valid @RequestBody DeviceCreateRequest request) {
        Long deviceId = deviceService.createDevice(request);
        if (DeviceOnlineStatusEnum.OFFLINE.getCode().equals(request.getOnlineStatus())) {
            warningService.generateDeviceOfflineWarning(deviceId);
        }
        return Result.success("新增设备成功", deviceId);
    }

    @Operation(summary = "修改设备")
    @PutMapping("/{id}")
    public Result<Void> updateDevice(@PathVariable Long id, @Valid @RequestBody DeviceUpdateRequest request) {
        request.setId(id);
        deviceService.updateDevice(request);
        if (DeviceOnlineStatusEnum.OFFLINE.getCode().equals(request.getOnlineStatus())) {
            warningService.generateDeviceOfflineWarning(id);
        }
        return Result.success("修改设备成功", null);
    }

    @Operation(summary = "删除设备")
    @DeleteMapping("/{id}")
    public Result<Void> deleteDevice(@PathVariable Long id) {
        deviceService.deleteDevice(id);
        return Result.success("删除设备成功", null);
    }

    @Operation(summary = "查看设备详情")
    @GetMapping("/{id}")
    public Result<DeviceDetailVO> getDeviceDetail(@PathVariable Long id) {
        return Result.success(deviceService.getDeviceDetail(id));
    }

    @Operation(summary = "分页查询设备列表")
    @GetMapping("/page")
    public Result<IPage<DeviceListVO>> pageDevices(@Valid DevicePageQueryRequest request) {
        return Result.success(deviceService.pageDevices(request));
    }
}
