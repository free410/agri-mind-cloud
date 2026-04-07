package com.smartagri.modules.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "AI 对话响应")
public class ChatResponse {

    @Schema(description = "会话 ID")
    private String sessionId;

    @Schema(description = "问题内容")
    private String question;

    @Schema(description = "AI 回答")
    private String answer;

    @Schema(description = "关联模块")
    private String relatedModule;

    @Schema(description = "关联记录 ID")
    private Long relatedRecordId;

    @Schema(description = "模型名称")
    private String modelName;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
