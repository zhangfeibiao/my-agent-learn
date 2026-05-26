package com.example.agentlearn.agent;

import com.example.agentlearn.util.Json;

import java.util.LinkedHashMap;
import java.util.Map;

public record ModelDirective(Type type, String answer, String tool, Map<String, Object> arguments) {
    public enum Type {
        FINAL,
        MCP_TOOL_CALL
    }

    public static ModelDirective finalAnswer(String answer) {
        return new ModelDirective(Type.FINAL, answer, "", Map.of());
    }

    public static ModelDirective toolCall(String tool, Map<String, Object> arguments) {
        return new ModelDirective(Type.MCP_TOOL_CALL, "", tool, Map.copyOf(arguments));
    }

    @SuppressWarnings("unchecked")
    public static ModelDirective parse(String text) {
        try {
            Object parsed = Json.parse(text);
            if (!(parsed instanceof Map<?, ?> raw)) {
                return finalAnswer(text);
            }
            Map<String, Object> map = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : raw.entrySet()) {
                map.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            String type = String.valueOf(map.getOrDefault("type", "final"));
            if ("mcp_tool_call".equals(type)) {
                Object rawArguments = map.get("arguments");
                Map<String, Object> arguments = rawArguments instanceof Map<?, ?> argumentMap
                        ? (Map<String, Object>) argumentMap
                        : Map.of();
                return toolCall(String.valueOf(map.getOrDefault("tool", "")), arguments);
            }
            return finalAnswer(String.valueOf(map.getOrDefault("answer", text)));
        } catch (RuntimeException e) {
            return finalAnswer(text);
        }
    }
}
