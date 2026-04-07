package com.smartagri.modules.crop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("crop")
@Schema(description = "作物实体")
public class Crop implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    @TableField("crop_name")
    @Schema(description = "作物名称")
    private String cropName;

    @TableField("crop_type")
    @Schema(description = "作物类型")
    private String cropType;

    @TableField("farmland_id")
    @Schema(description = "所属农田ID")
    private Long farmlandId;

    @TableField("sowing_date")
    @Schema(description = "播种日期")
    private LocalDate sowingDate;

    @TableField("maturity_date")
    @Schema(description = "预计成熟日期")
    private LocalDate maturityDate;

    @TableField("growth_stage")
    @Schema(description = "生长阶段编码")
    private String growthStage;

    @TableField("health_status")
    @Schema(description = "健康状态编码")
    private String healthStatus;

    @TableField("remark")
    @Schema(description = "备注")
    private String remark;

    @TableField("create_time")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableField("update_time")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
