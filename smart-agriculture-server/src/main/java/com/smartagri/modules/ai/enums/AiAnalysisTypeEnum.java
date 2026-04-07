package com.smartagri.modules.ai.enums;

import com.smartagri.common.exception.BusinessException;
import com.smartagri.common.result.ResultCode;
import lombok.Getter;

import java.util.Arrays;

@Getter
public enum AiAnalysisTypeEnum {

    ENVIRONMENT("ENVIRONMENT", "环境数据分析"),
    WARNING("WARNING", "预警分析");

    private final String code;
    private final String description;

    AiAnalysisTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static AiAnalysisTypeEnum fromCode(String code) {
        return Arrays.stream(values())
                .filter(item -> item.code.equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ResultCode.BAD_REQUEST, "不支持的 AI 分析类型"));
    }
}
