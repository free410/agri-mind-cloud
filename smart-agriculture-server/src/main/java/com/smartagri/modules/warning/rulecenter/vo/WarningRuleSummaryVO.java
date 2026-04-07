package com.smartagri.modules.warning.rulecenter.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "规则中心统计摘要")
public class WarningRuleSummaryVO {

    @Schema(description = "规则总数")
    private Long totalCount;

    @Schema(description = "启用规则数")
    private Long enabledCount;

    @Schema(description = "全局规则数")
    private Long globalCount;

    @Schema(description = "定向规则数")
    private Long scopedCount;
}

