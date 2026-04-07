package com.smartagri.modules.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@Schema(description = "AI 分析响应")
public class AnalyzeResponse {

    @Schema(description = "会话 ID")
    private String sessionId;

    @Schema(description = "分析类型")
    private String analysisType;

    @Schema(description = "分析标题")
    private String title;

    @Schema(description = "分析结果")
    private String answer;

    @Schema(description = "引用的业务数据")
    private Map<String, Object> referencedData;

    @Schema(description = "模型名称")
    private String modelName;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
