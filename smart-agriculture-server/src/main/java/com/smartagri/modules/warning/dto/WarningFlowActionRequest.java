package com.smartagri.modules.warning.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "预警状态流转通用请求")
public class WarningFlowActionRequest {

    @NotBlank(message = "操作人不能为空")
    @Size(max = 64, message = "操作人长度不能超过64")
    @Schema(description = "操作人", example = "系统管理员")
    private String operatorName;

    @Size(max = 1000, message = "处理说明长度不能超过1000")
    @Schema(description = "处理说明", example = "现场确认预警有效，进入处理流程。")
    private String processNote;
}
