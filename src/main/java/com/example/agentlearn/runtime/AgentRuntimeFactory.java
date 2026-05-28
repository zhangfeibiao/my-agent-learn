package com.example.agentlearn.runtime;

import com.example.agentlearn.agent.Agent;
import com.example.agentlearn.config.AppConfig;
import com.example.agentlearn.llm.EmbeddingClient;
import com.example.agentlearn.llm.OpenAiCompatibleChatClient;
import com.example.agentlearn.llm.OpenAiCompatibleEmbeddingClient;
import com.example.agentlearn.llm.logging.JsonlLlmCallLogStore;
import com.example.agentlearn.llm.logging.LlmCallLogStore;
import com.example.agentlearn.llm.logging.LoggingChatClient;
import com.example.agentlearn.llm.logging.LoggingEmbeddingClient;
import com.example.agentlearn.mcp.McpClient;
import com.example.agentlearn.mcp.StdioMcpClient;
import com.example.agentlearn.prompt.PromptComposer;
import com.example.agentlearn.rag.Indexer;
import com.example.agentlearn.rag.JsonVectorStore;
import com.example.agentlearn.rag.MarkdownDocumentLoader;
import com.example.agentlearn.rag.Retriever;
import com.example.agentlearn.rag.TextSplitter;
import com.example.agentlearn.skill.AgentSkill;
import com.example.agentlearn.skill.SkillLoader;
import com.example.agentlearn.skill.SkillSelector;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public final class AgentRuntimeFactory {
    private AgentRuntimeFactory() {
    }

    public static AgentRuntime create(AppConfig config) {
        LlmCallLogStore logStore = new JsonlLlmCallLogStore(config.llmLogPath());
        EmbeddingClient embeddingClient = new LoggingEmbeddingClient(
                new OpenAiCompatibleEmbeddingClient(
                        config.apiBaseUrl(),
                        config.apiKey(),
                        config.embeddingModel()
                ),
                logStore,
                config.embeddingModel(),
                "/embeddings"
        );
        JsonVectorStore vectorStore = new JsonVectorStore(config.vectorStorePath());
        Indexer indexer = new Indexer(
                new MarkdownDocumentLoader(),
                new TextSplitter(1200),
                embeddingClient,
                vectorStore,
                config.embeddingModel(),
                Indexer.DEFAULT_SPLITTER_VERSION
        );
        return new AgentRuntime(config, embeddingClient, vectorStore, indexer, createMcpClient(config), logStore);
    }

    public static Agent createAgent(
            AppConfig config,
            EmbeddingClient embeddingClient,
            JsonVectorStore vectorStore,
            McpClient mcpClient,
            LlmCallLogStore logStore
    ) {
        return new Agent(
                new LoggingChatClient(
                        new OpenAiCompatibleChatClient(config.apiBaseUrl(), config.apiKey(), config.chatModel()),
                        logStore,
                        config.chatModel(),
                        "/chat/completions"
                ),
                embeddingClient,
                new Retriever(vectorStore),
                new SkillSelector(),
                loadSkills(config),
                new PromptComposer(loadSystemPrompt(config)),
                4,
                mcpClient
        );
    }

    private static McpClient createMcpClient(AppConfig config) {
        try {
            String java = System.getProperty("java.home") + "/bin/java";
            String classpath = System.getProperty("java.class.path");
            List<String> command = new ArrayList<>();
            command.add(java);
            command.add("-cp");
            command.add(classpath);
            command.add("com.example.agentlearn.mcp.DemoMcpServer");
            command.add(config.knowledgeDir().toString());
            return new StdioMcpClient(command);
        } catch (Exception e) {
            return null;
        }
    }

    private static List<AgentSkill> loadSkills(AppConfig config) {
        try {
            return new SkillLoader().loadAll(config.skillsDir());
        } catch (Exception e) {
            return List.of();
        }
    }

    private static String loadSystemPrompt(AppConfig config) {
        try {
            return Files.readString(config.promptsDir().resolve("system.md"), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "You are a local Markdown knowledge base assistant. Answer using retrieved context and cite sources.";
        }
    }
}
