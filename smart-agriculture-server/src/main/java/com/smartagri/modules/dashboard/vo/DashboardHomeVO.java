package com.smartagri.modules.dashboard.vo;

import com.smartagri.modules.inspection.vo.InspectionTaskSummaryVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@Schema(description = "首页看板聚合数据")
public class DashboardHomeVO {

    @Schema(description = "核心指标")
    private DashboardSummaryVO summary;

    @Schema(description = "农田状态分布")
    private List<DashboardNameValueVO> farmlandStatusList;

    @Schema(description = "作物生长阶段分布")
    private List<DashboardNameValueVO> cropGrowthStageList;

    @Schema(description = "环境趋势")
    private DashboardChartVO environmentTrend;

    @Schema(description = "预警等级分布")
    private List<DashboardNameValueVO> warningLevelList;

    @Schema(description = "预警状态分布")
    private List<DashboardNameValueVO> warningStatusList;

    @Schema(description = "高风险预警优先队列（Top3）")
    private List<DashboardWarningQueueVO> highRiskWarningQueue;

    @Schema(description = "今日AI建议摘要")
    private List<String> aiSuggestionList;

    @Schema(description = "最新巡检结果")
    private InspectionTaskSummaryVO inspectionSummary;

    @Schema(description = "最近7天闭环趋势")
    private DashboardChartVO closureTrend;
}
