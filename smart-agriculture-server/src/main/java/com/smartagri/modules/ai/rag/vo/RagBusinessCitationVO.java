package com.smartagri.modules.ai.rag.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RagBusinessCitationVO {

    private List<String> summaryBlocks;

    private List<String> riskSignals;

    private Long relatedWarningId;

    private Long relatedDeviceId;

    private Long relatedFarmlandId;
}
