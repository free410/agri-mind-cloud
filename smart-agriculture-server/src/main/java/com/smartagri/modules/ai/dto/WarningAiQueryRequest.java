package com.smartagri.modules.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "预警AI分析查询参数")
public class WarningAiQueryRequest {

    @NotNull(message = "预警ID不能为空")
    @Schema(description = "预警ID", example = "1001")
    private Long warningId;
}
