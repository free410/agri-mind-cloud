package com.smartagri.modules.warning.rulecenter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "规则启用状态更新请求")
public class WarningRuleEnableRequest {

    @NotNull(message = "启用状态不能为空")
    @Min(value = 0, message = "启用状态取值不合法")
    @Max(value = 1, message = "启用状态取值不合法")
    @Schema(description = "启用状态：0禁用，1启用", example = "1")
    private Integer enabled;
}

