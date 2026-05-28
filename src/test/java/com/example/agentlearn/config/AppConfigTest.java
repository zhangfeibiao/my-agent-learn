package com.example.agentlearn.config;

import com.example.agentlearn.TestSupport;

import java.nio.file.Path;
import java.util.Map;

public final class AppConfigTest {
    public static void main(String[] args) {
        usesDefaultsWhenEnvironmentIsEmpty();
    }

    private static void usesDefaultsWhenEnvironmentIsEmpty() {
        AppConfig config = AppConfig.from(Map.of(), Path.of("."));

        TestSupport.assertEquals(config.knowledgeDir(), Path.of("knowledge"));
        TestSupport.assertEquals(config.skillsDir(), Path.of("skills"));
        TestSupport.assertEquals(config.promptsDir(), Path.of("prompts"));
        TestSupport.assertEquals(config.vectorStorePath(), Path.of("data/vector-store.json"));
        TestSupport.assertEquals(config.llmLogPath(), Path.of("data/llm-call-logs.jsonl"));
        TestSupport.assertEquals(config.chatModel(), "gpt-4.1-mini");
        TestSupport.assertEquals(config.embeddingModel(), "text-embedding-3-small");
    }
}
