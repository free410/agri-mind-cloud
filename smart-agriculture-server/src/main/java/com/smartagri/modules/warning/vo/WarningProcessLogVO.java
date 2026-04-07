package com.smartagri.modules.warning.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "预警处理日志展示对象")
public class WarningProcessLogVO {

    @Schema(description = "日志ID")
    private Long id;

    @Schema(description = "动作类型编码")
    private String actionType;

    @Schema(description = "动作类型名称")
    private String actionTypeName;

    @Schema(description = "流转前状态")
    private Integer statusFrom;

    @Schema(description = "流转前状态名称")
    private String statusFromName;

    @Schema(description = "流转后状态")
    private Integer statusTo;

    @Schema(description = "流转后状态名称")
    private String statusToName;

    @Schema(description = "操作人")
    private String operatorName;

    @Schema(description = "操作说明")
    private String processNote;

    @Schema(description = "操作时间")
    private LocalDateTime processTime;
}
