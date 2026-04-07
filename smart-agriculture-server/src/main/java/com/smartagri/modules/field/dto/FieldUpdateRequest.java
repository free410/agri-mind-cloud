package com.smartagri.modules.field.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "修改农田请求参数")
public class FieldUpdateRequest {

    @NotNull(message = "农田ID不能为空")
    @Schema(description = "农田ID", example = "1")
    private Long id;

    @NotBlank(message = "农田名称不能为空")
    @Size(max = 100, message = "农田名称长度不能超过100个字符")
    @Schema(description = "农田名称", example = "一号示范田")
    private String fieldName;

    @NotNull(message = "农田面积不能为空")
    @DecimalMin(value = "0.01", message = "农田面积必须大于0")
    @Schema(description = "农田面积", example = "20.50")
    private BigDecimal area;

    @NotBlank(message = "地理位置不能为空")
    @Size(max = 255, message = "地理位置长度不能超过255个字符")
    @Schema(description = "地理位置", example = "山东省寿光市智慧农业园区A区")
    private String location;

    @NotBlank(message = "土壤类型不能为空")
    @Size(max = 50, message = "土壤类型长度不能超过50个字符")
    @Schema(description = "土壤类型", example = "壤土")
    private String soilType;

    @NotNull(message = "农田状态不能为空")
    @Min(value = 0, message = "农田状态值不合法")
    @Max(value = 2, message = "农田状态值不合法")
    @Schema(description = "农田状态：0闲置，1种植中，2维护中", example = "1")
    private Integer status;

    @Size(max = 255, message = "备注长度不能超过255个字符")
    @Schema(description = "备注", example = "已接入土壤湿度传感器")
    private String remark;
}
