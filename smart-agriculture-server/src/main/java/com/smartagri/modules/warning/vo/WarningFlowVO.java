package com.smartagri.modules.warning.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@Schema(description = "预警状态流信息")
public class WarningFlowVO {

    @Schema(description = "预警ID")
    private Long warningId;

    @Schema(description = "预警编号")
    private String warningNo;

    @Schema(description = "当前状态")
    private Integer currentStatus;

    @Schema(description = "当前状态名称")
    private String currentStatusName;

    @Schema(description = "是否误报：0否，1是")
    private Integer isFalseAlarm;

    @Schema(description = "当前处理人")
    private String handlerName;

    @Schema(description = "处理时间")
    private LocalDateTime handleTime;

    @Schema(description = "闭环时间")
    private LocalDateTime closeTime;

    @Schema(description = "状态流日志")
    private List<WarningProcessLogVO> processLogs;
}
