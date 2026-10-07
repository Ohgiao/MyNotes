package com.mynotes.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.mynotes.config.AiProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 只依赖 OpenAI 兼容的两个端点：/embeddings 与 /chat/completions，
 * 换供应商时改 base-url 和模型名即可，不需要引入 SDK。
 */
@Component
public class OpenAiCompatibleClient {

    private final AiProperties properties;
    private final RestClient http;

    public OpenAiCompatibleClient(AiProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(120));
        this.http = RestClient.builder().requestFactory(factory).build();
    }

    public List<float[]> embed(List<String> texts) {
        JsonNode root = post("/embeddings", Map.of(
                "model", properties.embeddingModel(),
                "input", texts));
        JsonNode data = root.path("data");
        List<float[]> vectors = new ArrayList<>();
        data.forEach(item -> vectors.add(toFloatArray(item.path("embedding"))));
        if (vectors.size() != texts.size()) {
            throw new AiException("embedding 返回数量与请求不一致：" + vectors.size() + " / " + texts.size());
        }
        return vectors;
    }

    public String chat(String systemPrompt, String userPrompt) {
        JsonNode root = post("/chat/completions", Map.of(
                "model", properties.chatModel(),
                "temperature", 0.2,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt))));
        String content = root.path("choices").path(0).path("message").path("content").asText("");
        if (content.isBlank()) {
            throw new AiException("模型没有返回内容，请检查 chat-model 配置");
        }
        return content.strip();
    }

    private JsonNode post(String path, Map<String, Object> body) {
        String url = properties.baseUrl().replaceAll("/+$", "") + path;
        try {
            return http.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RuntimeException ex) {
            throw new AiException("调用模型失败：" + rootMessage(ex));
        }
    }

    private float[] toFloatArray(JsonNode node) {
        float[] vector = new float[node.size()];
        for (int i = 0; i < node.size(); i++) {
            vector[i] = (float) node.get(i).asDouble();
        }
        return vector;
    }

    private String rootMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.isBlank() ? current.getClass().getSimpleName() : message;
    }

    public static class AiException extends RuntimeException {

        public AiException(String message) {
            super(message);
        }
    }
}
