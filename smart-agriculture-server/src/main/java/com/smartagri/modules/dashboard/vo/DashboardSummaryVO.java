package com.smartagri.modules.dashboard.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
@Schema(description = "首页闭环核心指标")
public class DashboardSummaryVO {

    @Schema(description = "农田总数")
    private Long farmlandCount;

    @Schema(description = "作物总数")
    private Long cropCount;

    @Schema(description = "设备总数")
    private Long deviceCount;

    @Schema(description = "今日预警总数")
    private Long todayWarningCount;

    @Schema(description = "高风险预警数")
    private Long highRiskWarningCount;

    @Schema(description = "待处理预警数")
    private Long pendingWarningCount;

    @Schema(description = "已闭环预警数")
    private Long closedWarningCount;

    @Schema(description = "闭环率（%）")
    private BigDecimal closureRate;

    @Schema(description = "平均闭环时长（分钟）")
    private Long avgClosureMinutes;
}
