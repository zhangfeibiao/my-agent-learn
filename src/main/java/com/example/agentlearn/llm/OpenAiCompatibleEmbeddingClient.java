package com.example.agentlearn.llm;

import com.example.agentlearn.util.Json;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class OpenAiCompatibleEmbeddingClient implements EmbeddingClient {
    private final HttpClient httpClient;
    private final String apiBaseUrl;
    private final String apiKey;
    private final String model;

    public OpenAiCompatibleEmbeddingClient(String apiBaseUrl, String apiKey, String model) {
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
        this.apiBaseUrl = stripTrailingSlash(apiBaseUrl);
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Double> embed(String text) throws Exception {
        requireApiKey();
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", model);
        request.put("input", text);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(apiBaseUrl + "/embeddings"))
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(Json.stringify(request)))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Embedding request failed: HTTP " + response.statusCode() + " " + response.body());
        }

        Map<String, Object> body = (Map<String, Object>) Json.parse(response.body());
        List<Object> data = (List<Object>) body.get("data");
        if (data == null || data.isEmpty()) {
            throw new IllegalStateException("Embedding response did not contain data");
        }
        Map<String, Object> first = (Map<String, Object>) data.get(0);
        List<Object> rawEmbedding = (List<Object>) first.get("embedding");
        List<Double> embedding = new ArrayList<>();
        for (Object value : rawEmbedding) {
            embedding.add(((Number) value).doubleValue());
        }
        return List.copyOf(embedding);
    }

    private void requireApiKey() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("AGENT_API_KEY is required for live embedding requests");
        }
    }

    private String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
