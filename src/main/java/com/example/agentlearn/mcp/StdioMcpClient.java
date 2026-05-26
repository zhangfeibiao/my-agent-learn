package com.example.agentlearn.mcp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class StdioMcpClient implements McpClient, Closeable {
    private final Process process;
    private final BufferedReader reader;
    private final BufferedWriter writer;
    private int nextId = 1;

    public StdioMcpClient(List<String> command) throws Exception {
        this.process = new ProcessBuilder(command).start();
        this.reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
        this.writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
    }

    @Override
    @SuppressWarnings("unchecked")
    public synchronized List<McpTool> listTools() throws Exception {
        Map<String, Object> result = send("tools/list", Map.of());
        Object toolsValue = result.get("tools");
        if (!(toolsValue instanceof List<?> tools)) {
            return List.of();
        }

        List<McpTool> parsed = new ArrayList<>();
        for (Object item : tools) {
            if (item instanceof Map<?, ?> tool) {
                parsed.add(new McpTool(
                        String.valueOf(tool.get("name")),
                        String.valueOf(tool.containsKey("description") ? tool.get("description") : "")
                ));
            }
        }
        return List.copyOf(parsed);
    }

    @Override
    public synchronized McpToolCallResult callTool(String name, Map<String, Object> arguments) throws Exception {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("name", name);
        params.put("arguments", arguments);
        Map<String, Object> result = send("tools/call", params);
        boolean success = Boolean.TRUE.equals(result.getOrDefault("success", Boolean.TRUE));
        String content = String.valueOf(result.getOrDefault("content", ""));
        return new McpToolCallResult(success, content);
    }

    private Map<String, Object> send(String method, Map<String, Object> params) throws Exception {
        int id = nextId++;
        writer.write(McpJsonRpc.request(id, method, params));
        writer.newLine();
        writer.flush();

        String response = reader.readLine();
        if (response == null) {
            throw new IllegalStateException("MCP server closed stdout");
        }
        return McpJsonRpc.result(response);
    }

    @Override
    public void close() {
        process.destroy();
    }
}
