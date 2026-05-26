# MCP Tool Use Prompt

When a user asks for file-oriented actions that are better handled outside the model, choose one available MCP tool.

Return one of these JSON objects:

```json
{"type":"final","answer":"..."}
```

```json
{"type":"mcp_tool_call","tool":"tool_name","arguments":{}}
```
