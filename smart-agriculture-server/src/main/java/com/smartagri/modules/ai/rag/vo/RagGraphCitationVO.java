package com.smartagri.modules.ai.rag.vo;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
public class RagGraphCitationVO {

    private String id;

    /** 实体展示名（与知识引用中的 title 字段对齐，便于前端复用样式） */
    private String title;

    private String kind;

    /** 迷你子图数据，用于前端可视化 */
    private Map<String, Object> subgraph;
}
