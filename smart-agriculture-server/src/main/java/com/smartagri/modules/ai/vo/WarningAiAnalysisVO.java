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
@Schema(description = "预警AI分析结果")
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
public class WarningAiAnalysisVO {

    @Schema(description = "预警ID")
    private Long warningId;

    @Schema(description = "预警概述")
    private String warningOverview;

    @Schema(description = "可能原因")
    private List<String> possibleCauses;

    @Schema(description = "风险影响")
    private String riskImpact;

    @Schema(description = "推荐处理建议")
    private List<String> recommendations;

    @Schema(description = "优先级：1低，2中，3高")
    private Integer priorityLevel;

    @Schema(description = "是否建议人工复核")
    private Boolean manualReviewRequired;

    @Schema(description = "模型名称")
    private String modelName;

    @Schema(description = "生成时间")
    private LocalDateTime createTime;
}
