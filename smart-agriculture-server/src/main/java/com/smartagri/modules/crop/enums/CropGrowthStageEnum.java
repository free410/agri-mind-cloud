package com.smartagri.modules.crop.enums;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum CropGrowthStageEnum {

    SEEDING("SEEDING", "播种期"),
    SEEDLING("SEEDLING", "苗期"),
    GROWING("GROWING", "生长期"),
    FLOWERING("FLOWERING", "开花期"),
    FRUITING("FRUITING", "结果期"),
    MATURING("MATURING", "成熟期"),
    HARVESTED("HARVESTED", "已采收");

    private final String code;
    private final String description;

    CropGrowthStageEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static boolean contains(String code) {
        return Arrays.stream(values()).anyMatch(item -> item.code.equals(code));
    }

    public static String getDescriptionByCode(String code) {
        return Arrays.stream(values())
                .filter(item -> item.code.equals(code))
                .map(CropGrowthStageEnum::getDescription)
                .findFirst()
                .orElse("未知阶段");
    }
}
