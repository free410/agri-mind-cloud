package com.smartagri.modules.ai.adapter;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class AiModelRequest {

    private String scenario;

    private String systemPrompt;

    private String userPrompt;

    private Map<String, Object> metadata;
}
