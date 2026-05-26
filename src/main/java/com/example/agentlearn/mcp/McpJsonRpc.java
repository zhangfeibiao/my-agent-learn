package com.example.agentlearn.mcp;

import com.example.agentlearn.util.Json;

import java.util.LinkedHashMap;
import java.util.Map;

public final class McpJsonRpc {
    private McpJsonRpc() {
    }

    public static String request(int id, String method, Map<String, Object> params) {
        Map<String, Object> object = new LinkedHashMap<>();
        object.put("jsonrpc", "2.0");
        object.put("id", id);
        object.put("method", method);
        object.put("params", params);
        return Json.stringify(object);
    }

    public static String resultResponse(Object id, Object result) {
        Map<String, Object> object = new LinkedHashMap<>();
        object.put("jsonrpc", "2.0");
        object.put("id", id);
        object.put("result", result);
        return Json.stringify(object);
    }

    public static String errorResponse(Object id, String message) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("code", -32000);
        error.put("message", message);

        Map<String, Object> object = new LinkedHashMap<>();
        object.put("jsonrpc", "2.0");
        object.put("id", id);
        object.put("error", error);
        return Json.stringify(object);
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> result(String response) {
        Object parsed = Json.parse(response);
        if (!(parsed instanceof Map<?, ?> raw)) {
            throw new IllegalArgumentException("JSON-RPC response must be an object");
        }
        if (raw.containsKey("error")) {
            Object error = raw.get("error");
            throw new IllegalStateException("MCP error: " + error);
        }
        Object result = raw.get("result");
        if (result instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        Map<String, Object> wrapper = new LinkedHashMap<>();
        wrapper.put("value", result);
        return wrapper;
    }
}
