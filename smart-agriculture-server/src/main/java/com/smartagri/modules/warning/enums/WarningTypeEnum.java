package com.smartagri.modules.warning.enums;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum WarningTypeEnum {

    TEMPERATURE_HIGH("TEMPERATURE_HIGH", "温度过高"),
    TEMPERATURE_LOW("TEMPERATURE_LOW", "温度过低"),
    HUMIDITY_HIGH("HUMIDITY_HIGH", "空气湿度过高"),
    SOIL_MOISTURE_LOW("SOIL_MOISTURE_LOW", "土壤湿度过低"),
    LIGHT_ABNORMAL("LIGHT_ABNORMAL", "光照异常"),
    LIGHT_INTENSITY_HIGH("LIGHT_INTENSITY_HIGH", "光照强度过高"),
    DEVICE_OFFLINE("DEVICE_OFFLINE", "设备离线"),
    DEVICE_MAINTENANCE("DEVICE_MAINTENANCE", "设备维护");

    private final String code;
    private final String description;

    WarningTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static boolean contains(String code) {
        return Arrays.stream(values()).anyMatch(item -> item.code.equals(code));
    }

    public static String getDescriptionByCode(String code) {
        return Arrays.stream(values())
                .filter(item -> item.code.equals(code))
                .map(WarningTypeEnum::getDescription)
                .findFirst()
                .orElse("未知预警");
    }
}
