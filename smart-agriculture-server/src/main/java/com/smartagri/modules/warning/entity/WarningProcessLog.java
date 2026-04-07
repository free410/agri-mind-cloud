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
@TableName("warning_process_log")
@Schema(description = "预警处理过程日志")
public class WarningProcessLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("warning_id")
    private Long warningId;

    @TableField("action_type")
    private String actionType;

    @TableField("status_from")
    private Integer statusFrom;

    @TableField("status_to")
    private Integer statusTo;

    @TableField("operator_name")
    private String operatorName;

    @TableField("process_note")
    private String processNote;

    @TableField("process_time")
    private LocalDateTime processTime;

    @TableField("create_time")
    private LocalDateTime createTime;
}
