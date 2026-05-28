package com.example.agentlearn.llm.logging;

import com.example.agentlearn.llm.ChatClient;
import com.example.agentlearn.llm.ChatMessage;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class LoggingChatClient implements ChatClient {
    private final ChatClient delegate;
    private final LlmCallLogStore logStore;
    private final String model;
    private final String endpoint;

    public LoggingChatClient(ChatClient delegate, LlmCallLogStore logStore, String model, String endpoint) {
        this.delegate = delegate;
        this.logStore = logStore;
        this.model = model;
        this.endpoint = endpoint;
    }

    @Override
    public String complete(List<ChatMessage> messages) throws Exception {
        Instant started = Instant.now();
        String id = UUID.randomUUID().toString();
        try {
            String output = delegate.complete(messages);
            appendLog(id, started, Map.of("content", output), null, messages);
            return output;
        } catch (Exception e) {
            appendLog(id, started, Map.of(), e.getMessage(), messages);
            throw e;
        }
    }

    private void appendLog(String id, Instant started, Map<String, Object> response, String error, List<ChatMessage> messages) throws Exception {
        Instant ended = Instant.now();
        logStore.append(new LlmCallLog(
                id,
                LlmCallContext.sessionId(),
                "chat",
                model,
                endpoint,
                false,
                started.toString(),
                ended.toString(),
                Duration.between(started, ended).toMillis(),
                Map.of("messages", jsonMessages(messages), "stream", false),
                response,
                error
        ));
    }

    private List<Map<String, Object>> jsonMessages(List<ChatMessage> messages) {
        List<Map<String, Object>> jsonMessages = new ArrayList<>();
        for (ChatMessage message : messages) {
            Map<String, Object> object = new LinkedHashMap<>();
            object.put("role", message.role());
            object.put("content", message.content());
            jsonMessages.add(object);
        }
        return jsonMessages;
    }
}
