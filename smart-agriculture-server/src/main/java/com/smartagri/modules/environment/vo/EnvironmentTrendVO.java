package com.smartagri.modules.environment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@Schema(description = "环境趋势图返回对象")
public class EnvironmentTrendVO {

    @Schema(description = "农田ID")
    private Long farmlandId;

    @Schema(description = "农田名称")
    private String farmlandName;

    @Schema(description = "设备ID")
    private Long deviceId;

    @Schema(description = "设备名称")
    private String deviceName;

    @Schema(description = "X轴时间数据")
    private List<String> xAxis;

    @Schema(description = "图表序列")
    private List<EnvironmentChartSeriesVO> series;
}
