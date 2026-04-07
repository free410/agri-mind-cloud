package com.smartagri.modules.field.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "农田分页查询参数")
public class FieldPageQueryRequest {

    @Min(value = 1, message = "页码不能小于1")
    @Schema(description = "当前页码", example = "1")
    private long current = 1;

    @Min(value = 1, message = "每页条数不能小于1")
    @Max(value = 100, message = "每页条数不能超过100")
    @Schema(description = "每页条数", example = "10")
    private long size = 10;

    @Size(max = 100, message = "农田名称长度不能超过100个字符")
    @Schema(description = "农田名称", example = "示范")
    private String fieldName;

    @Size(max = 50, message = "土壤类型长度不能超过50个字符")
    @Schema(description = "土壤类型", example = "壤土")
    private String soilType;

    @Min(value = 0, message = "农田状态值不合法")
    @Max(value = 2, message = "农田状态值不合法")
    @Schema(description = "农田状态：0闲置，1种植中，2维护中", example = "1")
    private Integer status;
}
