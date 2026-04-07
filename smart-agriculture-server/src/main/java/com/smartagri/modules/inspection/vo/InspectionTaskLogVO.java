package com.smartagri.modules.inspection.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "巡检日志条目")
public class InspectionTaskLogVO {

    @Schema(description = "日志ID")
    private Long id;

    @Schema(description = "巡检时间")
    private LocalDateTime inspectionTime;

    @Schema(description = "环境数据条数")
    private Integer inspectEnvCount;

    @Schema(description = "设备巡检条数")
    private Integer inspectDeviceCount;

    @Schema(description = "检测总数据条数")
    private Integer detectDataCount;

    @Schema(description = "触发预警条数")
    private Integer triggeredWarningCount;

    @Schema(description = "巡检状态编码")
    private String status;

    @Schema(description = "巡检状态名称")
    private String statusName;

    @Schema(description = "巡检摘要")
    private String summary;

    @Schema(description = "触发方式（SCHEDULED/MANUAL）")
    private String triggerMode;
}

