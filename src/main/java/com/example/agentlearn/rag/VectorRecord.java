package com.example.agentlearn.rag;

import java.nio.file.Path;
import java.util.List;

public record VectorRecord(String id, Path source, String text, List<Double> embedding) {
}
