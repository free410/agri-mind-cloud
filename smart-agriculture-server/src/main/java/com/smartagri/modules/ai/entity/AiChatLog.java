package com.smartagri.modules.ai.entity;

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
@TableName("ai_chat_log")
@Schema(description = "AI 对话记录实体")
public class AiChatLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    @TableField("admin_id")
    @Schema(description = "管理员ID")
    private Long adminId;

    @TableField("session_id")
    @Schema(description = "会话ID")
    private String sessionId;

    @TableField("question")
    @Schema(description = "问题内容")
    private String question;

    @TableField("answer")
    @Schema(description = "AI 回答")
    private String answer;

    @TableField("related_module")
    @Schema(description = "关联模块")
    private String relatedModule;

    @TableField("related_record_id")
    @Schema(description = "关联记录ID")
    private Long relatedRecordId;

    @TableField("model_name")
    @Schema(description = "模型名称")
    private String modelName;

    @TableField("context_snapshot")
    @Schema(description = "上下文快照")
    private String contextSnapshot;

    @TableField("response_status")
    @Schema(description = "响应状态：1成功，0失败")
    private Integer responseStatus;

    @TableField("response_time_ms")
    @Schema(description = "响应耗时（毫秒）")
    private Integer responseTimeMs;

    @TableField("create_time")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableField("update_time")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
