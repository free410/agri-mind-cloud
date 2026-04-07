package com.smartagri.modules.warning.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "预警闭环状态更新参数")
public class WarningStatusUpdateRequest {

    @NotNull(message = "预警ID不能为空")
    @Schema(description = "预警ID", example = "1")
    private Long id;

    @NotNull(message = "预警状态不能为空")
    @Min(value = 0, message = "预警状态取值不合法")
    @Max(value = 3, message = "预警状态取值不合法")
    @Schema(description = "闭环状态：0待确认，1处理中，2已处理，3已闭环", example = "2")
    private Integer status;

    @Schema(description = "处理人", example = "系统管理员")
    private String handlerName;

    @Schema(description = "处理说明", example = "已安排巡检，灌溉系统恢复正常。")
    private String handleRemark;

    @Min(value = 0, message = "误报标记取值不合法")
    @Max(value = 1, message = "误报标记取值不合法")
    @Schema(description = "是否误报：0否，1是", example = "0")
    private Integer isFalseAlarm;
}
