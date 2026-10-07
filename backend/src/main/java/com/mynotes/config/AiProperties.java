package com.mynotes.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * base-url 用任意 OpenAI 兼容网关（DashScope compatible-mode、DeepSeek、本地 ollama 等）。
 * api-key 留空即整套问答降级为"关键词检索 + 原文引用"，不配 key 也能用。
 */
@ConfigurationProperties(prefix = "mynotes.ai")
public record AiProperties(String apiKey,
                           String baseUrl,
                           String chatModel,
                           String embeddingModel,
                           int embeddingDim) {

    public boolean configured() {
        return notBlank(apiKey) && notBlank(baseUrl) && notBlank(chatModel) && notBlank(embeddingModel);
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
