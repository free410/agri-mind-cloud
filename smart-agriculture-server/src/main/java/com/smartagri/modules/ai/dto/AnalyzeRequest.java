package com.smartagri.modules.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "AI 分析请求参数")
public class AnalyzeRequest {

    @Schema(description = "会话 ID，为空时由后端自动生成", example = "session-20260401-002")
    private String sessionId;

    @NotBlank(message = "分析类型不能为空")
    @Schema(description = "分析类型，可选值：ENVIRONMENT 或 WARNING", example = "ENVIRONMENT")
    private String analysisType;

    @Schema(description = "环境数据 ID，环境分析时可传", example = "1")
    private Long environmentDataId;

    @Schema(description = "农田 ID，环境分析时可传", example = "1")
    private Long farmlandId;

    @Schema(description = "设备 ID，环境分析时可传", example = "2")
    private Long deviceId;

    @Schema(description = "预警 ID，预警分析时必传", example = "3")
    private Long warningId;

    @Size(max = 1000, message = "补充问题长度不能超过 1000 个字符")
    @Schema(description = "补充问题，可选", example = "请结合当前数据给出三条处理建议")
    private String question;
}
