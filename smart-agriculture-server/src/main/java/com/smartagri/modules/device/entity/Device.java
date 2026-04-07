package com.smartagri.modules.device.entity;

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
@TableName("device")
@Schema(description = "设备实体")
public class Device implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    @TableField("device_code")
    @Schema(description = "设备编号")
    private String deviceCode;

    @TableField("device_name")
    @Schema(description = "设备名称")
    private String deviceName;

    @TableField("device_type")
    @Schema(description = "设备类型")
    private String deviceType;

    @TableField("farmland_id")
    @Schema(description = "所属农田ID")
    private Long farmlandId;

    @TableField("install_time")
    @Schema(description = "安装时间")
    private LocalDateTime installTime;

    @TableField("online_status")
    @Schema(description = "在线状态：0离线，1在线，2故障")
    private Integer onlineStatus;

    @TableField("run_status")
    @Schema(description = "运行状态：0停用，1运行中，2待机中，3异常")
    private Integer runStatus;

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
