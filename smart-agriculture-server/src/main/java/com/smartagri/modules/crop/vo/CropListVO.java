package com.smartagri.modules.crop.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "作物列表返回对象")
public class CropListVO {

    @Schema(description = "作物ID")
    private Long id;

    @Schema(description = "作物名称")
    private String cropName;

    @Schema(description = "作物类型")
    private String cropType;

    @Schema(description = "所属农田ID")
    private Long farmlandId;

    @Schema(description = "所属农田名称")
    private String farmlandName;

    @Schema(description = "播种日期")
    private LocalDate sowingDate;

    @Schema(description = "预计成熟日期")
    private LocalDate maturityDate;

    @Schema(description = "生长阶段编码")
    private String growthStage;

    @Schema(description = "生长阶段名称")
    private String growthStageName;

    @Schema(description = "健康状态编码")
    private String healthStatus;

    @Schema(description = "健康状态名称")
    private String healthStatusName;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
