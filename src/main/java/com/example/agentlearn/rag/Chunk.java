package com.example.agentlearn.rag;

import java.nio.file.Path;

public record Chunk(String id, Path source, String text) {
}
