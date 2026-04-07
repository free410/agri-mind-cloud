package com.smartagri.modules.warning.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "预警处置报告-环境数据快照")
public class WarningReportEnvSnapshotVO {

    @Schema(description = "农田ID")
    private Long farmlandId;

    @Schema(description = "设备ID")
    private Long deviceId;

    @Schema(description = "温度")
    private BigDecimal temperature;

    @Schema(description = "湿度")
    private BigDecimal humidity;

    @Schema(description = "土壤湿度")
    private BigDecimal soilMoisture;

    @Schema(description = "光照强度")
    private BigDecimal lightIntensity;

    @Schema(description = "pH值")
    private BigDecimal phValue;

    @Schema(description = "采集时间")
    private LocalDateTime collectTime;
}
