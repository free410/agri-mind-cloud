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
@TableName("warning_ai_analysis")
@Schema(description = "预警AI分析记录")
public class WarningAiAnalysis implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("warning_id")
    private Long warningId;

    @TableField("analysis_source")
    private String analysisSource;

    @TableField("analysis_summary")
    private String analysisSummary;

    @TableField("suggestion_summary")
    private String suggestionSummary;

    @TableField("risk_score")
    private Integer riskScore;

    @TableField("priority_level")
    private Integer priorityLevel;

    @TableField("raw_response")
    private String rawResponse;

    @TableField("create_time")
    private LocalDateTime createTime;
}
