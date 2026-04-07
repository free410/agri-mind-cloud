package com.smartagri.modules.warning.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "预警详情返回对象")
public class WarningDetailVO {

    @Schema(description = "预警ID")
    private Long id;

    @Schema(description = "预警编号")
    private String warningNo;

    @Schema(description = "预警类型编码")
    private String warningType;

    @Schema(description = "预警类型名称")
    private String warningTypeName;

    @Schema(description = "预警内容")
    private String content;

    @Schema(description = "预警等级")
    private Integer level;

    @Schema(description = "预警等级名称")
    private String levelName;

    @Schema(description = "闭环状态")
    private Integer status;

    @Schema(description = "闭环状态名称")
    private String statusName;

    @Schema(description = "所属农田ID")
    private Long farmlandId;

    @Schema(description = "所属农田名称")
    private String farmlandName;

    @Schema(description = "关联设备ID")
    private Long relatedDeviceId;

    @Schema(description = "关联设备名称")
    private String relatedDeviceName;

    @Schema(description = "关联作物ID")
    private Long relatedCropId;

    @Schema(description = "触发来源")
    private String triggerSource;

    @Schema(description = "触发来源名称")
    private String triggerSourceName;

    @Schema(description = "触发时间")
    private LocalDateTime triggerTime;

    @Schema(description = "AI分析摘要")
    private String aiAnalysisSummary;

    @Schema(description = "AI建议摘要")
    private String aiSuggestionSummary;

    @Schema(description = "处理人")
    private String handlerName;

    @Schema(description = "处理时间")
    private LocalDateTime handleTime;

    @Schema(description = "处理说明")
    private String handleRemark;

    @Schema(description = "闭环时间")
    private LocalDateTime closeTime;

    @Schema(description = "是否误报：0否，1是")
    private Integer isFalseAlarm;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
