package com.example.agentlearn.agent;

import com.example.agentlearn.llm.ChatClient;
import com.example.agentlearn.llm.ChatMessage;
import com.example.agentlearn.llm.EmbeddingClient;
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

    public Agent(
            ChatClient chatClient,
            EmbeddingClient embeddingClient,
            Retriever retriever,
            SkillSelector skillSelector,
            List<AgentSkill> skills,
            PromptComposer promptComposer,
            int topK
    ) {
        this.chatClient = chatClient;
        this.embeddingClient = embeddingClient;
        this.retriever = retriever;
        this.skillSelector = skillSelector;
        this.skills = List.copyOf(skills);
        this.promptComposer = promptComposer;
        this.topK = topK;
    }

    public AgentAnswer answer(String userQuestion) throws Exception {
        List<Double> queryEmbedding = embeddingClient.embed(userQuestion);
        List<Chunk> chunks = retriever.retrieve(queryEmbedding, topK);
        List<AgentSkill> selectedSkills = skillSelector.select(userQuestion, skills, true);
        String prompt = promptComposer.compose(selectedSkills, chunks, userQuestion, "", "");

        String modelOutput = chatClient.complete(List.of(new ChatMessage("user", prompt)));
        ModelDirective directive = ModelDirective.parse(modelOutput);
        if (directive.type() == ModelDirective.Type.MCP_TOOL_CALL) {
            return new AgentAnswer("Model requested MCP tool '" + directive.tool() + "', but MCP is not wired yet.", sourceIds(chunks));
        }
        return new AgentAnswer(directive.answer(), sourceIds(chunks));
    }

    private List<String> sourceIds(List<Chunk> chunks) {
        return chunks.stream().map(Chunk::id).toList();
    }
}
