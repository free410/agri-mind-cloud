package com.smartagri.modules.warning.rulecenter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
@Schema(description = "预警规则分页查询参数")
public class WarningRulePageQueryRequest {

    @Min(value = 1, message = "页码不能小于1")
    @Schema(description = "当前页码", example = "1")
    private long current = 1;

    @Min(value = 1, message = "每页条数不能小于1")
    @Max(value = 100, message = "每页条数不能超过100")
    @Schema(description = "每页条数", example = "10")
    private long size = 10;

    @Schema(description = "规则名称（模糊）")
    private String ruleName;

    @Schema(description = "预警类型", example = "SOIL_MOISTURE_LOW")
    private String warningType;

    @Min(value = 0, message = "启用状态取值不合法")
    @Max(value = 1, message = "启用状态取值不合法")
    @Schema(description = "启用状态：0禁用，1启用")
    private Integer enabled;
}

