package com.smartagri.modules.environment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@Schema(description = "图表序列数据")
public class EnvironmentChartSeriesVO {

    @Schema(description = "序列名称")
    private String name;

    @Schema(description = "图表类型", example = "line")
    private String type;

    @Schema(description = "数值数组")
    private List<BigDecimal> data;
}
