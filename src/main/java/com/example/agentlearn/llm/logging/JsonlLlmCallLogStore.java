package com.example.agentlearn.llm.logging;

import com.example.agentlearn.util.Json;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class JsonlLlmCallLogStore implements LlmCallLogStore {
    private final Path path;

    public JsonlLlmCallLogStore(Path path) {
        this.path = path;
    }

    @Override
    public synchronized void append(LlmCallLog log) throws IOException {
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        Files.writeString(
                path,
                Json.stringify(toMap(log)) + System.lineSeparator(),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
        );
    }

    @Override
    public synchronized List<LlmCallLog> listLatest(int limit) throws IOException {
        List<LlmCallLog> logs = readAll();
        Collections.reverse(logs);
        if (limit > 0 && logs.size() > limit) {
            return List.copyOf(logs.subList(0, limit));
        }
        return List.copyOf(logs);
    }

    @Override
    public synchronized Optional<LlmCallLog> findById(String id) throws IOException {
        return readAll().stream()
                .filter(log -> log.id().equals(id))
                .findFirst();
    }

    @Override
    public synchronized void clear() throws IOException {
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        Files.writeString(path, "", StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private List<LlmCallLog> readAll() throws IOException {
        if (!Files.isRegularFile(path)) {
            return new ArrayList<>();
        }

        List<LlmCallLog> logs = new ArrayList<>();
        for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            if (line.isBlank()) {
                continue;
            }
            Object parsed = Json.parse(line);
            if (parsed instanceof Map<?, ?> map) {
                logs.add(fromMap(map));
            }
        }
        return logs;
    }

    private Map<String, Object> toMap(LlmCallLog log) {
        Map<String, Object> object = new LinkedHashMap<>();
        object.put("id", log.id());
        object.put("sessionId", log.sessionId());
        object.put("type", log.type());
        object.put("model", log.model());
        object.put("endpoint", log.endpoint());
        object.put("stream", log.stream());
        object.put("startedAt", log.startedAt());
        object.put("endedAt", log.endedAt());
        object.put("durationMs", log.durationMs());
        object.put("request", log.request());
        object.put("response", log.response());
        object.put("error", log.error());
        return object;
    }

    @SuppressWarnings("unchecked")
    private LlmCallLog fromMap(Map<?, ?> map) {
        return new LlmCallLog(
                stringValue(map.get("id")),
                stringValue(map.get("sessionId")),
                stringValue(map.get("type")),
                stringValue(map.get("model")),
                stringValue(map.get("endpoint")),
                Boolean.TRUE.equals(map.get("stream")),
                stringValue(map.get("startedAt")),
                stringValue(map.get("endedAt")),
                longValue(map.get("durationMs")),
                mapValue(map.get("request")),
                mapValue(map.get("response")),
                nullIfBlank(map.get("error"))
        );
    }

    private Map<String, Object> mapValue(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            result.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return result;
    }

    private long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return 0;
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String nullIfBlank(Object value) {
        String text = stringValue(value);
        return text.isBlank() ? null : text;
    }
}
