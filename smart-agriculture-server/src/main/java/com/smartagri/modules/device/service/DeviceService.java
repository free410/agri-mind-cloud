package com.smartagri.modules.device.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.smartagri.modules.device.dto.DeviceCreateRequest;
import com.smartagri.modules.device.dto.DevicePageQueryRequest;
import com.smartagri.modules.device.dto.DeviceUpdateRequest;
import com.smartagri.modules.device.entity.Device;
import com.smartagri.modules.device.vo.DeviceDetailVO;
import com.smartagri.modules.device.vo.DeviceListVO;

public interface DeviceService extends IService<Device> {

    Long createDevice(DeviceCreateRequest request);

    void updateDevice(DeviceUpdateRequest request);

    void deleteDevice(Long id);

    DeviceDetailVO getDeviceDetail(Long id);

    IPage<DeviceListVO> pageDevices(DevicePageQueryRequest request);
}
