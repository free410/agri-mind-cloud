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

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AdminServiceImpl extends ServiceImpl<AdminMapper, Admin> implements AdminService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration LOCK_WINDOW = Duration.ofMinutes(5);
    private static final int MAX_TRACKED_USERS = 10000;

    /** 单实例内的登录失败计数；多实例部署可替换为 Redis 等共享存储。 */
    private final Map<String, Deque<Long>> loginFailures = new ConcurrentHashMap<>();

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
        assertLoginAllowed(request.getUsername());
        Admin admin = findByUsername(request.getUsername());
        if (admin == null || !passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
            recordLoginFailure(request.getUsername());
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }
        if (!Integer.valueOf(1).equals(admin.getStatus())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "当前管理员账号已被禁用");
        }

        clearLoginFailures(request.getUsername());
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

    private void assertLoginAllowed(String username) {
        Deque<Long> failures = loginFailures.get(username);
        if (failures == null) {
            return;
        }
        long cutoff = System.currentTimeMillis() - LOCK_WINDOW.toMillis();
        synchronized (failures) {
            while (!failures.isEmpty() && failures.peekFirst() < cutoff) {
                failures.pollFirst();
            }
            if (failures.size() >= MAX_FAILED_ATTEMPTS) {
                long retryAfterSeconds = Math.max(1,
                        (failures.peekFirst() + LOCK_WINDOW.toMillis() - System.currentTimeMillis()) / 1000);
                throw new BusinessException(ResultCode.FORBIDDEN,
                        "登录失败次数过多，请 " + retryAfterSeconds + " 秒后再试");
            }
        }
    }

    private void recordLoginFailure(String username) {
        if (loginFailures.size() > MAX_TRACKED_USERS) {
            loginFailures.clear();
        }
        Deque<Long> failures = loginFailures.computeIfAbsent(username, key -> new ArrayDeque<>());
        long cutoff = System.currentTimeMillis() - LOCK_WINDOW.toMillis();
        synchronized (failures) {
            while (!failures.isEmpty() && failures.peekFirst() < cutoff) {
                failures.pollFirst();
            }
            failures.addLast(System.currentTimeMillis());
        }
    }

    private void clearLoginFailures(String username) {
        loginFailures.remove(username);
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
