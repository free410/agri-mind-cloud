package com.smartagri.modules.dashboard.controller;

import com.smartagri.common.result.Result;
import com.smartagri.modules.dashboard.service.DashboardService;
import com.smartagri.modules.dashboard.vo.DashboardHomeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "首页看板")
@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @Operation(summary = "获取首页看板聚合数据")
    @GetMapping("/home")
    public Result<DashboardHomeVO> getHomeDashboard() {
        return Result.success(dashboardService.getHomeDashboard());
    }
}
