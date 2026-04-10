package com.smartagri.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "smart-agri.rag")
public class RagProperties {

    /**
     * 是否启用 FastAPI RAG 调用。
     */
    private boolean enabled = true;

    /**
     * FastAPI 服务基础地址。
     */
    private String baseUrl = "http://localhost:9001";

    /**
     * FastAPI /rag/answer 路径。
     */
    private String answerPath = "/rag/answer";

    /**
     * 连接超时（秒）。
     */
    private Integer connectTimeoutSeconds = 5;

    /**
     * 读取超时（秒）。
     */
    private Integer readTimeoutSeconds = 30;

    /**
     * FastAPI 不可用时是否启用本地兜底（默认 true，便于本地演示；生产可关闭）。
     */
    private boolean fallbackEnabled = true;

    /**
     * 默认知识检索 topK。
     */
    private Integer defaultTopK = 10;

    /**
     * 是否透传 debug_prompt。
     */
    private boolean debugPrompt = false;
}
