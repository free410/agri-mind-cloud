package com.smartagri.modules.warning.rule;

import com.smartagri.modules.device.entity.Device;
import com.smartagri.modules.environment.entity.EnvironmentData;

import java.util.List;

/**
 * 自动预警规则引擎（第一版）
 */
public interface WarningRuleEngineService {

    /**
     * 环境监测数据触发规则
     */
    List<WarningTriggerCommand> evaluateEnvironmentData(EnvironmentData environmentData);

    /**
     * 设备状态触发规则
     */
    List<WarningTriggerCommand> evaluateDeviceStatus(Device device);
}
