package com.smartagri.modules.inspection.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@Schema(description = "巡检任务摘要")
public class InspectionTaskSummaryVO {

    @Schema(description = "最近巡检时间")
    private LocalDateTime latestInspectionTime;

    @Schema(description = "今日巡检次数")
    private Long todayInspectionCount;

    @Schema(description = "今日新增预警数")
    private Long todayTriggeredWarningCount;

    @Schema(description = "巡检状态：RUNNING/STOPPED")
    private String taskStatus;

    @Schema(description = "当前是否在执行巡检")
    private Boolean executing;

    @Schema(description = "巡检开关")
    private Boolean enabled;

    @Schema(description = "间隔毫秒")
    private Long fixedDelayMs;

    @Schema(description = "最近巡检日志")
    private List<InspectionTaskLogVO> recentLogs;
}

