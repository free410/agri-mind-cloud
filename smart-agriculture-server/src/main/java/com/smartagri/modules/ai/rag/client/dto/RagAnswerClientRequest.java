package com.smartagri.modules.ai.rag.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class RagAnswerClientRequest {

    private String query;

    private String mode;

    @JsonProperty("business_context")
    private Map<String, Object> businessContext;

    @JsonProperty("related_warning_id")
    private Long relatedWarningId;

    @JsonProperty("related_device_id")
    private Long relatedDeviceId;

    @JsonProperty("related_farmland_id")
    private Long relatedFarmlandId;

    @JsonProperty("top_k")
    private Integer topK;

    @JsonProperty("knowledge_graph_context")
    private String knowledgeGraphContext;
}
