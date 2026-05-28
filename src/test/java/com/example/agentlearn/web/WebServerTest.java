package com.example.agentlearn.web;

import com.example.agentlearn.TestSupport;
import com.example.agentlearn.agent.AgentAnswer;
import com.example.agentlearn.config.AppConfig;
import com.example.agentlearn.llm.logging.JsonlLlmCallLogStore;
import com.example.agentlearn.llm.logging.LlmCallLog;
import com.example.agentlearn.rag.IndexResult;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class WebServerTest {
    public static void main(String[] args) throws Exception {
        servesChatAndKnowledgeApis();
    }

    private static void servesChatAndKnowledgeApis() throws Exception {
        Path root = Files.createTempDirectory("web-server");
        AppConfig config = AppConfig.from(Map.of(), root);
        JsonlLlmCallLogStore logStore = new JsonlLlmCallLogStore(root.resolve("data/logs.jsonl"));
        logStore.append(new LlmCallLog(
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
        ));
        WebServer server = new WebServer(
                0,
                config,
                new ManagedFileService(root.resolve("knowledge"), List.of(".md", ".txt")),
                new ManagedFileService(root.resolve("prompts"), List.of(".md")),
                new SkillFileService(root.resolve("skills")),
                message -> new AgentAnswer("Echo: " + message, List.of("notes/agent.md#chunk-0")),
                new IndexService() {
                    @Override
                    public IndexResult update() {
                        return new IndexResult(1, 0, 1, 1, 0);
                    }

                    @Override
                    public IndexResult rebuild() {
                        return new IndexResult(1, 0, 1, 1, 0);
                    }
                }
                ,
                logStore
        );
        server.start();
        try {
            HttpClient client = HttpClient.newHttpClient();
            URI baseUri = server.baseUri();

            HttpRequest chatRequest = HttpRequest.newBuilder(baseUri.resolve("/api/chat"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"message\":\"hello\"}"))
                    .build();
            HttpResponse<String> chatResponse = client.send(chatRequest, HttpResponse.BodyHandlers.ofString());

            TestSupport.assertEquals(chatResponse.statusCode(), 200);
            TestSupport.assertContains(chatResponse.body(), "Echo: hello");
            TestSupport.assertContains(chatResponse.body(), "notes/agent.md#chunk-0");

            HttpRequest saveRequest = HttpRequest.newBuilder(baseUri.resolve("/api/knowledge/file"))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString("{\"path\":\"notes/agent.md\",\"content\":\"# Agent\"}"))
                    .build();
            HttpResponse<String> saveResponse = client.send(saveRequest, HttpResponse.BodyHandlers.ofString());

            TestSupport.assertEquals(saveResponse.statusCode(), 200);

            HttpRequest readRequest = HttpRequest.newBuilder(baseUri.resolve("/api/knowledge/file?path=notes/agent.md"))
                    .GET()
                    .build();
            HttpResponse<String> readResponse = client.send(readRequest, HttpResponse.BodyHandlers.ofString());

            TestSupport.assertEquals(readResponse.statusCode(), 200);
            TestSupport.assertContains(readResponse.body(), "# Agent");

            HttpRequest logsRequest = HttpRequest.newBuilder(baseUri.resolve("/api/llm-logs"))
                    .GET()
                    .build();
            HttpResponse<String> logsResponse = client.send(logsRequest, HttpResponse.BodyHandlers.ofString());

            TestSupport.assertEquals(logsResponse.statusCode(), 200);
            TestSupport.assertContains(logsResponse.body(), "call-1");
            TestSupport.assertContains(logsResponse.body(), "session-1");

            HttpRequest logDetailRequest = HttpRequest.newBuilder(baseUri.resolve("/api/llm-logs/detail?id=call-1"))
                    .GET()
                    .build();
            HttpResponse<String> logDetailResponse = client.send(logDetailRequest, HttpResponse.BodyHandlers.ofString());

            TestSupport.assertEquals(logDetailResponse.statusCode(), 200);
            TestSupport.assertContains(logDetailResponse.body(), "hello");

            HttpRequest clearLogsRequest = HttpRequest.newBuilder(baseUri.resolve("/api/llm-logs"))
                    .DELETE()
                    .build();
            HttpResponse<String> clearLogsResponse = client.send(clearLogsRequest, HttpResponse.BodyHandlers.ofString());

            TestSupport.assertEquals(clearLogsResponse.statusCode(), 200);
            TestSupport.assertEquals(logStore.listLatest(10).size(), 0);
        } finally {
            server.stop();
        }
    }
}
