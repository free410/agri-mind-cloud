package com.smartagri.modules.ai.rag.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartagri.common.exception.BusinessException;
import com.smartagri.common.properties.RagProperties;
import com.smartagri.common.result.ResultCode;
import com.smartagri.modules.ai.rag.client.dto.FastApiResponse;
import com.smartagri.modules.ai.rag.client.dto.RagAnswerClientRequest;
import com.smartagri.modules.ai.rag.client.dto.RagAnswerClientResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class FastApiRagClient implements RagClient {

    private final RagProperties ragProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public FastApiRagClient(RagProperties ragProperties, ObjectMapper objectMapper) {
        this.ragProperties = ragProperties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(ragProperties.getConnectTimeoutSeconds()))
                .build();
    }

    @Override
    public RagAnswerClientResponse answer(RagAnswerClientRequest request) {
        try {
            // 显式组装 JSON：避免 Lombok @Builder + ObjectMapper 直接序列化 Bean 时出现空对象 {}
            String jsonBody = buildAnswerJsonBody(request);
            // 强制 HTTP/1.1：Java HttpClient 默认可能走 HTTP/2，Uvicorn 对 h2 支持不完整时会出现 POST body 在服务端为空（422 请求体为空）
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(buildAnswerUrl()))
                    .version(HttpClient.Version.HTTP_1_1)
                    .timeout(Duration.ofSeconds(ragProperties.getReadTimeoutSeconds()))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String body = StringUtils.defaultString(response.body());
                String preview = body.length() > 600 ? body.substring(0, 600) + "..." : body;
                throw new BusinessException(ResultCode.SYSTEM_ERROR,
                        "RAG 服务调用失败，HTTP 状态码：" + response.statusCode()
                                + (StringUtils.isBlank(preview) ? "" : "，响应：" + preview));
            }

            FastApiResponse<RagAnswerClientResponse> wrapper = objectMapper.readValue(
                    response.body(),
                    new TypeReference<FastApiResponse<RagAnswerClientResponse>>() {
                    }
            );
            if (wrapper.getCode() != null && wrapper.getCode() != 0) {
                throw new BusinessException(ResultCode.SYSTEM_ERROR,
                        "RAG 服务返回异常：" + StringUtils.defaultIfBlank(wrapper.getMessage(), "unknown"));
            }
            if (wrapper.getData() == null) {
                throw new BusinessException(ResultCode.SYSTEM_ERROR, "RAG 服务未返回有效数据");
            }
            return wrapper.getData();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "RAG 服务调用被中断");
        } catch (IOException exception) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "RAG 服务调用失败：" + exception.getMessage());
        }
    }

    private String buildAnswerUrl() {
        String baseUrl = StringUtils.removeEnd(ragProperties.getBaseUrl(), "/");
        String answerPath = ragProperties.getAnswerPath();
        if (!StringUtils.startsWith(answerPath, "/")) {
            answerPath = "/" + answerPath;
        }
        return baseUrl + answerPath;
    }

    /**
     * 与 FastAPI RagAnswerRequest 字段名（snake_case）一致，保证请求体非空且含 query。
     */
    private String buildAnswerJsonBody(RagAnswerClientRequest request) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("query", request.getQuery());
        body.put("mode", StringUtils.defaultIfBlank(request.getMode(), "auto"));
        if (request.getBusinessContext() != null) {
            body.put("business_context", request.getBusinessContext());
        }
        if (request.getRelatedWarningId() != null) {
            body.put("related_warning_id", request.getRelatedWarningId());
        }
        if (request.getRelatedDeviceId() != null) {
            body.put("related_device_id", request.getRelatedDeviceId());
        }
        if (request.getRelatedFarmlandId() != null) {
            body.put("related_farmland_id", request.getRelatedFarmlandId());
        }
        if (request.getTopK() != null) {
            body.put("top_k", request.getTopK());
        }
        if (StringUtils.isNotBlank(request.getKnowledgeGraphContext())) {
            body.put("knowledge_graph_context", request.getKnowledgeGraphContext());
        }
        return objectMapper.writeValueAsString(body);
    }
}
