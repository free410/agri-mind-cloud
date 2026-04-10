package com.smartagri.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "smart-agri.ai")
public class AiProperties {

    /**
     * AI 提供方，可选值：mock、openai-compatible。
     */
    private String provider = "openai-compatible";

    /**
     * 是否启用 mock 模式（仅当 provider 未显式指定时参与路由）。
     */
    private boolean mockEnabled = false;

    /**
     * 模型名称（百炼示例：qwen-plus、qwen-turbo）。
     */
    private String modelName = "qwen3.5-flash";

    /**
     * 真实模型调用失败时，是否自动回退到 mock 结果。
     */
    private boolean fallbackToMock = false;

    /**
     * OpenAI 兼容接口基地址；百炼为 https://dashscope.aliyuncs.com/compatible-mode/v1
     */
    private String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";

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
