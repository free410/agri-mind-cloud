package com.smartagri.modules.dashboard.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "名称数值对象")
public class DashboardNameValueVO {

    @Schema(description = "名称")
    private String name;

    @Schema(description = "数值")
    private Long value;
}
