package com.example.agentlearn.rag;

import com.example.agentlearn.llm.EmbeddingClient;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class Indexer {
    public static final String DEFAULT_SPLITTER_VERSION = "paragraph-v1";

    private final MarkdownDocumentLoader documentLoader;
    private final TextSplitter textSplitter;
    private final EmbeddingClient embeddingClient;
    private final JsonVectorStore vectorStore;
    private final String embeddingModel;
    private final String splitterVersion;

    public Indexer(
            MarkdownDocumentLoader documentLoader,
            TextSplitter textSplitter,
            EmbeddingClient embeddingClient,
            JsonVectorStore vectorStore
    ) {
        this(documentLoader, textSplitter, embeddingClient, vectorStore, "", DEFAULT_SPLITTER_VERSION);
    }

    public Indexer(
            MarkdownDocumentLoader documentLoader,
            TextSplitter textSplitter,
            EmbeddingClient embeddingClient,
            JsonVectorStore vectorStore,
            String embeddingModel,
            String splitterVersion
    ) {
        this.documentLoader = documentLoader;
        this.textSplitter = textSplitter;
        this.embeddingClient = embeddingClient;
        this.vectorStore = vectorStore;
        this.embeddingModel = embeddingModel;
        this.splitterVersion = splitterVersion;
    }

    public int rebuild(Path knowledgeDir) throws Exception {
        return rebuildFull(knowledgeDir).totalChunks();
    }

    public IndexResult rebuildFull(Path knowledgeDir) throws Exception {
        List<VectorRecord> records = new ArrayList<>();
        int updatedFiles = 0;
        for (Document document : documentLoader.load(knowledgeDir)) {
            String sourceHash = Hashing.sha256(document.text());
            List<Chunk> chunks = textSplitter.split(document);
            records.addAll(embedChunks(chunks, sourceHash));
            updatedFiles++;
        }
        vectorStore.save(records);
        return new IndexResult(records.size(), 0, records.size(), updatedFiles, 0);
    }

    public IndexResult rebuildIncremental(Path knowledgeDir) throws Exception {
        List<VectorRecord> existingRecords = vectorStore.load();
        Map<Path, List<VectorRecord>> existingBySource = existingRecords.stream()
                .collect(Collectors.groupingBy(VectorRecord::source));

        List<VectorRecord> newRecords = new ArrayList<>();
        Set<Path> currentSources = new HashSet<>();
        int reusedChunks = 0;
        int embeddedChunks = 0;
        int updatedFiles = 0;

        for (Document document : documentLoader.load(knowledgeDir)) {
            currentSources.add(document.source());
            String sourceHash = Hashing.sha256(document.text());
            List<VectorRecord> previousRecords = existingBySource.get(document.source());

            if (canReuse(previousRecords, sourceHash)) {
                newRecords.addAll(previousRecords);
                reusedChunks += previousRecords.size();
                continue;
            }

            List<Chunk> chunks = textSplitter.split(document);
            List<VectorRecord> embeddedRecords = embedChunks(chunks, sourceHash);
            newRecords.addAll(embeddedRecords);
            embeddedChunks += embeddedRecords.size();
            updatedFiles++;
        }

        int deletedFiles = 0;
        for (Path source : existingBySource.keySet()) {
            if (!currentSources.contains(source)) {
                deletedFiles++;
            }
        }

        vectorStore.save(newRecords);
        return new IndexResult(newRecords.size(), reusedChunks, embeddedChunks, updatedFiles, deletedFiles);
    }

    private boolean canReuse(List<VectorRecord> previousRecords, String sourceHash) {
        if (previousRecords == null || previousRecords.isEmpty()) {
            return false;
        }
        for (VectorRecord record : previousRecords) {
            if (!sourceHash.equals(record.sourceHash())) {
                return false;
            }
            if (!embeddingModel.equals(record.embeddingModel())) {
                return false;
            }
            if (!splitterVersion.equals(record.splitterVersion())) {
                return false;
            }
        }
        return true;
    }

    private List<VectorRecord> embedChunks(List<Chunk> chunks, String sourceHash) throws Exception {
        List<VectorRecord> records = new ArrayList<>();
        for (Chunk chunk : chunks) {
            records.add(new VectorRecord(
                    chunk.id(),
                    chunk.source(),
                    sourceHash,
                    chunk.text(),
                    embeddingClient.embed(chunk.text()),
                    embeddingModel,
                    splitterVersion
            ));
        }
        return records;
    }
}
