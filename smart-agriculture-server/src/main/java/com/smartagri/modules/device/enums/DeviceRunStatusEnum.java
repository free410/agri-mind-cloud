package com.smartagri.modules.device.enums;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum DeviceRunStatusEnum {

    STOPPED(0, "停用"),
    RUNNING(1, "运行中"),
    STANDBY(2, "待机中"),
    ABNORMAL(3, "异常");

    private final Integer code;
    private final String description;

    DeviceRunStatusEnum(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public static boolean contains(Integer code) {
        return Arrays.stream(values()).anyMatch(item -> item.code.equals(code));
    }

    public static String getDescriptionByCode(Integer code) {
        return Arrays.stream(values())
                .filter(item -> item.code.equals(code))
                .map(DeviceRunStatusEnum::getDescription)
                .findFirst()
                .orElse("未知状态");
    }
}
