package com.smartagri.modules.warning.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@Schema(description = "预警分页查询参数")
public class WarningPageQueryRequest {

    @Min(value = 1, message = "页码不能小于1")
    @Schema(description = "当前页码", example = "1")
    private long current = 1;

    @Min(value = 1, message = "每页条数不能小于1")
    @Max(value = 100, message = "每页条数不能超过100")
    @Schema(description = "每页条数", example = "10")
    private long size = 10;

    @Min(value = 1, message = "预警等级取值不合法")
    @Max(value = 3, message = "预警等级取值不合法")
    @Schema(description = "预警等级：1低，2中，3高", example = "2")
    private Integer level;

    @Min(value = 0, message = "预警状态取值不合法")
    @Max(value = 3, message = "预警状态取值不合法")
    @Schema(description = "闭环状态：0待确认，1处理中，2已处理，3已闭环", example = "1")
    private Integer status;

    @Schema(description = "农田ID", example = "1")
    private Long farmlandId;

    @Schema(description = "触发来源：ENV_MONITOR/DEVICE_STATUS/MANUAL_INPUT/RULE_ENGINE", example = "ENV_MONITOR")
    private String triggerSource;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "触发开始时间", example = "2026-04-01 00:00:00")
    private LocalDateTime triggerStartTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "触发结束时间", example = "2026-04-02 23:59:59")
    private LocalDateTime triggerEndTime;
}
