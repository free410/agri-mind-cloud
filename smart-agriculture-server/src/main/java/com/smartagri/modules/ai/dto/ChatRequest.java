package com.smartagri.modules.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "AI 对话请求参数")
public class ChatRequest {

    @Schema(description = "会话 ID，为空时由后端自动生成", example = "session-20260401-001")
    private String sessionId;

    @NotBlank(message = "问题内容不能为空")
    @Size(max = 1000, message = "问题内容长度不能超过 1000 个字符")
    @Schema(description = "管理员问题", example = "番茄生长期土壤湿度偏低会有什么影响？")
    private String question;

    @Schema(description = "关联模块，可选值：GENERAL、ENVIRONMENT、WARNING", example = "GENERAL")
    private String relatedModule;

    @Schema(description = "关联记录 ID，可选", example = "1")
    private Long relatedRecordId;
}
