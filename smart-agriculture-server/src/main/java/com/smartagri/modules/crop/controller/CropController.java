package com.smartagri.modules.crop.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.smartagri.common.result.Result;
import com.smartagri.modules.crop.dto.CropCreateRequest;
import com.smartagri.modules.crop.dto.CropPageQueryRequest;
import com.smartagri.modules.crop.dto.CropUpdateRequest;
import com.smartagri.modules.crop.service.CropService;
import com.smartagri.modules.crop.vo.CropDetailVO;
import com.smartagri.modules.crop.vo.CropListVO;
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
@Tag(name = "作物管理")
@RestController
@RequestMapping("/crops")
public class CropController {

    private final CropService cropService;

    public CropController(CropService cropService) {
        this.cropService = cropService;
    }

    @Operation(summary = "新增作物")
    @PostMapping
    public Result<Long> createCrop(@Valid @RequestBody CropCreateRequest request) {
        return Result.success("新增作物成功", cropService.createCrop(request));
    }

    @Operation(summary = "修改作物")
    @PutMapping("/{id}")
    public Result<Void> updateCrop(@PathVariable Long id, @Valid @RequestBody CropUpdateRequest request) {
        request.setId(id);
        cropService.updateCrop(request);
        return Result.success("修改作物成功", null);
    }

    @Operation(summary = "删除作物")
    @DeleteMapping("/{id}")
    public Result<Void> deleteCrop(@PathVariable Long id) {
        cropService.deleteCrop(id);
        return Result.success("删除作物成功", null);
    }

    @Operation(summary = "查看作物详情")
    @GetMapping("/{id}")
    public Result<CropDetailVO> getCropDetail(@PathVariable Long id) {
        return Result.success(cropService.getCropDetail(id));
    }

    @Operation(summary = "分页查询作物列表")
    @GetMapping("/page")
    public Result<IPage<CropListVO>> pageCrops(@Valid CropPageQueryRequest request) {
        return Result.success(cropService.pageCrops(request));
    }
}
