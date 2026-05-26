package com.example.agentlearn.rag;

import java.nio.file.Path;

public record Document(Path source, String text) {
}
