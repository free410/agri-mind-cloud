package com.smartagri.modules.warning.rulecenter.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "预警规则列表项")
public class WarningRuleListVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "规则名称")
    private String ruleName;

    @Schema(description = "规则编码")
    private String ruleCode;

    @Schema(description = "预警类型")
    private String warningType;

    @Schema(description = "预警等级")
    private Integer warningLevel;

    @Schema(description = "范围类型")
    private String targetScopeType;

    @Schema(description = "范围类型名称")
    private String targetScopeTypeName;

    @Schema(description = "范围ID")
    private Long targetScopeId;

    @Schema(description = "指标键")
    private String metricKey;

    @Schema(description = "指标名称")
    private String metricName;

    @Schema(description = "比较符")
    private String compareOperator;

    @Schema(description = "阈值")
    private BigDecimal thresholdValue;

    @Schema(description = "连续触发次数")
    private Integer continuousCount;

    @Schema(description = "重复抑制分钟数")
    private Integer deduplicateMinutes;

    @Schema(description = "是否启用：0否，1是")
    private Integer enabled;

    @Schema(description = "规则说明")
    private String description;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}

