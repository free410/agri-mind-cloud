package com.smartagri.modules.crop.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "作物分页查询参数")
public class CropPageQueryRequest {

    @Min(value = 1, message = "页码不能小于1")
    @Schema(description = "当前页码", example = "1")
    private long current = 1;

    @Min(value = 1, message = "每页条数不能小于1")
    @Max(value = 100, message = "每页条数不能超过100")
    @Schema(description = "每页条数", example = "10")
    private long size = 10;

    @Size(max = 100, message = "作物名称长度不能超过100个字符")
    @Schema(description = "作物名称", example = "番茄")
    private String cropName;

    @Size(max = 50, message = "作物类型长度不能超过50个字符")
    @Schema(description = "作物类型", example = "果蔬类")
    private String cropType;

    @Schema(description = "所属农田ID", example = "1")
    private Long farmlandId;
}
