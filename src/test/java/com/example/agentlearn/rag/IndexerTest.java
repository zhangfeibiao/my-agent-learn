package com.example.agentlearn.rag;

import com.example.agentlearn.TestSupport;
import com.example.agentlearn.llm.EmbeddingClient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class IndexerTest {
    public static void main(String[] args) throws Exception {
        rebuildsVectorStoreFromKnowledgeDirectory();
        reusesUnchangedFileRecordsDuringIncrementalIndexing();
        reembedsChangedFilesDuringIncrementalIndexing();
        removesDeletedFilesDuringIncrementalIndexing();
    }

    private static void rebuildsVectorStoreFromKnowledgeDirectory() throws Exception {
        Path root = Files.createTempDirectory("agent-index");
        Path knowledge = Files.createDirectories(root.resolve("knowledge"));
        Path vectorFile = root.resolve("data/vector-store.json");
        Files.writeString(knowledge.resolve("agent.md"), "# Agent\n\nAgents use loops, prompts, and tools.");

        EmbeddingClient fakeEmbedding = text -> List.of((double) text.length(), 1.0);
        JsonVectorStore store = new JsonVectorStore(vectorFile);
        Indexer indexer = new Indexer(
                new MarkdownDocumentLoader(),
                new TextSplitter(80),
                fakeEmbedding,
                store
        );

        int indexed = indexer.rebuild(knowledge);
        List<VectorRecord> records = store.load();

        TestSupport.assertEquals(indexed, 1);
        TestSupport.assertEquals(records.size(), 1);
        TestSupport.assertContains(records.get(0).text(), "Agents use loops");
        TestSupport.assertEquals(records.get(0).embedding(), List.of((double) records.get(0).text().length(), 1.0));
    }

    private static void reusesUnchangedFileRecordsDuringIncrementalIndexing() throws Exception {
        Path root = Files.createTempDirectory("agent-incremental-reuse");
        Path knowledge = Files.createDirectories(root.resolve("knowledge"));
        Path unchanged = knowledge.resolve("unchanged.md");
        Path added = knowledge.resolve("added.md");
        String unchangedText = "# Stable\n\nThis file did not change.";
        Files.writeString(unchanged, unchangedText);
        Files.writeString(added, "# Added\n\nThis file is new.");

        Path vectorFile = root.resolve("data/vector-store.json");
        JsonVectorStore store = new JsonVectorStore(vectorFile);
        store.save(List.of(new VectorRecord(
                unchanged + "#chunk-1",
                unchanged,
                Hashing.sha256(unchangedText),
                unchangedText,
                List.of(99.0, 99.0),
                "test-embedding",
                Indexer.DEFAULT_SPLITTER_VERSION
        )));

        CountingEmbeddingClient embedding = new CountingEmbeddingClient();
        Indexer indexer = new Indexer(new MarkdownDocumentLoader(), new TextSplitter(80), embedding, store, "test-embedding", Indexer.DEFAULT_SPLITTER_VERSION);

        IndexResult result = indexer.rebuildIncremental(knowledge);
        List<VectorRecord> records = store.load();

        TestSupport.assertEquals(result.reusedChunks(), 1);
        TestSupport.assertEquals(result.embeddedChunks(), 1);
        TestSupport.assertEquals(result.updatedFiles(), 1);
        TestSupport.assertEquals(result.deletedFiles(), 0);
        TestSupport.assertEquals(embedding.calls(), 1);
        TestSupport.assertTrue(records.stream().anyMatch(record -> record.embedding().equals(List.of(99.0, 99.0))), "Expected unchanged embedding to be reused");
        TestSupport.assertTrue(records.stream().anyMatch(record -> record.source().equals(added)), "Expected added file to be indexed");
    }

    private static void reembedsChangedFilesDuringIncrementalIndexing() throws Exception {
        Path root = Files.createTempDirectory("agent-incremental-change");
        Path knowledge = Files.createDirectories(root.resolve("knowledge"));
        Path changed = knowledge.resolve("changed.md");
        Files.writeString(changed, "# Changed\n\nNew content.");

        Path vectorFile = root.resolve("data/vector-store.json");
        JsonVectorStore store = new JsonVectorStore(vectorFile);
        store.save(List.of(new VectorRecord(
                changed + "#chunk-1",
                changed,
                "old-hash",
                "Old content.",
                List.of(1.0, 1.0),
                "test-embedding",
                Indexer.DEFAULT_SPLITTER_VERSION
        )));

        CountingEmbeddingClient embedding = new CountingEmbeddingClient();
        Indexer indexer = new Indexer(new MarkdownDocumentLoader(), new TextSplitter(80), embedding, store, "test-embedding", Indexer.DEFAULT_SPLITTER_VERSION);

        IndexResult result = indexer.rebuildIncremental(knowledge);
        List<VectorRecord> records = store.load();

        TestSupport.assertEquals(result.reusedChunks(), 0);
        TestSupport.assertEquals(result.embeddedChunks(), 1);
        TestSupport.assertEquals(embedding.calls(), 1);
        TestSupport.assertContains(records.get(0).text(), "New content");
        TestSupport.assertEquals(records.get(0).sourceHash(), Hashing.sha256(Files.readString(changed)));
    }

    private static void removesDeletedFilesDuringIncrementalIndexing() throws Exception {
        Path root = Files.createTempDirectory("agent-incremental-delete");
        Path knowledge = Files.createDirectories(root.resolve("knowledge"));
        Path kept = knowledge.resolve("kept.md");
        Path deleted = knowledge.resolve("deleted.md");
        String keptText = "# Kept\n\nStill here.";
        Files.writeString(kept, keptText);

        Path vectorFile = root.resolve("data/vector-store.json");
        JsonVectorStore store = new JsonVectorStore(vectorFile);
        store.save(List.of(
                new VectorRecord(kept + "#chunk-1", kept, Hashing.sha256(keptText), keptText, List.of(2.0), "test-embedding", Indexer.DEFAULT_SPLITTER_VERSION),
                new VectorRecord(deleted + "#chunk-1", deleted, "deleted-hash", "Gone.", List.of(3.0), "test-embedding", Indexer.DEFAULT_SPLITTER_VERSION)
        ));

        CountingEmbeddingClient embedding = new CountingEmbeddingClient();
        Indexer indexer = new Indexer(new MarkdownDocumentLoader(), new TextSplitter(80), embedding, store, "test-embedding", Indexer.DEFAULT_SPLITTER_VERSION);

        IndexResult result = indexer.rebuildIncremental(knowledge);
        List<VectorRecord> records = store.load();

        TestSupport.assertEquals(result.reusedChunks(), 1);
        TestSupport.assertEquals(result.embeddedChunks(), 0);
        TestSupport.assertEquals(result.deletedFiles(), 1);
        TestSupport.assertEquals(embedding.calls(), 0);
        TestSupport.assertTrue(records.stream().noneMatch(record -> record.source().equals(deleted)), "Expected deleted file records to be removed");
    }

    private static final class CountingEmbeddingClient implements EmbeddingClient {
        private final List<String> embeddedTexts = new ArrayList<>();

        @Override
        public List<Double> embed(String text) {
            embeddedTexts.add(text);
            return List.of((double) text.length(), (double) embeddedTexts.size());
        }

        private int calls() {
            return embeddedTexts.size();
        }
    }
}
