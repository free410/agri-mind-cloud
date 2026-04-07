package com.smartagri.modules.environment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@Schema(description = "环境监测数据分页查询参数")
public class EnvironmentDataPageQueryRequest {

    @Min(value = 1, message = "页码不能小于1")
    @Schema(description = "当前页码", example = "1")
    private long current = 1;

    @Min(value = 1, message = "每页条数不能小于1")
    @Max(value = 200, message = "每页条数不能超过200")
    @Schema(description = "每页条数", example = "10")
    private long size = 10;

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
}
