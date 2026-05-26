package com.example.agentlearn.agent;

import com.example.agentlearn.llm.ChatClient;
import com.example.agentlearn.llm.ChatMessage;
import com.example.agentlearn.llm.EmbeddingClient;
import com.example.agentlearn.mcp.McpClient;
import com.example.agentlearn.mcp.McpTool;
import com.example.agentlearn.mcp.McpToolCallResult;
import com.example.agentlearn.prompt.PromptComposer;
import com.example.agentlearn.rag.Chunk;
import com.example.agentlearn.rag.Retriever;
import com.example.agentlearn.skill.AgentSkill;
import com.example.agentlearn.skill.SkillSelector;

import java.util.List;

public final class Agent {
    private final ChatClient chatClient;
    private final EmbeddingClient embeddingClient;
    private final Retriever retriever;
    private final SkillSelector skillSelector;
    private final List<AgentSkill> skills;
    private final PromptComposer promptComposer;
    private final int topK;
    private final McpClient mcpClient;

    public Agent(
            ChatClient chatClient,
            EmbeddingClient embeddingClient,
            Retriever retriever,
            SkillSelector skillSelector,
            List<AgentSkill> skills,
            PromptComposer promptComposer,
            int topK
    ) {
        this(chatClient, embeddingClient, retriever, skillSelector, skills, promptComposer, topK, null);
    }

    public Agent(
            ChatClient chatClient,
            EmbeddingClient embeddingClient,
            Retriever retriever,
            SkillSelector skillSelector,
            List<AgentSkill> skills,
            PromptComposer promptComposer,
            int topK,
            McpClient mcpClient
    ) {
        this.chatClient = chatClient;
        this.embeddingClient = embeddingClient;
        this.retriever = retriever;
        this.skillSelector = skillSelector;
        this.skills = List.copyOf(skills);
        this.promptComposer = promptComposer;
        this.topK = topK;
        this.mcpClient = mcpClient;
    }

    public AgentAnswer answer(String userQuestion) throws Exception {
        List<Double> queryEmbedding = embeddingClient.embed(userQuestion);
        List<Chunk> chunks = retriever.retrieve(queryEmbedding, topK);
        List<AgentSkill> selectedSkills = skillSelector.select(userQuestion, skills, true);
        String prompt = promptComposer.compose(selectedSkills, chunks, userQuestion, mcpToolsPrompt(), "");

        String modelOutput = chatClient.complete(List.of(new ChatMessage("user", prompt)));
        ModelDirective directive = ModelDirective.parse(modelOutput);
        if (directive.type() == ModelDirective.Type.MCP_TOOL_CALL) {
            return answerAfterMcpToolCall(userQuestion, selectedSkills, chunks, directive);
        }
        return new AgentAnswer(directive.answer(), sourceIds(chunks));
    }

    private AgentAnswer answerAfterMcpToolCall(
            String userQuestion,
            List<AgentSkill> selectedSkills,
            List<Chunk> chunks,
            ModelDirective directive
    ) throws Exception {
        if (mcpClient == null) {
            return new AgentAnswer("Model requested MCP tool '" + directive.tool() + "', but MCP is not configured.", sourceIds(chunks));
        }

        McpToolCallResult toolResult = mcpClient.callTool(directive.tool(), directive.arguments());
        String observation = "Tool: " + directive.tool() + "\nSuccess: " + toolResult.success() + "\nContent:\n" + toolResult.content();
        String followUpPrompt = promptComposer.compose(selectedSkills, chunks, userQuestion, mcpToolsPrompt(), observation);
        String modelOutput = chatClient.complete(List.of(new ChatMessage("user", followUpPrompt)));
        ModelDirective finalDirective = ModelDirective.parse(modelOutput);
        return new AgentAnswer(finalDirective.answer(), sourceIds(chunks));
    }

    private String mcpToolsPrompt() {
        if (mcpClient == null) {
            return "";
        }
        try {
            StringBuilder builder = new StringBuilder();
            for (McpTool tool : mcpClient.listTools()) {
                builder.append("- ").append(tool.name()).append(": ").append(tool.description()).append('\n');
            }
            return builder.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private List<String> sourceIds(List<Chunk> chunks) {
        return chunks.stream().map(Chunk::id).toList();
    }
}
