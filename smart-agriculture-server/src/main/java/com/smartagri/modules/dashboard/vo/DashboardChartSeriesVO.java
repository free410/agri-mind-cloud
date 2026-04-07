package com.smartagri.modules.dashboard.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@Schema(description = "图表序列")
public class DashboardChartSeriesVO {

    @Schema(description = "序列名称")
    private String name;

    @Schema(description = "图表类型")
    private String type;

    @Schema(description = "数据列表")
    private List<BigDecimal> data;
}
