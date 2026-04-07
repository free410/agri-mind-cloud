package com.smartagri.modules.warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("warning")
@Schema(description = "预警主记录")
public class WarningRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    @TableField("warning_no")
    @Schema(description = "预警编号")
    private String warningNo;

    @TableField("warning_type")
    @Schema(description = "预警类型")
    private String warningType;

    @TableField("content")
    @Schema(description = "预警内容")
    private String content;

    @TableField("level")
    @Schema(description = "预警等级：1低，2中，3高")
    private Integer level;

    @TableField("status")
    @Schema(description = "闭环状态：0待确认，1处理中，2已处理，3已闭环")
    private Integer status;

    @TableField("farmland_id")
    @Schema(description = "所属农田ID")
    private Long farmlandId;

    @TableField("related_device_id")
    @Schema(description = "关联设备ID")
    private Long relatedDeviceId;

    @TableField("related_crop_id")
    @Schema(description = "关联作物ID")
    private Long relatedCropId;

    @TableField("trigger_source")
    @Schema(description = "触发来源")
    private String triggerSource;

    @TableField("trigger_time")
    @Schema(description = "触发时间")
    private LocalDateTime triggerTime;

    @TableField("ai_analysis_summary")
    @Schema(description = "AI分析摘要")
    private String aiAnalysisSummary;

    @TableField("ai_suggestion_summary")
    @Schema(description = "AI建议摘要")
    private String aiSuggestionSummary;

    @TableField("handler_name")
    @Schema(description = "处理人")
    private String handlerName;

    @TableField("handle_time")
    @Schema(description = "处理时间")
    private LocalDateTime handleTime;

    @TableField("handle_remark")
    @Schema(description = "处理说明")
    private String handleRemark;

    @TableField("close_time")
    @Schema(description = "闭环时间")
    private LocalDateTime closeTime;

    @TableField("is_false_alarm")
    @Schema(description = "是否误报：0否，1是")
    private Integer isFalseAlarm;

    @TableField("create_time")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableField("update_time")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
