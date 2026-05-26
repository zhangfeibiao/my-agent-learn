package com.example.agentlearn.mcp;

import com.example.agentlearn.TestSupport;

import java.util.Map;

public final class McpMessageTest {
    public static void main(String[] args) {
        serializesJsonRpcRequest();
        parsesJsonRpcResponseResult();
    }

    private static void serializesJsonRpcRequest() {
        String request = McpJsonRpc.request(7, "tools/call", Map.of(
                "name", "list_knowledge_files",
                "arguments", Map.of()
        ));

        TestSupport.assertContains(request, "\"jsonrpc\":\"2.0\"");
        TestSupport.assertContains(request, "\"id\":7");
        TestSupport.assertContains(request, "\"method\":\"tools/call\"");
        TestSupport.assertContains(request, "\"name\":\"list_knowledge_files\"");
    }

    private static void parsesJsonRpcResponseResult() {
        Map<String, Object> result = McpJsonRpc.result("{\"jsonrpc\":\"2.0\",\"id\":7,\"result\":{\"content\":\"ok\"}}");

        TestSupport.assertEquals(result.get("content"), "ok");
    }
}
