package com.smartagri.modules.auth.entity;

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
@TableName("admin")
@Schema(description = "管理员实体")
public class Admin implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    @TableField("username")
    @Schema(description = "登录账号")
    private String username;

    @TableField("password")
    @Schema(description = "登录密码")
    private String password;

    @TableField("nickname")
    @Schema(description = "管理员昵称")
    private String nickname;

    @TableField("phone")
    @Schema(description = "手机号")
    private String phone;

    @TableField("email")
    @Schema(description = "邮箱")
    private String email;

    @TableField("avatar")
    @Schema(description = "头像地址")
    private String avatar;

    @TableField("status")
    @Schema(description = "状态：1正常，0禁用")
    private Integer status;

    @TableField("last_login_time")
    @Schema(description = "最后登录时间")
    private LocalDateTime lastLoginTime;

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
