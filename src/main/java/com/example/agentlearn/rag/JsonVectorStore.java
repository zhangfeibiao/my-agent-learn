package com.example.agentlearn.rag;

import com.example.agentlearn.util.Json;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class JsonVectorStore {
    private final Path path;

    public JsonVectorStore(Path path) {
        this.path = path;
    }

    public void save(List<VectorRecord> records) throws IOException {
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        List<Map<String, Object>> jsonRecords = new ArrayList<>();
        for (VectorRecord record : records) {
            Map<String, Object> object = new LinkedHashMap<>();
            object.put("id", record.id());
            object.put("source", record.source().toString());
            object.put("text", record.text());
            object.put("embedding", record.embedding());
            jsonRecords.add(object);
        }
        Files.writeString(path, Json.stringify(jsonRecords), StandardCharsets.UTF_8);
    }

    @SuppressWarnings("unchecked")
    public List<VectorRecord> load() throws IOException {
        if (!Files.isRegularFile(path)) {
            return List.of();
        }

        Object parsed = Json.parse(Files.readString(path, StandardCharsets.UTF_8));
        if (!(parsed instanceof List<?> list)) {
            return List.of();
        }

        List<VectorRecord> records = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> map)) {
                continue;
            }
            String id = (String) map.get("id");
            String source = (String) map.get("source");
            String text = (String) map.get("text");
            List<Double> embedding = new ArrayList<>();
            Object rawEmbedding = map.get("embedding");
            if (rawEmbedding instanceof List<?> values) {
                for (Object value : values) {
                    embedding.add(((Number) value).doubleValue());
                }
            }
            records.add(new VectorRecord(id, Path.of(source), text, List.copyOf(embedding)));
        }
        return List.copyOf(records);
    }
}
