package com.smartagri.modules.ai.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartagri.common.properties.AiProperties;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class OpenAiCompatibleModelAdapter implements AiModelAdapter {

    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OpenAiCompatibleModelAdapter(AiProperties aiProperties, ObjectMapper objectMapper) {
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(aiProperties.getConnectTimeoutSeconds()))
                .build();
    }

    @Override
    public AiModelResponse chat(AiModelRequest request) {
        validateConfiguration();

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(buildRequestUrl()))
                .timeout(Duration.ofSeconds(aiProperties.getReadTimeoutSeconds()))
                .header("Authorization", "Bearer " + aiProperties.getApiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(buildRequestBody(request)))
                .build();

        try {
            HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 300) {
                throw new IllegalStateException("模型接口调用失败：" + extractErrorMessage(httpResponse.body(), httpResponse.statusCode()));
            }

            JsonNode responseNode = objectMapper.readTree(httpResponse.body());
            JsonNode choiceNode = responseNode.path("choices").path(0);
            String content = choiceNode.path("message").path("content").asText();
            if (StringUtils.isBlank(content)) {
                throw new IllegalStateException("模型接口返回内容为空");
            }

            String modelName = responseNode.path("model").asText(aiProperties.getModelName());
            return AiModelResponse.builder()
                    .modelName(modelName)
                    .content(content.trim())
                    .build();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("调用真实大模型接口失败：" + exception.getMessage(), exception);
        } catch (IOException exception) {
            throw new IllegalStateException("调用真实大模型接口失败：" + exception.getMessage(), exception);
        }
    }

    private void validateConfiguration() {
        if (StringUtils.isBlank(aiProperties.getApiKey())) {
            throw new IllegalStateException("未配置 AI API Key，请先设置 smart-agri.ai.api-key 或环境变量 SMART_AGRI_AI_API_KEY");
        }
        if (StringUtils.isBlank(aiProperties.getBaseUrl())) {
            throw new IllegalStateException("未配置 AI 接口地址");
        }
        if (StringUtils.isBlank(aiProperties.getModelName())) {
            throw new IllegalStateException("未配置 AI 模型名称");
        }
    }

    private String buildRequestBody(AiModelRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", aiProperties.getModelName());
        payload.put("messages", List.of(
                Map.of("role", "system", "content", request.getSystemPrompt()),
                Map.of("role", "user", "content", request.getUserPrompt())
        ));
        payload.put("temperature", aiProperties.getTemperature());
        payload.put("max_tokens", aiProperties.getMaxTokens());

        try {
            return objectMapper.writeValueAsString(payload);
        } catch (IOException exception) {
            throw new IllegalStateException("构建 AI 请求参数失败", exception);
        }
    }

    private String buildRequestUrl() {
        String baseUrl = StringUtils.removeEnd(aiProperties.getBaseUrl(), "/");
        String chatPath = aiProperties.getChatPath();
        if (!StringUtils.startsWith(chatPath, "/")) {
            chatPath = "/" + chatPath;
        }
        return baseUrl + chatPath;
    }

    private String extractErrorMessage(String body, int statusCode) {
        if (StringUtils.isBlank(body)) {
            return "HTTP " + statusCode;
        }
        try {
            JsonNode responseNode = objectMapper.readTree(body);
            String message = responseNode.path("error").path("message").asText();
            return StringUtils.defaultIfBlank(message, "HTTP " + statusCode);
        } catch (IOException exception) {
            return "HTTP " + statusCode + "，响应内容：" + body;
        }
    }
}
