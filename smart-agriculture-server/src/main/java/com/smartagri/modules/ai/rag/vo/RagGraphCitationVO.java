package com.smartagri.modules.ai.rag.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RagGraphCitationVO {

    private String id;

    /** 实体展示名（与知识引用中的 title 字段对齐，便于前端复用样式） */
    private String title;

    private String kind;
}
