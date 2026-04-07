package com.smartagri.modules.environment.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.smartagri.common.result.Result;
import com.smartagri.modules.environment.dto.EnvironmentDataCreateRequest;
import com.smartagri.modules.environment.dto.EnvironmentDataPageQueryRequest;
import com.smartagri.modules.environment.dto.EnvironmentTrendQueryRequest;
import com.smartagri.modules.environment.service.EnvironmentDataService;
import com.smartagri.modules.environment.vo.EnvironmentDataListVO;
import com.smartagri.modules.environment.vo.EnvironmentTrendVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@Tag(name = "环境监测")
@RestController
@RequestMapping("/environment-data")
public class EnvironmentDataController {

    private final EnvironmentDataService environmentDataService;

    public EnvironmentDataController(EnvironmentDataService environmentDataService) {
        this.environmentDataService = environmentDataService;
    }

    @Operation(summary = "新增环境监测数据")
    @PostMapping
    public Result<Long> createEnvironmentData(@Valid @RequestBody EnvironmentDataCreateRequest request) {
        return Result.success("新增环境监测数据成功", environmentDataService.createEnvironmentData(request));
    }

    @Operation(summary = "分页查询环境监测数据列表")
    @GetMapping("/page")
    public Result<IPage<EnvironmentDataListVO>> pageEnvironmentData(@Valid EnvironmentDataPageQueryRequest request) {
        return Result.success(environmentDataService.pageEnvironmentData(request));
    }

    @Operation(summary = "查询农田近期环境趋势数据")
    @GetMapping("/trend")
    public Result<EnvironmentTrendVO> getTrendData(@Valid EnvironmentTrendQueryRequest request) {
        return Result.success(environmentDataService.getTrendData(request));
    }
}
