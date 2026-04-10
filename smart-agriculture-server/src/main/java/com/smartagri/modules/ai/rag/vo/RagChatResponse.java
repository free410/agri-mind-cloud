package com.smartagri.modules.ai.rag.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RagChatResponse {

    private String sessionId;

    private String mode;

    private String questionType;

    private String answerSummary;

    private String conclusion;

    private List<String> possibleCauses;

    private List<String> recommendations;

    private String priority;

    private Boolean shouldManualReview;

    private RagBusinessCitationVO citedBusinessContext;

    private List<RagKnowledgeCitationVO> citedKnowledgeSources;

    private List<RagGraphCitationVO> citedKnowledgeGraph;

    private Boolean fallback;

    private String fallbackReason;
}
