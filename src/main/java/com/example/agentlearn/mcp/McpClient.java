package com.example.agentlearn.mcp;

import java.util.List;
import java.util.Map;

public interface McpClient {
    List<McpTool> listTools() throws Exception;

    McpToolCallResult callTool(String name, Map<String, Object> arguments) throws Exception;
}
