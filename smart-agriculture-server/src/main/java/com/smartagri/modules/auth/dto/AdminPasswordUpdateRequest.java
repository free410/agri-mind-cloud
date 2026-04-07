package com.smartagri.modules.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "管理员修改密码请求参数")
public class AdminPasswordUpdateRequest {

    @NotBlank(message = "原密码不能为空")
    @Schema(description = "原密码", example = "123456")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 20, message = "新密码长度必须在6到20位之间")
    @Schema(description = "新密码", example = "654321")
    private String newPassword;

    @NotBlank(message = "确认密码不能为空")
    @Schema(description = "确认密码", example = "654321")
    private String confirmPassword;
}
