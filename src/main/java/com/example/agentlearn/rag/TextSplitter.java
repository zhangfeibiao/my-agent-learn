package com.example.agentlearn.rag;

import java.util.ArrayList;
import java.util.List;

public final class TextSplitter {
    private final int maxChars;

    public TextSplitter(int maxChars) {
        if (maxChars < 32) {
            throw new IllegalArgumentException("maxChars must be at least 32");
        }
        this.maxChars = maxChars;
    }

    public List<Chunk> split(Document document) {
        List<String> paragraphs = List.of(document.text().split("\\R\\s*\\R"));
        List<Chunk> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String paragraph : paragraphs) {
            String cleaned = paragraph.strip();
            if (cleaned.isBlank()) {
                continue;
            }
            if (!current.isEmpty() && current.length() + cleaned.length() + 2 > maxChars) {
                addChunk(document, chunks, current.toString());
                current.setLength(0);
            }
            if (!current.isEmpty()) {
                current.append("\n\n");
            }
            current.append(cleaned);
        }

        if (!current.isEmpty()) {
            addChunk(document, chunks, current.toString());
        }

        return List.copyOf(chunks);
    }

    private void addChunk(Document document, List<Chunk> chunks, String text) {
        String id = document.source() + "#chunk-" + (chunks.size() + 1);
        chunks.add(new Chunk(id, document.source(), text));
    }
}
