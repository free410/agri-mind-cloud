package com.smartagri.modules.warning.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "提交预警处理结果请求")
public class WarningHandleSubmitRequest {

    @NotBlank(message = "操作人不能为空")
    @Size(max = 64, message = "操作人长度不能超过64")
    @Schema(description = "操作人", example = "系统管理员")
    private String operatorName;

    @NotBlank(message = "处理结果不能为空")
    @Size(max = 1000, message = "处理结果长度不能超过1000")
    @Schema(description = "处理结果说明", example = "已完成灌溉与通风联动，指标恢复正常。")
    private String handleRemark;
}
