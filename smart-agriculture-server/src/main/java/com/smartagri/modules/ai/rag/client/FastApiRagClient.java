package com.smartagri.modules.ai.rag.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.smartagri.common.exception.BusinessException;
import com.smartagri.common.properties.RagProperties;
import com.smartagri.common.result.ResultCode;
import com.smartagri.modules.ai.rag.client.dto.FastApiResponse;
import com.smartagri.modules.ai.rag.client.dto.RagAnswerClientRequest;
import com.smartagri.modules.ai.rag.client.dto.RagAnswerClientResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class FastApiRagClient implements RagClient {

    private final RagProperties ragProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    /** 专门用于反序列化 FastAPI 响应（snake_case） */
    private final ObjectMapper fastApiResponseMapper;

    public FastApiRagClient(RagProperties ragProperties, ObjectMapper objectMapper) {
        this.ragProperties = ragProperties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(ragProperties.getConnectTimeoutSeconds()))
                .build();
        // FastAPI 使用 snake_case，单独配置一个 mapper 接收
        this.fastApiResponseMapper = objectMapper.copy();
        this.fastApiResponseMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
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

            FastApiResponse<RagAnswerClientResponse> wrapper = fastApiResponseMapper.readValue(
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

    @Override
    public Map<String, Object> uploadKnowledge(String filename, byte[] content, String category) {
        try {
            String boundary = "----SmartAgriBoundary" + UUID.randomUUID().toString().replace("-", "");
            byte[] body = buildMultipartBody(boundary, filename, content, category);

            String baseUrl = StringUtils.removeEnd(ragProperties.getBaseUrl(), "/");
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/knowledge/upload"))
                    .version(HttpClient.Version.HTTP_1_1)
                    .timeout(Duration.ofSeconds(Math.max(ragProperties.getReadTimeoutSeconds(), 300)))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String raw = StringUtils.defaultString(response.body());
                String preview = raw.length() > 600 ? raw.substring(0, 600) + "..." : raw;
                throw new BusinessException(ResultCode.SYSTEM_ERROR,
                        "知识库上传失败，HTTP 状态码：" + response.statusCode()
                                + (StringUtils.isBlank(preview) ? "" : "，响应：" + preview));
            }

            FastApiResponse<Map<String, Object>> wrapper = fastApiResponseMapper.readValue(
                    response.body(),
                    new TypeReference<FastApiResponse<Map<String, Object>>>() {
                    }
            );
            if (wrapper.getCode() != null && wrapper.getCode() != 0) {
                throw new BusinessException(ResultCode.SYSTEM_ERROR,
                        "知识库上传失败：" + StringUtils.defaultIfBlank(wrapper.getMessage(), "unknown"));
            }
            return wrapper.getData() == null ? Map.of() : wrapper.getData();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "知识库上传被中断");
        } catch (IOException exception) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "知识库上传失败：" + exception.getMessage());
        }
    }

    @Override
    public Map<String, Object> listKnowledgeDocuments() {
        String baseUrl = StringUtils.removeEnd(ragProperties.getBaseUrl(), "/");
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/knowledge/documents"))
                .version(HttpClient.Version.HTTP_1_1)
                .timeout(Duration.ofSeconds(Math.max(ragProperties.getReadTimeoutSeconds(), 60)))
                .GET()
                .build();
        return executeForMap(httpRequest);
    }

    @Override
    public Map<String, Object> deleteKnowledgeDocument(String path) {
        String baseUrl = StringUtils.removeEnd(ragProperties.getBaseUrl(), "/");
        String encodedPath = URLEncoder.encode(path, StandardCharsets.UTF_8).replace("+", "%20");
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/knowledge/documents?path=" + encodedPath))
                .version(HttpClient.Version.HTTP_1_1)
                .timeout(Duration.ofSeconds(Math.max(ragProperties.getReadTimeoutSeconds(), 60)))
                .DELETE()
                .build();
        return executeForMap(httpRequest);
    }

    private Map<String, Object> executeForMap(HttpRequest httpRequest) {
        try {
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException(ResultCode.SYSTEM_ERROR,
                        "RAG 服务调用失败，HTTP 状态码：" + response.statusCode());
            }
            FastApiResponse<Map<String, Object>> wrapper = fastApiResponseMapper.readValue(
                    response.body(),
                    new TypeReference<FastApiResponse<Map<String, Object>>>() {
                    }
            );
            if (wrapper.getCode() != null && wrapper.getCode() != 0) {
                throw new BusinessException(ResultCode.SYSTEM_ERROR,
                        "RAG 服务返回错误：" + StringUtils.defaultIfBlank(wrapper.getMessage(), "unknown"));
            }
            return wrapper.getData() == null ? Map.of() : wrapper.getData();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "RAG 服务调用被中断");
        } catch (IOException exception) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "RAG 服务调用失败：" + exception.getMessage());
        }
    }

    private byte[] buildMultipartBody(String boundary, String filename, byte[] content, String category)
            throws IOException {
        String safeName = StringUtils.defaultIfBlank(filename, "upload.bin");
        String asciiName = safeName.replaceAll("[^\\x20-\\x7E]", "_").replace("\"", "_");
        String encodedName = URLEncoder.encode(safeName, StandardCharsets.UTF_8).replace("+", "%20");

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        String categoryPart = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"category\"\r\n\r\n"
                + StringUtils.defaultIfBlank(category, "crop") + "\r\n";
        buffer.write(categoryPart.getBytes(StandardCharsets.UTF_8));

        String fileHeader = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"" + asciiName
                + "\"; filename*=UTF-8''" + encodedName + "\r\n"
                + "Content-Type: application/octet-stream\r\n\r\n";
        buffer.write(fileHeader.getBytes(StandardCharsets.UTF_8));
        buffer.write(content);
        buffer.write("\r\n".getBytes(StandardCharsets.UTF_8));
        buffer.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return buffer.toByteArray();
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
        if (request.getCitedKnowledgeGraph() != null && !request.getCitedKnowledgeGraph().isEmpty()) {
            body.put("cited_knowledge_graph", request.getCitedKnowledgeGraph());
        }
        return objectMapper.writeValueAsString(body);
    }
}
