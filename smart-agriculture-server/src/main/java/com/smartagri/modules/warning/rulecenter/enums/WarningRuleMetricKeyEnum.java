package com.smartagri.modules.warning.rulecenter.enums;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum WarningRuleMetricKeyEnum {

    TEMPERATURE("temperature", "温度"),
    HUMIDITY("humidity", "湿度"),
    SOIL_MOISTURE("soil_moisture", "土壤湿度"),
    LIGHT("light", "光照"),
    DEVICE_OFFLINE("device_offline", "设备离线");

    private final String code;
    private final String description;

    WarningRuleMetricKeyEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static boolean contains(String code) {
        return Arrays.stream(values()).anyMatch(item -> item.code.equalsIgnoreCase(code));
    }

    public static String getDescriptionByCode(String code) {
        return Arrays.stream(values())
                .filter(item -> item.code.equalsIgnoreCase(code))
                .map(WarningRuleMetricKeyEnum::getDescription)
                .findFirst()
                .orElse("未知指标");
    }
}

