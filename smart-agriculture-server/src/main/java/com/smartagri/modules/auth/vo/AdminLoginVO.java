package com.smartagri.modules.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "管理员登录返回结果")
public class AdminLoginVO {

    @Schema(description = "JWT 登录令牌")
    private String token;

    @Schema(description = "令牌类型")
    private String tokenType;

    @Schema(description = "令牌有效时长，单位小时")
    private Integer expireHours;

    @Schema(description = "管理员 ID")
    private Long adminId;

    @Schema(description = "管理员用户名")
    private String username;

    @Schema(description = "管理员信息")
    private AdminInfoVO adminInfo;
}
