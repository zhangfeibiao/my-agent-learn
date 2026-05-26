package com.example.agentlearn.rag;

import java.io.IOException;
import java.util.List;

public final class Retriever {
    private final JsonVectorStore vectorStore;

    public Retriever(JsonVectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public List<Chunk> retrieve(List<Double> queryEmbedding, int topK) throws IOException {
        return CosineSimilarity.rank(queryEmbedding, vectorStore.load(), topK).stream()
                .map(record -> new Chunk(record.id(), record.source(), record.text()))
                .toList();
    }
}
