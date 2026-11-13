package com.smartagri.modules.ai.rag.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;
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

    /**
     * 图谱引用列表，包含实体信息和 subgraph 数据用于前端迷你图谱可视化。
     * 每个元素包含: entity_id, entity_name, entity_kind, subgraph
     */
    @JsonProperty("cited_knowledge_graph")
    private List<Map<String, Object>> citedKnowledgeGraph;
}
