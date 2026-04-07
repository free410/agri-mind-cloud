package com.smartagri.modules.warning.enums;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum WarningLevelEnum {

    LOW(1, "低"),
    MEDIUM(2, "中"),
    HIGH(3, "高");

    private final Integer code;
    private final String description;

    WarningLevelEnum(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public static boolean contains(Integer code) {
        return Arrays.stream(values()).anyMatch(item -> item.code.equals(code));
    }

    public static String getDescriptionByCode(Integer code) {
        return Arrays.stream(values())
                .filter(item -> item.code.equals(code))
                .map(WarningLevelEnum::getDescription)
                .findFirst()
                .orElse("未知等级");
    }
}
