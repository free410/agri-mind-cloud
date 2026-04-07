package com.smartagri.modules.environment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "新增环境监测数据请求参数")
public class EnvironmentDataCreateRequest {

    @NotNull(message = "农田ID不能为空")
    @Schema(description = "农田ID", example = "1")
    private Long farmlandId;

    @NotNull(message = "设备ID不能为空")
    @Schema(description = "设备ID", example = "1")
    private Long deviceId;

    @NotNull(message = "温度不能为空")
    @DecimalMin(value = "-50.00", message = "温度值过低")
    @DecimalMax(value = "100.00", message = "温度值过高")
    @Schema(description = "温度(℃)", example = "26.5")
    private BigDecimal temperature;

    @NotNull(message = "湿度不能为空")
    @DecimalMin(value = "0.00", message = "湿度值不能小于0")
    @DecimalMax(value = "100.00", message = "湿度值不能大于100")
    @Schema(description = "空气湿度(%)", example = "68.0")
    private BigDecimal humidity;

    @NotNull(message = "土壤湿度不能为空")
    @DecimalMin(value = "0.00", message = "土壤湿度值不能小于0")
    @DecimalMax(value = "100.00", message = "土壤湿度值不能大于100")
    @Schema(description = "土壤湿度(%)", example = "35.5")
    private BigDecimal soilMoisture;

    @NotNull(message = "光照强度不能为空")
    @DecimalMin(value = "0.00", message = "光照强度不能小于0")
    @Schema(description = "光照强度(lux)", example = "22000")
    private BigDecimal lightIntensity;

    @NotNull(message = "PH值不能为空")
    @DecimalMin(value = "0.00", message = "PH值不能小于0")
    @DecimalMax(value = "14.00", message = "PH值不能大于14")
    @Schema(description = "PH值", example = "6.5")
    private BigDecimal phValue;

    @NotNull(message = "采集时间不能为空")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "采集时间", example = "2026-03-31 09:30:00")
    private LocalDateTime collectTime;
}
