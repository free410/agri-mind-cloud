package com.smartagri.modules.ai.rag.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RagKnowledgeCitationVO {

    private String id;

    private String title;

    private String source;

    private Double score;

    private String topic;
}
