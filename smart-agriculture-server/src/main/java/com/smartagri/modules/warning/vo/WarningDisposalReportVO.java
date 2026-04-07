package com.smartagri.modules.warning.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@Schema(description = "预警处置报告")
public class WarningDisposalReportVO {

    @Schema(description = "预警ID")
    private Long warningId;

    @Schema(description = "预警编号")
    private String warningNo;

    @Schema(description = "预警类型编码")
    private String warningType;

    @Schema(description = "预警类型名称")
    private String warningTypeName;

    @Schema(description = "风险等级")
    private Integer warningLevel;

    @Schema(description = "风险等级名称")
    private String warningLevelName;

    @Schema(description = "触发来源")
    private String triggerSource;

    @Schema(description = "触发来源名称")
    private String triggerSourceName;

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

    @Schema(description = "关联作物名称")
    private String relatedCropName;

    @Schema(description = "触发时间")
    private LocalDateTime triggerTime;

    @Schema(description = "环境数据快照")
    private WarningReportEnvSnapshotVO environmentSnapshot;

    @Schema(description = "AI原因分析摘要")
    private String aiAnalysisSummary;

    @Schema(description = "AI处置建议摘要")
    private String aiSuggestionSummary;

    @Schema(description = "当前处理状态")
    private Integer currentStatus;

    @Schema(description = "当前处理状态名称")
    private String currentStatusName;

    @Schema(description = "处理记录摘要")
    private String processRecordSummary;

    @Schema(description = "处理记录时间线")
    private List<WarningProcessLogVO> processLogs;

    @Schema(description = "是否闭环")
    private Boolean closedLoop;

    @Schema(description = "是否误报：0否，1是")
    private Integer isFalseAlarm;

    @Schema(description = "闭环时间")
    private LocalDateTime closeTime;

    @Schema(description = "报告摘要")
    private String reportSummary;

    @Schema(description = "报告生成时间")
    private LocalDateTime reportGenerateTime;
}
