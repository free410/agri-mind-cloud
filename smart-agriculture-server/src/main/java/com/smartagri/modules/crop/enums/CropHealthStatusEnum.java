package com.smartagri.modules.crop.enums;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum CropHealthStatusEnum {

    HEALTHY("HEALTHY", "健康"),
    ATTENTION("ATTENTION", "需关注"),
    DISEASE_RISK("DISEASE_RISK", "病害风险"),
    ABNORMAL("ABNORMAL", "异常");

    private final String code;
    private final String description;

    CropHealthStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static boolean contains(String code) {
        return Arrays.stream(values()).anyMatch(item -> item.code.equals(code));
    }

    public static String getDescriptionByCode(String code) {
        return Arrays.stream(values())
                .filter(item -> item.code.equals(code))
                .map(CropHealthStatusEnum::getDescription)
                .findFirst()
                .orElse("未知状态");
    }
}
