package com.example.agentlearn.cli;

import com.example.agentlearn.agent.Agent;
import com.example.agentlearn.agent.AgentAnswer;
import com.example.agentlearn.config.AppConfig;
import com.example.agentlearn.mcp.McpClient;
import com.example.agentlearn.mcp.McpTool;
import com.example.agentlearn.rag.IndexResult;
import com.example.agentlearn.rag.Indexer;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Scanner;

public final class ConsoleSession {
    private final AppConfig config;
    private final InputStream input;
    private final PrintStream output;
    private final Indexer indexer;
    private final Agent agent;
    private final McpClient mcpClient;

    public ConsoleSession(AppConfig config, InputStream input, PrintStream output, Indexer indexer, Agent agent, McpClient mcpClient) {
        this.config = config;
        this.input = input;
        this.output = output;
        this.indexer = indexer;
        this.agent = agent;
        this.mcpClient = mcpClient;
    }

    public static ConsoleSession placeholder(AppConfig config) {
        return new ConsoleSession(config, System.in, System.out, null, null, null);
    }

    public void run() {
        output.println("Personal Knowledge Agent");
        output.println("Type /index to update the knowledge index, /reindex to rebuild it fully, /exit to quit.");
        output.println();

        Scanner scanner = new Scanner(input);
        while (true) {
            output.print("> ");
            if (!scanner.hasNextLine()) {
                output.println();
                return;
            }
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) {
                continue;
            }
            if ("/exit".equals(line)) {
                output.println("Bye.");
                return;
            }
            if ("/index".equals(line)) {
                updateIndex();
                continue;
            }
            if ("/reindex".equals(line)) {
                rebuildIndex();
                continue;
            }
            if ("/mcp-tools".equals(line)) {
                printMcpTools();
                continue;
            }
            answer(line);
        }
    }

    private void updateIndex() {
        if (indexer == null) {
            output.println("Indexer is not configured.");
            return;
        }
        try {
            IndexResult result = indexer.rebuildIncremental(config.knowledgeDir());
            printIndexResult("Updated", result);
        } catch (Exception e) {
            output.println("Indexing failed: " + e.getMessage());
        }
    }

    private void rebuildIndex() {
        if (indexer == null) {
            output.println("Indexer is not configured.");
            return;
        }
        try {
            IndexResult result = indexer.rebuildFull(config.knowledgeDir());
            printIndexResult("Rebuilt", result);
        } catch (Exception e) {
            output.println("Indexing failed: " + e.getMessage());
        }
    }

    private void printIndexResult(String action, IndexResult result) {
        output.printf("%s %d chunks into %s%n", action, result.totalChunks(), config.vectorStorePath());
        output.printf("Reused chunks: %d%n", result.reusedChunks());
        output.printf("Embedded chunks: %d%n", result.embeddedChunks());
        output.printf("Updated files: %d%n", result.updatedFiles());
        output.printf("Deleted files: %d%n", result.deletedFiles());
    }

    private void printMcpTools() {
        if (mcpClient == null) {
            output.println("MCP client is not configured.");
            return;
        }
        try {
            for (McpTool tool : mcpClient.listTools()) {
                output.printf("- %s: %s%n", tool.name(), tool.description());
            }
        } catch (Exception e) {
            output.println("MCP tools failed: " + e.getMessage());
        }
    }

    private void answer(String line) {
        if (agent == null) {
            output.printf("Agent is not configured. Chat model: %s%n", config.chatModel());
            return;
        }
        try {
            AgentAnswer answer = agent.answer(line);
            output.println();
            output.println("Agent:");
            output.println(answer.answer());
            if (!answer.sources().isEmpty()) {
                output.println();
                output.println("Sources:");
                for (String source : answer.sources()) {
                    output.println("- " + source);
                }
            }
        } catch (Exception e) {
            output.println("Agent failed: " + e.getMessage());
        }
    }
}
