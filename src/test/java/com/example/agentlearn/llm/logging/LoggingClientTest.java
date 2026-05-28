package com.example.agentlearn.llm.logging;

import com.example.agentlearn.TestSupport;
import com.example.agentlearn.llm.ChatMessage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class LoggingClientTest {
    public static void main(String[] args) throws Exception {
        recordsChatAndEmbeddingCallsWithSessionAndTiming();
    }

    private static void recordsChatAndEmbeddingCallsWithSessionAndTiming() throws Exception {
        Path path = Files.createTempDirectory("llm-logs").resolve("logs.jsonl");
        JsonlLlmCallLogStore store = new JsonlLlmCallLogStore(path);
        LoggingChatClient chatClient = new LoggingChatClient(
                messages -> "你好",
                store,
                "deepseek-v4-flash",
                "/chat/completions"
        );
        LoggingEmbeddingClient embeddingClient = new LoggingEmbeddingClient(
                text -> List.of(0.1, 0.2, 0.3),
                store,
                "embedding-model",
                "/embeddings"
        );

        LlmCallContext.withSession("session-1", () -> {
            TestSupport.assertEquals(chatClient.complete(List.of(new ChatMessage("user", "hello"))), "你好");
            TestSupport.assertEquals(embeddingClient.embed("hello").size(), 3);
        });

        List<LlmCallLog> logs = store.listLatest(10);

        TestSupport.assertEquals(logs.size(), 2);
        TestSupport.assertTrue(logs.stream().allMatch(log -> "session-1".equals(log.sessionId())), "Expected same session id");
        TestSupport.assertTrue(logs.stream().allMatch(log -> !log.id().isBlank()), "Expected generated ids");
        TestSupport.assertTrue(logs.stream().allMatch(log -> !log.startedAt().isBlank() && !log.endedAt().isBlank()), "Expected timing fields");
        TestSupport.assertTrue(logs.stream().anyMatch(log -> "chat".equals(log.type()) && log.response().toString().contains("你好")), "Expected chat output log");
        TestSupport.assertTrue(logs.stream().anyMatch(log -> "embedding".equals(log.type()) && log.response().toString().contains("dimensions=3")), "Expected embedding summary log");
    }
}
