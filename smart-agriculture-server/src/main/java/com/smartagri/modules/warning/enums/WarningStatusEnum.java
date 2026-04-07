package com.smartagri.modules.warning.enums;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum WarningStatusEnum {

    TO_CONFIRM(0, "待确认"),
    PROCESSING(1, "处理中"),
    HANDLED(2, "已处理"),
    CLOSED_LOOP(3, "已闭环");

    private final Integer code;
    private final String description;

    WarningStatusEnum(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public static boolean contains(Integer code) {
        return Arrays.stream(values()).anyMatch(item -> item.code.equals(code));
    }

    public static String getDescriptionByCode(Integer code) {
        return Arrays.stream(values())
                .filter(item -> item.code.equals(code))
                .map(WarningStatusEnum::getDescription)
                .findFirst()
                .orElse("未知状态");
    }
}
