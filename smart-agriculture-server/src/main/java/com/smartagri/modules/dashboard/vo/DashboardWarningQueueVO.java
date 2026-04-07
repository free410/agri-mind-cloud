package com.smartagri.modules.dashboard.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "首页高风险预警队列项")
public class DashboardWarningQueueVO {

    @Schema(description = "预警ID")
    private Long id;

    @Schema(description = "预警编号")
    private String warningNo;

    @Schema(description = "预警类型")
    private String warningTypeName;

    @Schema(description = "风险等级")
    private String levelName;

    @Schema(description = "当前状态")
    private String statusName;

    @Schema(description = "所属农田")
    private String farmlandName;

    @Schema(description = "关联设备")
    private String relatedDeviceName;

    @Schema(description = "触发时间")
    private LocalDateTime triggerTime;
}
