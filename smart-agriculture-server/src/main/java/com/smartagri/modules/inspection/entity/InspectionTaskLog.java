package com.smartagri.modules.inspection.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("inspection_task_log")
public class InspectionTaskLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("inspection_time")
    private LocalDateTime inspectionTime;

    @TableField("inspect_env_count")
    private Integer inspectEnvCount;

    @TableField("inspect_device_count")
    private Integer inspectDeviceCount;

    @TableField("detect_data_count")
    private Integer detectDataCount;

    @TableField("triggered_warning_count")
    private Integer triggeredWarningCount;

    @TableField("status")
    private String status;

    @TableField("summary")
    private String summary;

    @TableField("error_message")
    private String errorMessage;

    @TableField("trigger_mode")
    private String triggerMode;

    @TableField("create_time")
    private LocalDateTime createTime;
}

