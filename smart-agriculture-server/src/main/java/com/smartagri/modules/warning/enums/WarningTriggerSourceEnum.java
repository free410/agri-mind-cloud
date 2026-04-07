package com.smartagri.modules.warning.enums;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum WarningTriggerSourceEnum {

    ENV_MONITOR("ENV_MONITOR", "环境监测"),
    DEVICE_STATUS("DEVICE_STATUS", "设备状态"),
    MANUAL_INPUT("MANUAL_INPUT", "手动录入"),
    RULE_ENGINE("RULE_ENGINE", "规则引擎");

    private final String code;
    private final String description;

    WarningTriggerSourceEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static boolean contains(String code) {
        return Arrays.stream(values()).anyMatch(item -> item.code.equals(code));
    }

    public static String getDescriptionByCode(String code) {
        return Arrays.stream(values())
                .filter(item -> item.code.equals(code))
                .map(WarningTriggerSourceEnum::getDescription)
                .findFirst()
                .orElse("未知来源");
    }
}
