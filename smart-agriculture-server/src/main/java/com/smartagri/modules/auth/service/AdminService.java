package com.smartagri.modules.auth.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartagri.modules.auth.dto.AdminLoginRequest;
import com.smartagri.modules.auth.dto.AdminPasswordUpdateRequest;
import com.smartagri.modules.auth.entity.Admin;
import com.smartagri.modules.auth.vo.AdminInfoVO;
import com.smartagri.modules.auth.vo.AdminLoginVO;

public interface AdminService extends IService<Admin> {

    AdminLoginVO login(AdminLoginRequest request);

    AdminInfoVO getCurrentAdminInfo();

    void updatePassword(AdminPasswordUpdateRequest request);

    void logout();
}
