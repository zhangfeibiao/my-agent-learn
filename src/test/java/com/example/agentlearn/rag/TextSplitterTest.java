package com.example.agentlearn.rag;

import com.example.agentlearn.TestSupport;

import java.nio.file.Path;
import java.util.List;

public final class TextSplitterTest {
    public static void main(String[] args) {
        splitsDocumentsIntoStableChunks();
    }

    private static void splitsDocumentsIntoStableChunks() {
        Document document = new Document(
                Path.of("knowledge/mcp.md"),
                "# MCP\n\nMCP exposes external tools to an agent.\n\nThe agent calls tools and reads observations."
        );

        List<Chunk> chunks = new TextSplitter(48).split(document);

        TestSupport.assertEquals(chunks.size(), 2);
        TestSupport.assertEquals(chunks.get(0).id(), "knowledge/mcp.md#chunk-1");
        TestSupport.assertEquals(chunks.get(1).id(), "knowledge/mcp.md#chunk-2");
        TestSupport.assertContains(chunks.get(0).text(), "MCP exposes");
    }
}
