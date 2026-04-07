package com.smartagri.modules.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartagri.common.exception.BusinessException;
import com.smartagri.common.properties.JwtProperties;
import com.smartagri.common.result.ResultCode;
import com.smartagri.common.util.JwtUtils;
import com.smartagri.modules.auth.dto.AdminLoginRequest;
import com.smartagri.modules.auth.dto.AdminPasswordUpdateRequest;
import com.smartagri.modules.auth.entity.Admin;
import com.smartagri.modules.auth.mapper.AdminMapper;
import com.smartagri.modules.auth.service.AdminService;
import com.smartagri.modules.auth.vo.AdminInfoVO;
import com.smartagri.modules.auth.vo.AdminLoginVO;
import com.smartagri.security.LoginAdminContext;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AdminServiceImpl extends ServiceImpl<AdminMapper, Admin> implements AdminService {

    private final JwtUtils jwtUtils;
    private final JwtProperties jwtProperties;
    private final PasswordEncoder passwordEncoder;

    public AdminServiceImpl(JwtUtils jwtUtils, JwtProperties jwtProperties, PasswordEncoder passwordEncoder) {
        this.jwtUtils = jwtUtils;
        this.jwtProperties = jwtProperties;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public AdminLoginVO login(AdminLoginRequest request) {
        Admin admin = findByUsername(request.getUsername());
        if (admin == null || !passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }
        if (!Integer.valueOf(1).equals(admin.getStatus())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "当前管理员账号已被禁用");
        }

        admin.setLastLoginTime(LocalDateTime.now());
        updateById(admin);

        String token = jwtUtils.generateToken(admin.getId(), admin.getUsername());
        return AdminLoginVO.builder()
                .token(token)
                .tokenType(jwtProperties.getTokenPrefix())
                .expireHours(jwtProperties.getExpireHours())
                .adminId(admin.getId())
                .username(admin.getUsername())
                .adminInfo(buildAdminInfo(admin))
                .build();
    }

    @Override
    public AdminInfoVO getCurrentAdminInfo() {
        return buildAdminInfo(getCurrentAdminEntity());
    }

    @Override
    public void updatePassword(AdminPasswordUpdateRequest request) {
        if (!StringUtils.equals(request.getNewPassword(), request.getConfirmPassword())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "两次输入的新密码不一致");
        }
        if (StringUtils.equals(request.getOldPassword(), request.getNewPassword())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "新密码不能与原密码相同");
        }

        Admin admin = getCurrentAdminEntity();
        if (!passwordEncoder.matches(request.getOldPassword(), admin.getPassword())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "原密码输入错误");
        }

        admin.setPassword(passwordEncoder.encode(request.getNewPassword()));
        updateById(admin);
    }

    @Override
    public void logout() {
        if (LoginAdminContext.getAdminId() == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "未获取到当前登录管理员信息");
        }
    }

    private Admin getCurrentAdminEntity() {
        Long adminId = LoginAdminContext.getAdminId();
        if (adminId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "未获取到当前登录管理员信息");
        }

        Admin admin = getById(adminId);
        if (admin == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "管理员不存在");
        }
        return admin;
    }

    private Admin findByUsername(String username) {
        return getOne(new LambdaQueryWrapper<Admin>()
                .eq(Admin::getUsername, username)
                .last("LIMIT 1"));
    }

    private AdminInfoVO buildAdminInfo(Admin admin) {
        return AdminInfoVO.builder()
                .id(admin.getId())
                .username(admin.getUsername())
                .nickname(admin.getNickname())
                .phone(admin.getPhone())
                .email(admin.getEmail())
                .avatar(admin.getAvatar())
                .lastLoginTime(admin.getLastLoginTime())
                .build();
    }
}
