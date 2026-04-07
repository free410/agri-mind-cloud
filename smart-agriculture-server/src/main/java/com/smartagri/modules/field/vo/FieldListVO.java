package com.smartagri.modules.field.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "农田列表返回对象")
public class FieldListVO {

    @Schema(description = "农田ID")
    private Long id;

    @Schema(description = "农田名称")
    private String fieldName;

    @Schema(description = "农田面积")
    private BigDecimal area;

    @Schema(description = "地理位置")
    private String location;

    @Schema(description = "土壤类型")
    private String soilType;

    @Schema(description = "农田状态：0闲置，1种植中，2维护中")
    private Integer status;

    @Schema(description = "农田状态名称")
    private String statusName;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
