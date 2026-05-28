package com.example.agentlearn.rag;

import java.nio.file.Path;
import java.util.List;

public record VectorRecord(
        String id,
        Path source,
        String sourceHash,
        String text,
        List<Double> embedding,
        String embeddingModel,
        String splitterVersion
) {
    public VectorRecord(String id, Path source, String text, List<Double> embedding) {
        this(id, source, "", text, embedding, "", "");
    }
}
