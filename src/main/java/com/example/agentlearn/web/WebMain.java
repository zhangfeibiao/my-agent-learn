package com.example.agentlearn.web;

import com.example.agentlearn.config.AppConfig;
import com.example.agentlearn.llm.logging.LlmCallContext;
import com.example.agentlearn.mcp.McpClient;
import com.example.agentlearn.rag.IndexResult;
import com.example.agentlearn.runtime.AgentRuntime;
import com.example.agentlearn.runtime.AgentRuntimeFactory;

import java.io.Closeable;
import java.util.List;

public final class WebMain {
    private WebMain() {
    }

    public static void main(String[] args) throws Exception {
        AppConfig config = AppConfig.fromEnvironment();
        AgentRuntime runtime = AgentRuntimeFactory.create(config);
        int port = resolvePort(args);
        WebServer server = new WebServer(
                port,
                config,
                new ManagedFileService(config.knowledgeDir(), List.of(".md", ".txt")),
                new ManagedFileService(config.promptsDir(), List.of(".md")),
                new SkillFileService(config.skillsDir()),
                message -> LlmCallContext.withNewSession(() -> runtime.createAgent().answer(message)),
                new IndexService() {
                    @Override
                    public IndexResult update() throws Exception {
                        return LlmCallContext.withNewSession(() -> runtime.indexer().rebuildIncremental(config.knowledgeDir()));
                    }

                    @Override
                    public IndexResult rebuild() throws Exception {
                        return LlmCallContext.withNewSession(() -> runtime.indexer().rebuildFull(config.knowledgeDir()));
                    }
                },
                runtime.llmCallLogStore()
        );

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.stop();
            closeMcp(runtime.mcpClient());
        }));
        server.start();
        System.out.println("Personal Knowledge Agent UI: " + server.baseUri());
        Thread.currentThread().join();
    }

    private static int resolvePort(String[] args) {
        if (args.length > 0 && !args[0].isBlank()) {
            return Integer.parseInt(args[0]);
        }
        return Integer.parseInt(System.getenv().getOrDefault("AGENT_WEB_PORT", "8080"));
    }

    private static void closeMcp(McpClient mcpClient) {
        if (mcpClient instanceof Closeable closeable) {
            try {
                closeable.close();
            } catch (Exception ignored) {
                // Shutdown hooks cannot report useful recovery actions.
            }
        }
    }
}
