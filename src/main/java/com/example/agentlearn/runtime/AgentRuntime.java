package com.example.agentlearn.runtime;

import com.example.agentlearn.agent.Agent;
import com.example.agentlearn.config.AppConfig;
import com.example.agentlearn.llm.EmbeddingClient;
import com.example.agentlearn.llm.logging.LlmCallLogStore;
import com.example.agentlearn.mcp.McpClient;
import com.example.agentlearn.rag.Indexer;
import com.example.agentlearn.rag.JsonVectorStore;

public record AgentRuntime(
        AppConfig config,
        EmbeddingClient embeddingClient,
        JsonVectorStore vectorStore,
        Indexer indexer,
        McpClient mcpClient,
        LlmCallLogStore llmCallLogStore
) {
    public Agent createAgent() {
        return AgentRuntimeFactory.createAgent(config, embeddingClient, vectorStore, mcpClient, llmCallLogStore);
    }
}
