package com.smartagri.modules.crop.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@Schema(description = "修改作物请求参数")
public class CropUpdateRequest {

    @NotNull(message = "作物ID不能为空")
    @Schema(description = "作物ID", example = "1")
    private Long id;

    @NotBlank(message = "作物名称不能为空")
    @Size(max = 100, message = "作物名称长度不能超过100个字符")
    @Schema(description = "作物名称", example = "樱桃番茄")
    private String cropName;

    @NotBlank(message = "作物类型不能为空")
    @Size(max = 50, message = "作物类型长度不能超过50个字符")
    @Schema(description = "作物类型", example = "果蔬类")
    private String cropType;

    @NotNull(message = "所属农田不能为空")
    @Schema(description = "所属农田ID", example = "1")
    private Long farmlandId;

    @NotNull(message = "播种日期不能为空")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "播种日期", example = "2026-03-01")
    private LocalDate sowingDate;

    @NotNull(message = "预计成熟日期不能为空")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "预计成熟日期", example = "2026-06-10")
    private LocalDate maturityDate;

    @NotBlank(message = "生长阶段不能为空")
    @Schema(description = "生长阶段编码，可选值：SEEDING、SEEDLING、GROWING、FLOWERING、FRUITING、MATURING、HARVESTED", example = "GROWING")
    private String growthStage;

    @NotBlank(message = "健康状态不能为空")
    @Schema(description = "健康状态编码，可选值：HEALTHY、ATTENTION、DISEASE_RISK、ABNORMAL", example = "HEALTHY")
    private String healthStatus;

    @Size(max = 255, message = "备注长度不能超过255个字符")
    @Schema(description = "备注", example = "当前生长状态良好")
    private String remark;
}
