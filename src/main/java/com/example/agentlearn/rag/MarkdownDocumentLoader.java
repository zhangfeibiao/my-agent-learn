package com.example.agentlearn.rag;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public final class MarkdownDocumentLoader {
    public List<Document> load(Path knowledgeDir) throws IOException {
        if (!Files.isDirectory(knowledgeDir)) {
            return List.of();
        }

        try (Stream<Path> paths = Files.walk(knowledgeDir)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(this::isKnowledgeFile)
                    .sorted(Comparator.comparing(Path::toString))
                    .map(this::readDocument)
                    .toList();
        }
    }

    private boolean isKnowledgeFile(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        return fileName.endsWith(".md") || fileName.endsWith(".txt");
    }

    private Document readDocument(Path path) {
        try {
            return new Document(path.normalize(), Files.readString(path, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read document " + path, e);
        }
    }
}
