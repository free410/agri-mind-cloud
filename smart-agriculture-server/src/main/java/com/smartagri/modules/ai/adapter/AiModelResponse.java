package com.smartagri.modules.ai.adapter;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiModelResponse {

    private String modelName;

    private String content;
}
