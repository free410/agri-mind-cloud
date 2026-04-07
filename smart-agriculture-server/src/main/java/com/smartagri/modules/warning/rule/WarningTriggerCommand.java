package com.smartagri.modules.warning.rule;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class WarningTriggerCommand {

    /**
     * 对应 WarningTypeEnum code
     */
    private String warningType;

    /**
     * 对应 WarningLevelEnum code
     */
    private Integer warningLevel;

    /**
     * 对应 WarningTriggerSourceEnum code
     */
    private String triggerSource;

    private Long farmlandId;

    private Long relatedDeviceId;

    private Long relatedCropId;

    private LocalDateTime triggerTime;

    /**
     * 去重抑制分钟数
     */
    private Integer deduplicateMinutes;

    /**
     * 来源规则ID（可为空）
     */
    private Long ruleId;

    /**
     * 来源规则编码（可为空）
     */
    private String ruleCode;

    /**
     * 来源规则名称（可为空）
     */
    private String ruleName;

    private String content;
}
