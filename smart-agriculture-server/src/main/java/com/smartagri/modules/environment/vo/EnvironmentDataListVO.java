package com.smartagri.modules.environment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "环境监测数据列表项")
public class EnvironmentDataListVO {

    @Schema(description = "监测数据ID")
    private Long id;

    @Schema(description = "农田ID")
    private Long farmlandId;

    @Schema(description = "农田名称")
    private String farmlandName;

    @Schema(description = "设备ID")
    private Long deviceId;

    @Schema(description = "设备名称")
    private String deviceName;

    @Schema(description = "设备类型")
    private String deviceType;

    @Schema(description = "温度(℃)")
    private BigDecimal temperature;

    @Schema(description = "湿度(%)")
    private BigDecimal humidity;

    @Schema(description = "土壤湿度(%)")
    private BigDecimal soilMoisture;

    @Schema(description = "光照强度(lux)")
    private BigDecimal lightIntensity;

    @Schema(description = "PH值")
    private BigDecimal phValue;

    @Schema(description = "采集时间")
    private LocalDateTime collectTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
