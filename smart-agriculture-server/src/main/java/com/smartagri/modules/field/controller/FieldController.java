package com.smartagri.modules.field.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.smartagri.common.result.Result;
import com.smartagri.modules.field.dto.FieldCreateRequest;
import com.smartagri.modules.field.dto.FieldPageQueryRequest;
import com.smartagri.modules.field.dto.FieldUpdateRequest;
import com.smartagri.modules.field.service.FieldService;
import com.smartagri.modules.field.vo.FieldDetailVO;
import com.smartagri.modules.field.vo.FieldListVO;
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
@Tag(name = "农田管理")
@RestController
@RequestMapping("/fields")
public class FieldController {

    private final FieldService fieldService;

    public FieldController(FieldService fieldService) {
        this.fieldService = fieldService;
    }

    @Operation(summary = "新增农田")
    @PostMapping
    public Result<Long> createField(@Valid @RequestBody FieldCreateRequest request) {
        return Result.success("新增农田成功", fieldService.createField(request));
    }

    @Operation(summary = "修改农田")
    @PutMapping("/{id}")
    public Result<Void> updateField(@PathVariable Long id, @Valid @RequestBody FieldUpdateRequest request) {
        request.setId(id);
        fieldService.updateField(request);
        return Result.success("修改农田成功", null);
    }

    @Operation(summary = "删除农田")
    @DeleteMapping("/{id}")
    public Result<Void> deleteField(@PathVariable Long id) {
        fieldService.deleteField(id);
        return Result.success("删除农田成功", null);
    }

    @Operation(summary = "查看农田详情")
    @GetMapping("/{id}")
    public Result<FieldDetailVO> getFieldDetail(@PathVariable Long id) {
        return Result.success(fieldService.getFieldDetail(id));
    }

    @Operation(summary = "分页查询农田列表")
    @GetMapping("/page")
    public Result<IPage<FieldListVO>> pageFields(@Valid FieldPageQueryRequest request) {
        return Result.success(fieldService.pageFields(request));
    }
}
