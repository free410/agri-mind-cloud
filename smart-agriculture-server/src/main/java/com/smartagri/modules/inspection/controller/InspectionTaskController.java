package com.smartagri.modules.inspection.controller;

import com.smartagri.common.result.Result;
import com.smartagri.modules.inspection.service.InspectionTaskService;
import com.smartagri.modules.inspection.vo.InspectionTaskSummaryVO;
import com.smartagri.security.LoginAdminContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@Tag(name = "巡检任务")
@RestController
@RequestMapping("/inspection/tasks")
public class InspectionTaskController {

    private final InspectionTaskService inspectionTaskService;

    public InspectionTaskController(InspectionTaskService inspectionTaskService) {
        this.inspectionTaskService = inspectionTaskService;
    }

    @Operation(summary = "获取巡检摘要")
    @GetMapping("/summary")
    public Result<InspectionTaskSummaryVO> getSummary(
            @RequestParam(defaultValue = "5") Integer recentLimit) {
        return Result.success(inspectionTaskService.getInspectionSummary(recentLimit));
    }

    @Operation(summary = "手动触发一次巡检")
    @PostMapping("/run-once")
    public Result<Void> runOnce() {
        String operatorName = LoginAdminContext.getUsername();
        inspectionTaskService.executeManualInspection(operatorName);
        return Result.success("巡检任务已触发", null);
    }
}

