package com.example.agentlearn.llm.logging;

import com.example.agentlearn.TestSupport;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class LlmCallLogStoreTest {
    public static void main(String[] args) throws Exception {
        appendsListsReadsAndClearsLogs();
    }

    private static void appendsListsReadsAndClearsLogs() throws Exception {
        Path path = Files.createTempDirectory("llm-logs").resolve("logs.jsonl");
        JsonlLlmCallLogStore store = new JsonlLlmCallLogStore(path);

        LlmCallLog log = new LlmCallLog(
                "call-1",
                "session-1",
                "chat",
                "model-a",
                "/chat/completions",
                false,
                "2026-05-28T10:00:00Z",
                "2026-05-28T10:00:01Z",
                1000,
                Map.of("messages", List.of(Map.of("role", "user", "content", "hello"))),
                Map.of("content", "hi"),
                null
        );

        store.append(log);

        TestSupport.assertEquals(store.listLatest(10).size(), 1);
        TestSupport.assertEquals(store.findById("call-1").orElseThrow().sessionId(), "session-1");
        TestSupport.assertContains(Files.readString(path), "\"id\":\"call-1\"");

        store.clear();

        TestSupport.assertEquals(store.listLatest(10).size(), 0);
    }
}
