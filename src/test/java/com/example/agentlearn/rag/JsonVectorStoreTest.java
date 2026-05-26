package com.example.agentlearn.rag;

import com.example.agentlearn.TestSupport;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class JsonVectorStoreTest {
    public static void main(String[] args) throws Exception {
        savesAndLoadsVectorRecords();
    }

    private static void savesAndLoadsVectorRecords() throws Exception {
        Path file = Files.createTempDirectory("vectors").resolve("vector-store.json");
        JsonVectorStore store = new JsonVectorStore(file);
        List<VectorRecord> records = List.of(
                new VectorRecord("knowledge/mcp.md#chunk-1", Path.of("knowledge/mcp.md"), "MCP text", List.of(1.0, 0.0)),
                new VectorRecord("knowledge/rag.md#chunk-1", Path.of("knowledge/rag.md"), "RAG text", List.of(0.0, 1.0))
        );

        store.save(records);
        List<VectorRecord> loaded = store.load();

        TestSupport.assertEquals(loaded.size(), 2);
        TestSupport.assertEquals(loaded.get(0).id(), "knowledge/mcp.md#chunk-1");
        TestSupport.assertEquals(loaded.get(0).source(), Path.of("knowledge/mcp.md"));
        TestSupport.assertEquals(loaded.get(0).embedding(), List.of(1.0, 0.0));
    }
}
