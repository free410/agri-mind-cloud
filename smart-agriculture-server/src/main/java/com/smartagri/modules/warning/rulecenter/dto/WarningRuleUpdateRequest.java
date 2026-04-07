package com.smartagri.modules.warning.rulecenter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "编辑预警规则请求")
public class WarningRuleUpdateRequest {

    @NotBlank(message = "规则名称不能为空")
    @Schema(description = "规则名称")
    private String ruleName;

    @NotBlank(message = "预警类型不能为空")
    @Schema(description = "预警类型")
    private String warningType;

    @NotNull(message = "预警等级不能为空")
    @Min(value = 1, message = "预警等级取值不合法")
    @Max(value = 3, message = "预警等级取值不合法")
    @Schema(description = "预警等级")
    private Integer warningLevel;

    @NotBlank(message = "范围类型不能为空")
    @Schema(description = "范围类型：GLOBAL/FARMLAND/CROP")
    private String targetScopeType;

    @Schema(description = "范围ID（GLOBAL 可空）")
    private Long targetScopeId;

    @NotBlank(message = "指标键不能为空")
    @Schema(description = "指标键")
    private String metricKey;

    @NotBlank(message = "比较符不能为空")
    @Schema(description = "比较符")
    private String compareOperator;

    @NotNull(message = "阈值不能为空")
    @Schema(description = "阈值")
    private BigDecimal thresholdValue;

    @NotNull(message = "连续触发次数不能为空")
    @Min(value = 1, message = "连续触发次数不能小于1")
    @Max(value = 20, message = "连续触发次数不能超过20")
    @Schema(description = "连续触发次数")
    private Integer continuousCount;

    @NotNull(message = "重复抑制分钟数不能为空")
    @Min(value = 0, message = "重复抑制分钟数不能小于0")
    @Max(value = 1440, message = "重复抑制分钟数不能超过1440")
    @Schema(description = "重复抑制分钟数")
    private Integer deduplicateMinutes;

    @NotNull(message = "启用状态不能为空")
    @Min(value = 0, message = "启用状态取值不合法")
    @Max(value = 1, message = "启用状态取值不合法")
    @Schema(description = "启用状态")
    private Integer enabled;

    @Schema(description = "规则说明")
    private String description;
}

