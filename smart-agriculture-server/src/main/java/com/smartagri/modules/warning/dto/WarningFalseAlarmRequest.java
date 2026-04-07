package com.smartagri.modules.warning.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "预警误报标记请求")
public class WarningFalseAlarmRequest {

    @NotBlank(message = "操作人不能为空")
    @Size(max = 64, message = "操作人长度不能超过64")
    @Schema(description = "操作人", example = "系统管理员")
    private String operatorName;

    @NotBlank(message = "误报说明不能为空")
    @Size(max = 1000, message = "误报说明长度不能超过1000")
    @Schema(description = "误报说明", example = "传感器短时波动导致误报，现场核验正常。")
    private String falseAlarmReason;
}
