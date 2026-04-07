package com.smartagri.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "smart-agri.ai")
public class AiProperties {

    /**
     * AI 提供方，可选值：mock、openai-compatible。
     */
    private String provider = "mock";

    /**
     * 是否启用 mock 模式。
     * 该字段用于兼容旧配置，实际优先以 provider 为准。
     */
    private boolean mockEnabled = true;

    /**
     * 模型名称。
     */
    private String modelName = "mock-smart-agri-v1";

    /**
     * 真实模型调用失败时，是否自动回退到 mock 结果。
     */
    private boolean fallbackToMock = true;

    /**
     * OpenAI 兼容接口基地址，例如 https://api.openai.com/v1
     */
    private String baseUrl = "https://api.openai.com/v1";

    /**
     * API Key。
     */
    private String apiKey;

    /**
     * 对话接口路径。
     */
    private String chatPath = "/chat/completions";

    /**
     * 采样温度。
     */
    private Double temperature = 0.4D;

    /**
     * 最大输出 token 数。
     */
    private Integer maxTokens = 1200;

    /**
     * 连接超时时间，单位秒。
     */
    private Integer connectTimeoutSeconds = 10;

    /**
     * 读取超时时间，单位秒。
     */
    private Integer readTimeoutSeconds = 60;
}
