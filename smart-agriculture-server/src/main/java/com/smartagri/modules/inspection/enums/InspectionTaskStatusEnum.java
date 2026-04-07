package com.smartagri.modules.inspection.enums;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum InspectionTaskStatusEnum {

    SUCCESS("SUCCESS", "成功"),
    FAILED("FAILED", "失败"),
    SKIPPED("SKIPPED", "跳过");

    private final String code;
    private final String description;

    InspectionTaskStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static String getDescriptionByCode(String code) {
        return Arrays.stream(values())
                .filter(item -> item.code.equals(code))
                .map(InspectionTaskStatusEnum::getDescription)
                .findFirst()
                .orElse("未知");
    }
}

