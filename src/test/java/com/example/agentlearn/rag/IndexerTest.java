package com.example.agentlearn.rag;

import com.example.agentlearn.TestSupport;
import com.example.agentlearn.llm.EmbeddingClient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class IndexerTest {
    public static void main(String[] args) throws Exception {
        rebuildsVectorStoreFromKnowledgeDirectory();
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
}
