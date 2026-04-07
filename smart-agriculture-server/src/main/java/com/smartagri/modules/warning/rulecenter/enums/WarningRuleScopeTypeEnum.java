package com.smartagri.modules.warning.rulecenter.enums;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum WarningRuleScopeTypeEnum {

    GLOBAL("GLOBAL", "全局规则"),
    FARMLAND("FARMLAND", "农田定向"),
    CROP("CROP", "作物定向");

    private final String code;
    private final String description;

    WarningRuleScopeTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static boolean contains(String code) {
        return Arrays.stream(values()).anyMatch(item -> item.code.equalsIgnoreCase(code));
    }

    public static String getDescriptionByCode(String code) {
        return Arrays.stream(values())
                .filter(item -> item.code.equalsIgnoreCase(code))
                .map(WarningRuleScopeTypeEnum::getDescription)
                .findFirst()
                .orElse("未知范围");
    }
}

