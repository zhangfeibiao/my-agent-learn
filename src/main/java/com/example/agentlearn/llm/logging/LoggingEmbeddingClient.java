package com.example.agentlearn.llm.logging;

import com.example.agentlearn.llm.EmbeddingClient;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class LoggingEmbeddingClient implements EmbeddingClient {
    private final EmbeddingClient delegate;
    private final LlmCallLogStore logStore;
    private final String model;
    private final String endpoint;

    public LoggingEmbeddingClient(EmbeddingClient delegate, LlmCallLogStore logStore, String model, String endpoint) {
        this.delegate = delegate;
        this.logStore = logStore;
        this.model = model;
        this.endpoint = endpoint;
    }

    @Override
    public List<Double> embed(String text) throws Exception {
        Instant started = Instant.now();
        String id = UUID.randomUUID().toString();
        try {
            List<Double> embedding = delegate.embed(text);
            appendLog(id, started, responseSummary(embedding), null, text);
            return embedding;
        } catch (Exception e) {
            appendLog(id, started, Map.of(), e.getMessage(), text);
            throw e;
        }
    }

    private void appendLog(String id, Instant started, Map<String, Object> response, String error, String text) throws Exception {
        Instant ended = Instant.now();
        logStore.append(new LlmCallLog(
                id,
                LlmCallContext.sessionId(),
                "embedding",
                model,
                endpoint,
                false,
                started.toString(),
                ended.toString(),
                Duration.between(started, ended).toMillis(),
                Map.of("input", text),
                response,
                error
        ));
    }

    private Map<String, Object> responseSummary(List<Double> embedding) {
        int sampleSize = Math.min(5, embedding.size());
        return Map.of(
                "dimensions", embedding.size(),
                "sample", embedding.subList(0, sampleSize)
        );
    }
}
