package com.smartagri.modules.dashboard.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@Schema(description = "图表数据")
public class DashboardChartVO {

    @Schema(description = "横轴数据")
    private List<String> xAxis;

    @Schema(description = "图表序列")
    private List<DashboardChartSeriesVO> series;
}
