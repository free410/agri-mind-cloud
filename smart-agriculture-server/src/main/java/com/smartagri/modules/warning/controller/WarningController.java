package com.smartagri.modules.warning.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.smartagri.common.result.Result;
import com.smartagri.modules.warning.dto.WarningFalseAlarmRequest;
import com.smartagri.modules.warning.dto.WarningFlowActionRequest;
import com.smartagri.modules.warning.dto.WarningHandleSubmitRequest;
import com.smartagri.modules.warning.dto.WarningPageQueryRequest;
import com.smartagri.modules.warning.dto.WarningStatusUpdateRequest;
import com.smartagri.modules.warning.service.WarningService;
import com.smartagri.modules.warning.vo.WarningDetailVO;
import com.smartagri.modules.warning.vo.WarningDisposalReportVO;
import com.smartagri.modules.warning.vo.WarningFlowVO;
import com.smartagri.modules.warning.vo.WarningListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@Tag(name = "预警管理")
@RestController
@RequestMapping("/warnings")
public class WarningController {

    private final WarningService warningService;

    public WarningController(WarningService warningService) {
        this.warningService = warningService;
    }

    @Operation(summary = "分页查询预警列表")
    @GetMapping("/page")
    public Result<IPage<WarningListVO>> pageWarnings(@Valid WarningPageQueryRequest request) {
        return Result.success(warningService.pageWarnings(request));
    }

    @Operation(summary = "查看预警详情")
    @GetMapping("/{id}")
    public Result<WarningDetailVO> getWarningDetail(@PathVariable Long id) {
        return Result.success(warningService.getWarningDetail(id));
    }

    @Operation(summary = "兼容旧版状态更新接口")
    @PutMapping("/status")
    public Result<Void> updateWarningStatus(@Valid @RequestBody WarningStatusUpdateRequest request) {
        warningService.updateWarningStatus(request);
        return Result.success("更新预警状态成功", null);
    }

    @Operation(summary = "确认预警")
    @PutMapping("/{id}/confirm")
    public Result<Void> confirmWarning(@PathVariable Long id, @Valid @RequestBody WarningFlowActionRequest request) {
        warningService.confirmWarning(id, request);
        return Result.success("确认预警成功", null);
    }

    @Operation(summary = "标记为处理中")
    @PutMapping("/{id}/processing")
    public Result<Void> markProcessing(@PathVariable Long id, @Valid @RequestBody WarningFlowActionRequest request) {
        warningService.markProcessing(id, request);
        return Result.success("预警已进入处理流程", null);
    }

    @Operation(summary = "提交处理结果")
    @PutMapping("/{id}/handle-result")
    public Result<Void> submitHandleResult(@PathVariable Long id, @Valid @RequestBody WarningHandleSubmitRequest request) {
        warningService.submitHandleResult(id, request);
        return Result.success("提交处理结果成功", null);
    }

    @Operation(summary = "标记为已闭环")
    @PutMapping("/{id}/close-loop")
    public Result<Void> closeWarning(@PathVariable Long id, @Valid @RequestBody WarningFlowActionRequest request) {
        warningService.closeWarning(id, request);
        return Result.success("预警已闭环", null);
    }

    @Operation(summary = "标记误报并闭环")
    @PutMapping("/{id}/false-alarm")
    public Result<Void> markFalseAlarm(@PathVariable Long id, @Valid @RequestBody WarningFalseAlarmRequest request) {
        warningService.markFalseAlarm(id, request);
        return Result.success("误报标记成功，预警已闭环", null);
    }

    @Operation(summary = "获取预警状态流转信息")
    @GetMapping("/{id}/flow")
    public Result<WarningFlowVO> getWarningFlow(@PathVariable Long id) {
        return Result.success(warningService.getWarningFlow(id));
    }

    @Operation(summary = "生成预警处置报告")
    @GetMapping("/{id}/disposal-report")
    public Result<WarningDisposalReportVO> generateDisposalReport(@PathVariable Long id) {
        return Result.success(warningService.generateDisposalReport(id));
    }
}
