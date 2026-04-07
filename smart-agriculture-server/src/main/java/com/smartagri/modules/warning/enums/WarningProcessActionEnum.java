package com.smartagri.modules.warning.enums;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum WarningProcessActionEnum {

    AUTO_GENERATE("AUTO_GENERATE", "自动生成预警"),
    CONFIRM("CONFIRM", "确认预警"),
    START_PROCESS("START_PROCESS", "开始处理"),
    SUBMIT_RESULT("SUBMIT_RESULT", "提交处理结果"),
    CLOSE_LOOP("CLOSE_LOOP", "标记已闭环"),
    MARK_FALSE_ALARM("MARK_FALSE_ALARM", "标记误报"),
    STATUS_UPDATE("STATUS_UPDATE", "状态更新"),
    INIT_IMPORT("INIT_IMPORT", "历史初始化");

    private final String code;
    private final String description;

    WarningProcessActionEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static String getDescriptionByCode(String code) {
        return Arrays.stream(values())
                .filter(item -> item.code.equals(code))
                .map(WarningProcessActionEnum::getDescription)
                .findFirst()
                .orElse("未知动作");
    }
}
