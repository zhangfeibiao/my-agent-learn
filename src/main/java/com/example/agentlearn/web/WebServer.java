package com.example.agentlearn.web;

import com.example.agentlearn.agent.AgentAnswer;
import com.example.agentlearn.config.AppConfig;
import com.example.agentlearn.llm.logging.LlmCallLog;
import com.example.agentlearn.llm.logging.LlmCallLogStore;
import com.example.agentlearn.rag.IndexResult;
import com.example.agentlearn.util.Json;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WebServer {
    private final HttpServer server;
    private final AppConfig config;
    private final ManagedFileService knowledgeFiles;
    private final ManagedFileService promptFiles;
    private final SkillFileService skillFiles;
    private final ChatService chatService;
    private final IndexService indexService;
    private final LlmCallLogStore llmCallLogStore;

    public WebServer(
            int port,
            AppConfig config,
            ManagedFileService knowledgeFiles,
            ManagedFileService promptFiles,
            SkillFileService skillFiles,
            ChatService chatService,
            IndexService indexService,
            LlmCallLogStore llmCallLogStore
    ) throws IOException {
        this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        this.config = config;
        this.knowledgeFiles = knowledgeFiles;
        this.promptFiles = promptFiles;
        this.skillFiles = skillFiles;
        this.chatService = chatService;
        this.indexService = indexService;
        this.llmCallLogStore = llmCallLogStore;
        this.server.createContext("/", this::handle);
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(0);
    }

    public URI baseUri() {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/");
    }

    private void handle(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            if (path.startsWith("/api/")) {
                handleApi(exchange, path);
            } else {
                handleStatic(exchange, path);
            }
        } catch (IllegalArgumentException e) {
            sendJson(exchange, 400, Map.of("error", e.getMessage()));
        } catch (Exception e) {
            sendJson(exchange, 500, Map.of("error", e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
        }
    }

    private void handleApi(HttpExchange exchange, String path) throws Exception {
        String method = exchange.getRequestMethod();
        if ("/api/config".equals(path)) {
            requireMethod(method, "GET");
            sendJson(exchange, 200, configSummary());
            return;
        }
        if ("/api/chat".equals(path)) {
            requireMethod(method, "POST");
            handleChat(exchange);
            return;
        }
        if ("/api/index".equals(path)) {
            requireMethod(method, "POST");
            sendJson(exchange, 200, indexResult(indexService.update()));
            return;
        }
        if ("/api/reindex".equals(path)) {
            requireMethod(method, "POST");
            sendJson(exchange, 200, indexResult(indexService.rebuild()));
            return;
        }
        if ("/api/llm-logs".equals(path)) {
            handleLlmLogs(exchange, method);
            return;
        }
        if ("/api/llm-logs/detail".equals(path)) {
            requireMethod(method, "GET");
            handleLlmLogDetail(exchange);
            return;
        }
        if ("/api/knowledge".equals(path)) {
            requireMethod(method, "GET");
            sendJson(exchange, 200, Map.of("files", managedFiles(knowledgeFiles.list())));
            return;
        }
        if ("/api/knowledge/file".equals(path)) {
            handleManagedFile(exchange, method, knowledgeFiles);
            return;
        }
        if ("/api/prompts".equals(path)) {
            requireMethod(method, "GET");
            sendJson(exchange, 200, Map.of("files", managedFiles(promptFiles.list())));
            return;
        }
        if ("/api/prompts/file".equals(path)) {
            handleManagedFile(exchange, method, promptFiles);
            return;
        }
        if ("/api/skills".equals(path)) {
            requireMethod(method, "GET");
            sendJson(exchange, 200, Map.of("files", skillFiles()));
            return;
        }
        if ("/api/skills/file".equals(path)) {
            handleSkillFile(exchange, method);
            return;
        }
        sendJson(exchange, 404, Map.of("error", "Not found"));
    }

    private void handleChat(HttpExchange exchange) throws Exception {
        Map<String, Object> request = readJsonObject(exchange);
        String message = stringField(request, "message");
        AgentAnswer answer = chatService.answer(message);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("answer", answer.answer());
        response.put("sources", answer.sources());
        sendJson(exchange, 200, response);
    }

    private void handleManagedFile(HttpExchange exchange, String method, ManagedFileService files) throws IOException {
        if ("GET".equals(method)) {
            String path = queryParam(exchange, "path");
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("path", path);
            response.put("content", files.read(path));
            sendJson(exchange, 200, response);
            return;
        }
        if ("PUT".equals(method)) {
            Map<String, Object> request = readJsonObject(exchange);
            files.write(stringField(request, "path"), stringField(request, "content"));
            sendJson(exchange, 200, Map.of("ok", true));
            return;
        }
        if ("DELETE".equals(method)) {
            files.delete(queryParam(exchange, "path"));
            sendJson(exchange, 200, Map.of("ok", true));
            return;
        }
        sendJson(exchange, 405, Map.of("error", "Method not allowed"));
    }

    private void handleSkillFile(HttpExchange exchange, String method) throws IOException {
        if ("GET".equals(method)) {
            String name = queryParam(exchange, "name");
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("name", name);
            response.put("content", skillFiles.read(name));
            sendJson(exchange, 200, response);
            return;
        }
        if ("PUT".equals(method)) {
            Map<String, Object> request = readJsonObject(exchange);
            skillFiles.write(stringField(request, "name"), stringField(request, "content"));
            sendJson(exchange, 200, Map.of("ok", true));
            return;
        }
        if ("DELETE".equals(method)) {
            skillFiles.delete(queryParam(exchange, "name"));
            sendJson(exchange, 200, Map.of("ok", true));
            return;
        }
        sendJson(exchange, 405, Map.of("error", "Method not allowed"));
    }

    private Map<String, Object> configSummary() throws IOException {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("knowledgeDir", config.knowledgeDir().toString());
        response.put("skillsDir", config.skillsDir().toString());
        response.put("promptsDir", config.promptsDir().toString());
        response.put("vectorStorePath", config.vectorStorePath().toString());
        response.put("llmLogPath", config.llmLogPath().toString());
        response.put("apiBaseUrl", config.apiBaseUrl());
        response.put("chatModel", config.chatModel());
        response.put("embeddingModel", config.embeddingModel());
        response.put("debug", config.debug());
        response.put("vectorStoreExists", Files.isRegularFile(config.vectorStorePath()));
        response.put("knowledgeFileCount", knowledgeFiles.list().size());
        response.put("skillCount", skillFiles.list().size());
        response.put("promptFileCount", promptFiles.list().size());
        return response;
    }

    private void handleLlmLogs(HttpExchange exchange, String method) throws IOException {
        if ("GET".equals(method)) {
            sendJson(exchange, 200, Map.of("logs", llmLogs(llmCallLogStore.listLatest(200))));
            return;
        }
        if ("DELETE".equals(method)) {
            llmCallLogStore.clear();
            sendJson(exchange, 200, Map.of("ok", true));
            return;
        }
        sendJson(exchange, 405, Map.of("error", "Method not allowed"));
    }

    private void handleLlmLogDetail(HttpExchange exchange) throws IOException {
        String id = queryParam(exchange, "id");
        LlmCallLog log = llmCallLogStore.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Log not found"));
        sendJson(exchange, 200, Map.of("log", llmLog(log)));
    }

    private List<Map<String, Object>> managedFiles(List<ManagedFile> files) {
        List<Map<String, Object>> response = new ArrayList<>();
        for (ManagedFile file : files) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("path", file.path());
            item.put("size", file.size());
            response.add(item);
        }
        return response;
    }

    private List<Map<String, Object>> skillFiles() throws IOException {
        List<Map<String, Object>> response = new ArrayList<>();
        for (SkillFile file : skillFiles.list()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", file.name());
            item.put("size", file.size());
            response.add(item);
        }
        return response;
    }

    private Map<String, Object> indexResult(IndexResult result) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalChunks", result.totalChunks());
        response.put("reusedChunks", result.reusedChunks());
        response.put("embeddedChunks", result.embeddedChunks());
        response.put("updatedFiles", result.updatedFiles());
        response.put("deletedFiles", result.deletedFiles());
        return response;
    }

    private List<Map<String, Object>> llmLogs(List<LlmCallLog> logs) {
        List<Map<String, Object>> response = new ArrayList<>();
        for (LlmCallLog log : logs) {
            response.add(llmLog(log));
        }
        return response;
    }

    private Map<String, Object> llmLog(LlmCallLog log) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", log.id());
        response.put("sessionId", log.sessionId());
        response.put("type", log.type());
        response.put("model", log.model());
        response.put("endpoint", log.endpoint());
        response.put("stream", log.stream());
        response.put("startedAt", log.startedAt());
        response.put("endedAt", log.endedAt());
        response.put("durationMs", log.durationMs());
        response.put("request", log.request());
        response.put("response", log.response());
        response.put("error", log.error());
        return response;
    }

    private void handleStatic(HttpExchange exchange, String path) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendJson(exchange, 405, Map.of("error", "Method not allowed"));
            return;
        }
        if (path.contains("..")) {
            sendJson(exchange, 404, Map.of("error", "Not found"));
            return;
        }

        String resource = "/".equals(path) ? "web/index.html" : "web" + path;
        if (resource.endsWith("/")) {
            resource += "index.html";
        }
        try (InputStream input = WebServer.class.getClassLoader().getResourceAsStream(resource)) {
            if (input == null) {
                sendJson(exchange, 404, Map.of("error", "Not found"));
                return;
            }
            byte[] bytes = input.readAllBytes();
            exchange.getResponseHeaders().set("Content-Type", contentType(resource));
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(bytes);
            }
        }
    }

    private String contentType(String resource) {
        if (resource.endsWith(".html")) {
            return "text/html; charset=utf-8";
        }
        if (resource.endsWith(".css")) {
            return "text/css; charset=utf-8";
        }
        if (resource.endsWith(".js")) {
            return "application/javascript; charset=utf-8";
        }
        return "application/octet-stream";
    }

    private Map<String, Object> readJsonObject(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        if (body.isBlank()) {
            return new LinkedHashMap<>();
        }
        Object parsed = Json.parse(body);
        if (!(parsed instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("JSON object is required");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            result.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return result;
    }

    private String stringField(Map<String, Object> request, String field) {
        Object value = request.get(field);
        if (!(value instanceof String string) || string.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return string;
    }

    private String queryParam(HttpExchange exchange, String name) {
        String rawQuery = exchange.getRequestURI().getRawQuery();
        if (rawQuery == null || rawQuery.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        for (String part : rawQuery.split("&")) {
            int separator = part.indexOf('=');
            if (separator < 0) {
                continue;
            }
            String key = decode(part.substring(0, separator));
            if (name.equals(key)) {
                String value = decode(part.substring(separator + 1));
                if (value.isBlank()) {
                    throw new IllegalArgumentException(name + " is required");
                }
                return value;
            }
        }
        throw new IllegalArgumentException(name + " is required");
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private void requireMethod(String actual, String expected) {
        if (!expected.equals(actual)) {
            throw new IllegalArgumentException("Expected " + expected);
        }
    }

    private void sendJson(HttpExchange exchange, int status, Object value) throws IOException {
        byte[] bytes = Json.stringify(value).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
