package com.smartagri.modules.ai.rag.vo;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
public class RagBusinessCitationVO {

    private List<String> summaryBlocks;

    private List<String> riskSignals;

    private Long relatedWarningId;

    private Long relatedDeviceId;

    private Long relatedFarmlandId;
}
