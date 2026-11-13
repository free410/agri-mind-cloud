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
@Schema(description = "预警处置建议结果")
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
public class WarningDisposalSuggestionVO {

    @Schema(description = "预警ID")
    private Long warningId;

    @Schema(description = "推荐处理步骤")
    private List<String> steps;

    @Schema(description = "推荐检查对象")
    private List<String> checkTargets;

    @Schema(description = "是否需要联动设备巡检")
    private Boolean needDeviceInspection;

    @Schema(description = "简短总结")
    private String summary;

    @Schema(description = "模型名称")
    private String modelName;

    @Schema(description = "生成时间")
    private LocalDateTime createTime;
}
