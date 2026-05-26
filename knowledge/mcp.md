# MCP

MCP 是 Model Context Protocol。它把外部工具暴露给 Agent，让 Agent 可以用统一协议发现工具、查看参数，并发起调用。

MCP 和本地 Java 方法的区别在于边界：

- Java 方法通常是进程内调用。
- MCP 工具通常是进程外能力，通过 JSON-RPC、stdio 或网络通信。

在这个项目里，MCP 只实现最小子集：

- `tools/list`：列出可用工具。
- `tools/call`：调用某个工具。

示例 MCP Server 只允许访问 `knowledge/` 目录，提供 `list_knowledge_files` 和 `read_knowledge_file` 两个工具。
