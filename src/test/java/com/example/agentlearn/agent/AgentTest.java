package com.example.agentlearn.agent;

import com.example.agentlearn.TestSupport;
import com.example.agentlearn.llm.ChatClient;
import com.example.agentlearn.llm.ChatMessage;
import com.example.agentlearn.llm.EmbeddingClient;
import com.example.agentlearn.prompt.PromptComposer;
import com.example.agentlearn.rag.JsonVectorStore;
import com.example.agentlearn.rag.Retriever;
import com.example.agentlearn.rag.VectorRecord;
import com.example.agentlearn.skill.AgentSkill;
import com.example.agentlearn.skill.SkillSelector;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class AgentTest {
    public static void main(String[] args) throws Exception {
        includesSelectedSkillsAndRagContextInPrompt();
        treatsInvalidJsonModelOutputAsFinalAnswer();
    }

    private static void includesSelectedSkillsAndRagContextInPrompt() throws Exception {
        Path vectorFile = Files.createTempDirectory("agent").resolve("vector-store.json");
        JsonVectorStore store = new JsonVectorStore(vectorFile);
        store.save(List.of(new VectorRecord(
                "knowledge/mcp.md#chunk-1",
                Path.of("knowledge/mcp.md"),
                "MCP lets agents call external tools.",
                List.of(1.0, 0.0)
        )));

        CapturingChatClient chat = new CapturingChatClient("{\"type\":\"final\",\"answer\":\"MCP answer\"}");
        Agent agent = new Agent(
                chat,
                text -> List.of(1.0, 0.0),
                new Retriever(store),
                new SkillSelector(),
                List.of(new AgentSkill("markdown-qa", "当用户询问知识库内容时使用", "qa body")),
                new PromptComposer("You are a local knowledge agent."),
                3
        );

        AgentAnswer answer = agent.answer("什么是 MCP?");

        TestSupport.assertEquals(answer.answer(), "MCP answer");
        TestSupport.assertContains(answer.sources(), "knowledge/mcp.md#chunk-1");
        TestSupport.assertContains(chat.lastPrompt(), "qa body");
        TestSupport.assertContains(chat.lastPrompt(), "MCP lets agents call external tools.");
    }

    private static void treatsInvalidJsonModelOutputAsFinalAnswer() throws Exception {
        Path vectorFile = Files.createTempDirectory("agent").resolve("vector-store.json");
        JsonVectorStore store = new JsonVectorStore(vectorFile);
        store.save(List.of());

        Agent agent = new Agent(
                messages -> "plain answer",
                text -> List.of(0.0),
                new Retriever(store),
                new SkillSelector(),
                List.of(),
                new PromptComposer("system"),
                3
        );

        AgentAnswer answer = agent.answer("hello");

        TestSupport.assertEquals(answer.answer(), "plain answer");
        TestSupport.assertEquals(answer.sources().size(), 0);
    }

    private static final class CapturingChatClient implements ChatClient {
        private final String response;
        private final List<ChatMessage> messages = new ArrayList<>();

        private CapturingChatClient(String response) {
            this.response = response;
        }

        @Override
        public String complete(List<ChatMessage> messages) {
            this.messages.clear();
            this.messages.addAll(messages);
            return response;
        }

        private String lastPrompt() {
            return messages.get(messages.size() - 1).content();
        }
    }
}
