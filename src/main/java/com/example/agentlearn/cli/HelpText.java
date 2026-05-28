package com.example.agentlearn.cli;

public final class HelpText {
    private HelpText() {
    }

    public static String usage() {
        return """
                Usage:
                  ./scripts/run.sh [--help]

                Commands:
                  /index      Incrementally update data/vector-store.json from knowledge/*.md and *.txt.
                  /reindex    Rebuild data/vector-store.json from scratch.
                  /mcp-tools  List tools exposed by the demo MCP server.
                  /help       Show this help text.
                  /exit       Quit the CLI.

                Normal questions:
                  Type any question after indexing, for example:
                  什么是 Agent Skill？它和 MCP 有什么区别？
                """;
    }
}
