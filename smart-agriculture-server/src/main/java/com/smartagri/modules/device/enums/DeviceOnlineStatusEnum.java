package com.smartagri.modules.device.enums;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum DeviceOnlineStatusEnum {

    OFFLINE(0, "离线"),
    ONLINE(1, "在线"),
    FAULT(2, "故障");

    private final Integer code;
    private final String description;

    DeviceOnlineStatusEnum(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public static boolean contains(Integer code) {
        return Arrays.stream(values()).anyMatch(item -> item.code.equals(code));
    }

    public static String getDescriptionByCode(Integer code) {
        return Arrays.stream(values())
                .filter(item -> item.code.equals(code))
                .map(DeviceOnlineStatusEnum::getDescription)
                .findFirst()
                .orElse("未知状态");
    }
}
