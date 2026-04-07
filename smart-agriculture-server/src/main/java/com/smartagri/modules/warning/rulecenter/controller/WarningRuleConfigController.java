package com.smartagri.modules.warning.rulecenter.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.smartagri.common.result.Result;
import com.smartagri.modules.warning.rulecenter.dto.WarningRuleCreateRequest;
import com.smartagri.modules.warning.rulecenter.dto.WarningRuleEnableRequest;
import com.smartagri.modules.warning.rulecenter.dto.WarningRulePageQueryRequest;
import com.smartagri.modules.warning.rulecenter.dto.WarningRuleUpdateRequest;
import com.smartagri.modules.warning.rulecenter.service.WarningRuleConfigService;
import com.smartagri.modules.warning.rulecenter.vo.WarningRuleListVO;
import com.smartagri.modules.warning.rulecenter.vo.WarningRuleSummaryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@Tag(name = "预警规则中心")
@RestController
@RequestMapping("/warning-rules")
public class WarningRuleConfigController {

    private final WarningRuleConfigService warningRuleConfigService;

    public WarningRuleConfigController(WarningRuleConfigService warningRuleConfigService) {
        this.warningRuleConfigService = warningRuleConfigService;
    }

    @Operation(summary = "分页查询规则列表")
    @GetMapping("/page")
    public Result<IPage<WarningRuleListVO>> pageRules(@Valid WarningRulePageQueryRequest request) {
        return Result.success(warningRuleConfigService.pageRules(request));
    }

    @Operation(summary = "规则统计摘要")
    @GetMapping("/summary")
    public Result<WarningRuleSummaryVO> getSummary() {
        return Result.success(warningRuleConfigService.getSummary());
    }

    @Operation(summary = "新增规则")
    @PostMapping
    public Result<Long> createRule(@Valid @RequestBody WarningRuleCreateRequest request) {
        return Result.success("新增规则成功", warningRuleConfigService.createRule(request));
    }

    @Operation(summary = "编辑规则")
    @PutMapping("/{id}")
    public Result<Void> updateRule(@PathVariable Long id, @Valid @RequestBody WarningRuleUpdateRequest request) {
        warningRuleConfigService.updateRule(id, request);
        return Result.success("编辑规则成功", null);
    }

    @Operation(summary = "启用/停用规则")
    @PutMapping("/{id}/enabled")
    public Result<Void> updateEnabled(@PathVariable Long id, @Valid @RequestBody WarningRuleEnableRequest request) {
        warningRuleConfigService.updateEnabled(id, request.getEnabled());
        return Result.success("更新规则状态成功", null);
    }

    @Operation(summary = "删除规则")
    @DeleteMapping("/{id}")
    public Result<Void> deleteRule(@PathVariable Long id) {
        warningRuleConfigService.deleteRule(id);
        return Result.success("删除规则成功", null);
    }
}

