package com.smartagri.modules.field.entity;

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
@TableName("farmland")
@Schema(description = "农田实体")
public class Field implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    @TableField("field_name")
    @Schema(description = "农田名称")
    private String fieldName;

    @TableField("area")
    @Schema(description = "农田面积")
    private BigDecimal area;

    @TableField("location")
    @Schema(description = "地理位置")
    private String location;

    @TableField("soil_type")
    @Schema(description = "土壤类型")
    private String soilType;

    @TableField("status")
    @Schema(description = "农田状态：0闲置，1种植中，2维护中")
    private Integer status;

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
