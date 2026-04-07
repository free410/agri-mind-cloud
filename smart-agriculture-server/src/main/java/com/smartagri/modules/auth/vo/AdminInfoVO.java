package com.smartagri.modules.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "当前管理员信息")
public class AdminInfoVO {

    @Schema(description = "管理员ID")
    private Long id;

    @Schema(description = "管理员用户名")
    private String username;

    @Schema(description = "管理员昵称")
    private String nickname;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "头像地址")
    private String avatar;

    @Schema(description = "最后登录时间")
    private LocalDateTime lastLoginTime;
}
