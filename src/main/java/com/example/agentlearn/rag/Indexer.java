package com.example.agentlearn.rag;

import com.example.agentlearn.llm.EmbeddingClient;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class Indexer {
    private final MarkdownDocumentLoader documentLoader;
    private final TextSplitter textSplitter;
    private final EmbeddingClient embeddingClient;
    private final JsonVectorStore vectorStore;

    public Indexer(
            MarkdownDocumentLoader documentLoader,
            TextSplitter textSplitter,
            EmbeddingClient embeddingClient,
            JsonVectorStore vectorStore
    ) {
        this.documentLoader = documentLoader;
        this.textSplitter = textSplitter;
        this.embeddingClient = embeddingClient;
        this.vectorStore = vectorStore;
    }

    public int rebuild(Path knowledgeDir) throws Exception {
        List<VectorRecord> records = new ArrayList<>();
        for (Document document : documentLoader.load(knowledgeDir)) {
            for (Chunk chunk : textSplitter.split(document)) {
                records.add(new VectorRecord(
                        chunk.id(),
                        chunk.source(),
                        chunk.text(),
                        embeddingClient.embed(chunk.text())
                ));
            }
        }
        vectorStore.save(records);
        return records.size();
    }
}
