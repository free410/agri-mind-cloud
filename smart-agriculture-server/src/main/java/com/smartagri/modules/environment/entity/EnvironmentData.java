package com.smartagri.modules.environment.entity;

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
@TableName("env_data")
@Schema(description = "环境监测数据实体")
public class EnvironmentData implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    @TableField("farmland_id")
    @Schema(description = "农田ID")
    private Long farmlandId;

    @TableField("device_id")
    @Schema(description = "设备ID")
    private Long deviceId;

    @TableField("temperature")
    @Schema(description = "温度")
    private BigDecimal temperature;

    @TableField("humidity")
    @Schema(description = "湿度")
    private BigDecimal humidity;

    @TableField("soil_moisture")
    @Schema(description = "土壤湿度")
    private BigDecimal soilMoisture;

    @TableField("light_intensity")
    @Schema(description = "光照强度")
    private BigDecimal lightIntensity;

    @TableField("ph_value")
    @Schema(description = "PH值")
    private BigDecimal phValue;

    @TableField("collect_time")
    @Schema(description = "采集时间")
    private LocalDateTime collectTime;

    @TableField("create_time")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
