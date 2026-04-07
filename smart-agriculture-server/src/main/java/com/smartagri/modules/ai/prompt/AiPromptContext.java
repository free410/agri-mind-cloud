package com.smartagri.modules.ai.prompt;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class AiPromptContext {

    private String scenario;

    private String systemPrompt;

    private String userPrompt;

    private String relatedModule;

    private Long relatedRecordId;

    private String title;

    private String displayQuestion;

    private Map<String, Object> metadata;

    private String contextSnapshot;
}
