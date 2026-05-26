package com.example.agentlearn.prompt;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class PromptRepository {
    private final Path promptsDir;

    public PromptRepository(Path promptsDir) {
        this.promptsDir = promptsDir;
    }

    public String load(String name) throws IOException {
        Path promptPath = promptsDir.resolve(name);
        if (!promptPath.getFileName().toString().endsWith(".md")) {
            promptPath = promptsDir.resolve(name + ".md");
        }
        return Files.readString(promptPath, StandardCharsets.UTF_8);
    }
}
