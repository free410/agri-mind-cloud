package com.smartagri.modules.warning.rulecenter.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("warning_rule_config")
@Schema(description = "预警规则配置")
public class WarningRuleConfig implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    @TableField("rule_name")
    @Schema(description = "规则名称")
    private String ruleName;

    @TableField("rule_code")
    @Schema(description = "规则编码")
    private String ruleCode;

    @TableField("warning_type")
    @Schema(description = "预警类型")
    private String warningType;

    @TableField("warning_level")
    @Schema(description = "预警等级：1低，2中，3高")
    private Integer warningLevel;

    @TableField("target_scope_type")
    @Schema(description = "范围类型：GLOBAL/FARMLAND/CROP")
    private String targetScopeType;

    @TableField("target_scope_id")
    @Schema(description = "范围ID（全局可空）")
    private Long targetScopeId;

    @TableField("metric_key")
    @Schema(description = "指标键：temperature/humidity/soil_moisture/light/device_offline")
    private String metricKey;

    @TableField("compare_operator")
    @Schema(description = "比较符：> / < / >= / <= / ==")
    private String compareOperator;

    @TableField("threshold_value")
    @Schema(description = "阈值")
    private BigDecimal thresholdValue;

    @TableField("continuous_count")
    @Schema(description = "连续触发次数")
    private Integer continuousCount;

    @TableField("deduplicate_minutes")
    @Schema(description = "重复抑制分钟数")
    private Integer deduplicateMinutes;

    @TableField("enabled")
    @Schema(description = "是否启用：0否，1是")
    private Integer enabled;

    @TableField("description")
    @Schema(description = "规则说明")
    private String description;

    @TableField("create_time")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableField("update_time")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}

