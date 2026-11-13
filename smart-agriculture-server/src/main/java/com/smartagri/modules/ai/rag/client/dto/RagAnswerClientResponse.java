package com.smartagri.modules.ai.rag.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RagAnswerClientResponse {

    @JsonProperty("session_id")
    private String sessionId;

    private String mode;

    @JsonProperty("question_type")
    private String questionType;

    @JsonProperty("answer_summary")
    private String answerSummary;

    private String conclusion;

    @JsonProperty("possible_causes")
    private List<String> possibleCauses = new ArrayList<>();

    private List<String> recommendations = new ArrayList<>();

    private String priority;

    @JsonProperty("should_manual_review")
    private Boolean shouldManualReview;

    @JsonProperty("cited_business_context")
    private CitedBusinessContext citedBusinessContext;

    @JsonProperty("cited_knowledge_sources")
    private List<CitedKnowledgeSource> citedKnowledgeSources = new ArrayList<>();

    /**
     * 知识图谱引用列表（含 subgraph 数据，用于前端迷你图谱可视化）。
     * 来自 FastAPI RagAnswerResponse.cited_knowledge_graph。
     */
    @JsonProperty("cited_knowledge_graph")
    private List<Map<String, Object>> citedKnowledgeGraph = new ArrayList<>();

    /**
     * LLM 兜底原因，来自 FastAPI 响应。
     */
    @JsonProperty("fallback_reason")
    private String fallbackReason;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CitedBusinessContext {

        @JsonProperty("summary_blocks")
        private List<String> summaryBlocks = new ArrayList<>();

        @JsonProperty("risk_signals")
        private List<String> riskSignals = new ArrayList<>();

        @JsonProperty("related_ids")
        private RelatedIds relatedIds;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RelatedIds {

        @JsonProperty("related_warning_id")
        private Long relatedWarningId;

        @JsonProperty("related_device_id")
        private Long relatedDeviceId;

        @JsonProperty("related_farmland_id")
        private Long relatedFarmlandId;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CitedKnowledgeSource {

        private String id;

        private String title;

        private String source;

        private Double score;

        private String topic;

        /**
         * 知识块完整内容（FastAPI 新增）
         */
        private String content;

        /**
         * 重排分数（FastAPI 新增）
         */
        @JsonProperty("rerank_score")
        private Double rerankScore;

        /**
         * 知识领域标记（FastAPI 新增）
         */
        private String domain;

        /**
         * 元数据对象（FastAPI 新增）
         */
        private Map<String, Object> metadata;
    }
}
