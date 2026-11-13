package com.smartagri.modules.ai.vo;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@Schema(description = "预警快捷聚合分析结果")
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
public class WarningQuickSummaryVO {

    @Schema(description = "模式：abnormal/closure")
    private String mode;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "结论")
    private String conclusion;

    @Schema(description = "可能原因")
    private List<String> possibleCauses;

    @Schema(description = "建议")
    private List<String> suggestions;

    @Schema(description = "优先级：1低 2中 3高")
    private Integer priorityLevel;

    @Schema(description = "建议人工复核")
    private Boolean manualReviewRequired;

    @Schema(description = "关联预警ID（可选）")
    private Long relatedWarningId;

    @Schema(description = "总预警数")
    private Long totalWarnings;

    @Schema(description = "高风险预警数")
    private Long highRiskWarnings;

    @Schema(description = "待处理预警数")
    private Long pendingWarnings;

    @Schema(description = "已闭环预警数")
    private Long closedWarnings;

    @Schema(description = "闭环率（%）")
    private Double closureRate;

    @Schema(description = "平均闭环时长（分钟）")
    private Long avgClosureMinutes;

    @Schema(description = "生成时间")
    private LocalDateTime generateTime;
}
