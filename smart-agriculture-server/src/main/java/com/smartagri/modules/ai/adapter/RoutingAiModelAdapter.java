package com.smartagri.modules.ai.adapter;

import com.smartagri.common.properties.AiProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Slf4j
@Primary
@Component
public class RoutingAiModelAdapter implements AiModelAdapter {

    private final AiProperties aiProperties;
    private final MockAiModelAdapter mockAiModelAdapter;
    private final OpenAiCompatibleModelAdapter openAiCompatibleModelAdapter;

    public RoutingAiModelAdapter(AiProperties aiProperties,
                                 MockAiModelAdapter mockAiModelAdapter,
                                 OpenAiCompatibleModelAdapter openAiCompatibleModelAdapter) {
        this.aiProperties = aiProperties;
        this.mockAiModelAdapter = mockAiModelAdapter;
        this.openAiCompatibleModelAdapter = openAiCompatibleModelAdapter;
    }

    @Override
    public AiModelResponse chat(AiModelRequest request) {
        if (!useRealModel()) {
            return mockAiModelAdapter.chat(request);
        }

        try {
            return openAiCompatibleModelAdapter.chat(request);
        } catch (Exception exception) {
            if (!aiProperties.isFallbackToMock()) {
                throw exception;
            }
            log.warn("真实大模型调用失败，已自动回退到 mock 结果：{}", exception.getMessage());
            AiModelResponse fallbackResponse = mockAiModelAdapter.chat(request);
            return AiModelResponse.builder()
                    .modelName("mock-fallback(" + aiProperties.getModelName() + ")")
                    .content(fallbackResponse.getContent())
                    .build();
        }
    }

    private boolean useRealModel() {
        String provider = StringUtils.trimToEmpty(aiProperties.getProvider());
        if ("mock".equalsIgnoreCase(provider)) {
            return false;
        }
        if (StringUtils.isBlank(provider)) {
            return !aiProperties.isMockEnabled();
        }
        return "openai-compatible".equalsIgnoreCase(provider) || "openai".equalsIgnoreCase(provider);
    }
}
