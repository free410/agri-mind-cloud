package com.smartagri.modules.ai.rag.vo;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
public class RagKnowledgeCitationVO {

    private String id;

    private String title;

    private String source;

    private Double score;

    private String topic;

    /**
     * 知识块完整内容，用于前端检索详情展示
     */
    private String content;

    /**
     * 重排分数（Reranker 提供的相关性分数）
     */
    private Double rerankScore;

    /**
     * 知识领域标记（crop_disease / market_price / agri_policy / general）
     */
    private String domain;

    /**
     * 元数据对象，包含来源类型、作物类型等
     */
    private Object metadata;
}
