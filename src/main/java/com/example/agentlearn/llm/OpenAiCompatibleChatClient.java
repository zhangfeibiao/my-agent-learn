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

public final class OpenAiCompatibleChatClient implements ChatClient {
    private final HttpClient httpClient;
    private final String apiBaseUrl;
    private final String apiKey;
    private final String model;

    public OpenAiCompatibleChatClient(String apiBaseUrl, String apiKey, String model) {
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
        this.apiBaseUrl = stripTrailingSlash(apiBaseUrl);
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    @SuppressWarnings("unchecked")
    public String complete(List<ChatMessage> messages) throws Exception {
        requireApiKey();
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", model);
        request.put("messages", toJsonMessages(messages));
        request.put("temperature", 0.2);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(apiBaseUrl + "/chat/completions"))
                .timeout(Duration.ofSeconds(90))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(Json.stringify(request)))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Chat request failed: HTTP " + response.statusCode() + " " + response.body());
        }

        Map<String, Object> body = (Map<String, Object>) Json.parse(response.body());
        List<Object> choices = (List<Object>) body.get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new IllegalStateException("Chat response did not contain choices");
        }
        Map<String, Object> first = (Map<String, Object>) choices.get(0);
        Map<String, Object> message = (Map<String, Object>) first.get("message");
        return (String) message.getOrDefault("content", "");
    }

    private List<Map<String, Object>> toJsonMessages(List<ChatMessage> messages) {
        List<Map<String, Object>> jsonMessages = new ArrayList<>();
        for (ChatMessage message : messages) {
            Map<String, Object> object = new LinkedHashMap<>();
            object.put("role", message.role());
            object.put("content", message.content());
            jsonMessages.add(object);
        }
        return jsonMessages;
    }

    private void requireApiKey() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("AGENT_API_KEY is required for live chat requests");
        }
    }

    private String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
