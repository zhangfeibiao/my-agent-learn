package com.example.agentlearn.config;

import java.nio.file.Path;
import java.util.Map;

public record AppConfig(
        Path knowledgeDir,
        Path skillsDir,
        Path promptsDir,
        Path vectorStorePath,
        String apiBaseUrl,
        String apiKey,
        String chatModel,
        String embeddingModel,
        boolean debug
) {
    public static AppConfig from(Map<String, String> env, Path baseDir) {
        return new AppConfig(
                path(env, "AGENT_KNOWLEDGE_DIR", "knowledge"),
                path(env, "AGENT_SKILLS_DIR", "skills"),
                path(env, "AGENT_PROMPTS_DIR", "prompts"),
                path(env, "AGENT_VECTOR_STORE", "data/vector-store.json"),
                env.getOrDefault("AGENT_API_BASE_URL", "https://api.openai.com/v1"),
                env.getOrDefault("AGENT_API_KEY", ""),
                env.getOrDefault("AGENT_CHAT_MODEL", "gpt-4.1-mini"),
                env.getOrDefault("AGENT_EMBEDDING_MODEL", "text-embedding-3-small"),
                Boolean.parseBoolean(env.getOrDefault("AGENT_DEBUG", "false"))
        );
    }

    public static AppConfig fromEnvironment() {
        return from(System.getenv(), Path.of("."));
    }

    private static Path path(Map<String, String> env, String key, String defaultValue) {
        return Path.of(env.getOrDefault(key, defaultValue));
    }
}
