package com.smartagri.modules.environment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@Schema(description = "环境趋势图查询参数")
public class EnvironmentTrendQueryRequest {

    @NotNull(message = "农田ID不能为空")
    @Schema(description = "农田ID", example = "1")
    private Long farmlandId;

    @Schema(description = "设备ID", example = "1")
    private Long deviceId;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "开始时间", example = "2026-03-31 00:00:00")
    private LocalDateTime startTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "结束时间", example = "2026-03-31 23:59:59")
    private LocalDateTime endTime;

    @Min(value = 1, message = "趋势点数量不能小于1")
    @Max(value = 100, message = "趋势点数量不能超过100")
    @Schema(description = "返回数据点数量", example = "12")
    private Integer limit = 12;
}
