package com.smartagri.modules.auth.controller;

import com.smartagri.common.result.Result;
import com.smartagri.modules.auth.dto.AdminLoginRequest;
import com.smartagri.modules.auth.dto.AdminPasswordUpdateRequest;
import com.smartagri.modules.auth.service.AdminService;
import com.smartagri.modules.auth.vo.AdminInfoVO;
import com.smartagri.modules.auth.vo.AdminLoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理员认证")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AdminService adminService;

    public AuthController(AdminService adminService) {
        this.adminService = adminService;
    }

    @Operation(summary = "管理员登录")
    @PostMapping("/login")
    public Result<AdminLoginVO> login(@Valid @RequestBody AdminLoginRequest request) {
        return Result.success("登录成功", adminService.login(request));
    }

    @Operation(summary = "获取当前管理员信息")
    @GetMapping("/me")
    public Result<AdminInfoVO> getCurrentAdminInfo() {
        return Result.success(adminService.getCurrentAdminInfo());
    }

    @Operation(summary = "修改当前管理员密码")
    @PutMapping("/password")
    public Result<Void> updatePassword(@Valid @RequestBody AdminPasswordUpdateRequest request) {
        adminService.updatePassword(request);
        return Result.success("密码修改成功", null);
    }

    @Operation(summary = "管理员退出登录")
    @PostMapping("/logout")
    public Result<Void> logout() {
        adminService.logout();
        return Result.success("退出登录成功", null);
    }
}
