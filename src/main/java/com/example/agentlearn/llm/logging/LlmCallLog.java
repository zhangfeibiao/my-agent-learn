package com.example.agentlearn.llm.logging;

import java.util.Map;

public record LlmCallLog(
        String id,
        String sessionId,
        String type,
        String model,
        String endpoint,
        boolean stream,
        String startedAt,
        String endedAt,
        long durationMs,
        Map<String, Object> request,
        Map<String, Object> response,
        String error
) {
}
