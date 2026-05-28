package com.example.agentlearn.prompt;

import com.example.agentlearn.rag.Chunk;
import com.example.agentlearn.skill.AgentSkill;

import java.util.List;

public final class PromptComposer {
    private final String systemPrompt;

    public PromptComposer(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public String compose(List<AgentSkill> skills, List<Chunk> chunks, String userQuestion, String mcpTools, String observation) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("# System\n\n").append(systemPrompt).append("\n\n");

        prompt.append("# Selected Agent Skills\n\n");
        if (skills.isEmpty()) {
            prompt.append("No specific Agent Skill selected.\n\n");
        } else {
            for (AgentSkill skill : skills) {
                prompt.append("## ").append(skill.name()).append("\n\n");
                prompt.append(skill.body()).append("\n\n");
            }
        }

        prompt.append("# Retrieved Knowledge Context\n\n");
        if (chunks.isEmpty()) {
            prompt.append("No relevant chunks were found.\n\n");
        } else {
            for (Chunk chunk : chunks) {
                prompt.append("## ").append(chunk.id()).append("\n\n");
                prompt.append(chunk.text()).append("\n\n");
            }
        }

        if (mcpTools != null && !mcpTools.isBlank()) {
            prompt.append("# Available MCP Tools\n\n").append(mcpTools).append("\n\n");
        }
        if (observation != null && !observation.isBlank()) {
            prompt.append("# Tool Observation\n\n").append(observation).append("\n\n");
        }

        prompt.append("# User Question\n\n").append(userQuestion).append("\n\n");
        prompt.append("# Response Contract\n\n");
        prompt.append("If the answer comes from retrieved knowledge context, answer directly. ");
        prompt.append("You may summarize or synthesize retrieved chunks when that makes the answer clearer. ");
        prompt.append("The JSON answer field must contain only the answer itself. ");
        prompt.append("Do not include sources, citations, source filenames, source labels, or chunk ids in the answer field. ");
        prompt.append("If retrieved knowledge context cannot answer the question, use the model's own general knowledge to answer. ");
        prompt.append("Do not claim that fallback knowledge came from retrieved context. ");
        prompt.append("Do not prefix the answer with phrases like \"According to the knowledge base\", \"Based on retrieved context\", \"根据知识库信息\", or \"根据资料\". ");
        prompt.append("Do not include source filenames or source labels inside the answer text; the CLI displays sources separately. ");
        prompt.append("Return JSON: {\"type\":\"final\",\"answer\":\"...\"}. ");
        prompt.append("If a tool is required, return {\"type\":\"mcp_tool_call\",\"tool\":\"tool_name\",\"arguments\":{...}}.");
        return prompt.toString();
    }
}
