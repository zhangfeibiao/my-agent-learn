package com.example.agentlearn.mcp;

import com.example.agentlearn.util.Json;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public final class DemoMcpServer {
    private final Path knowledgeDir;

    private DemoMcpServer(Path knowledgeDir) {
        this.knowledgeDir = knowledgeDir.toAbsolutePath().normalize();
    }

    public static void main(String[] args) throws Exception {
        Path knowledgeDir = args.length > 0 ? Path.of(args[0]) : Path.of("knowledge");
        new DemoMcpServer(knowledgeDir).run();
    }

    private void run() throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8));

        String line;
        while ((line = reader.readLine()) != null) {
            writer.write(handle(line));
            writer.newLine();
            writer.flush();
        }
    }

    @SuppressWarnings("unchecked")
    private String handle(String line) {
        Object id = null;
        try {
            Map<String, Object> request = (Map<String, Object>) Json.parse(line);
            id = request.get("id");
            String method = String.valueOf(request.get("method"));
            Map<String, Object> params = request.get("params") instanceof Map<?, ?> rawParams
                    ? (Map<String, Object>) rawParams
                    : Map.of();

            return switch (method) {
                case "tools/list" -> McpJsonRpc.resultResponse(id, Map.of("tools", tools()));
                case "tools/call" -> McpJsonRpc.resultResponse(id, callTool(params));
                default -> McpJsonRpc.errorResponse(id, "Unknown method: " + method);
            };
        } catch (Exception e) {
            return McpJsonRpc.errorResponse(id, e.getMessage());
        }
    }

    private List<Map<String, Object>> tools() {
        List<Map<String, Object>> tools = new ArrayList<>();
        tools.add(tool("list_knowledge_files", "List Markdown and text files under the knowledge directory."));
        tools.add(tool("read_knowledge_file", "Read one Markdown or text file from the knowledge directory."));
        return tools;
    }

    private Map<String, Object> tool(String name, String description) {
        Map<String, Object> tool = new LinkedHashMap<>();
        tool.put("name", name);
        tool.put("description", description);
        return tool;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> callTool(Map<String, Object> params) throws Exception {
        String name = String.valueOf(params.get("name"));
        Map<String, Object> arguments = params.get("arguments") instanceof Map<?, ?> rawArguments
                ? (Map<String, Object>) rawArguments
                : Map.of();

        return switch (name) {
            case "list_knowledge_files" -> ok(String.join("\n", listKnowledgeFiles()));
            case "read_knowledge_file" -> ok(readKnowledgeFile(String.valueOf(arguments.getOrDefault("path", ""))));
            default -> fail("Unknown tool: " + name);
        };
    }

    private List<String> listKnowledgeFiles() throws Exception {
        if (!Files.isDirectory(knowledgeDir)) {
            return List.of();
        }
        try (Stream<Path> paths = Files.walk(knowledgeDir)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(this::isKnowledgeFile)
                    .sorted(Comparator.comparing(Path::toString))
                    .map(knowledgeDir::relativize)
                    .map(Path::toString)
                    .toList();
        }
    }

    private String readKnowledgeFile(String path) throws Exception {
        Path requested = knowledgeDir.resolve(path).toAbsolutePath().normalize();
        if (!requested.startsWith(knowledgeDir)) {
            throw new IllegalArgumentException("Path escapes knowledge directory");
        }
        if (!Files.isRegularFile(requested) || !isKnowledgeFile(requested)) {
            throw new IllegalArgumentException("Knowledge file not found: " + path);
        }
        return Files.readString(requested, StandardCharsets.UTF_8);
    }

    private boolean isKnowledgeFile(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        return fileName.endsWith(".md") || fileName.endsWith(".txt");
    }

    private Map<String, Object> ok(String content) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("content", content);
        return result;
    }

    private Map<String, Object> fail(String content) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", false);
        result.put("content", content);
        return result;
    }
}
