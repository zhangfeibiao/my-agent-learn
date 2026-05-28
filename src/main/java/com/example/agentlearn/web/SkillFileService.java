package com.example.agentlearn.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class SkillFileService {
    private static final String SKILL_FILE = "SKILL.md";
    private final Path root;

    public SkillFileService(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    public List<SkillFile> list() throws IOException {
        if (!Files.isDirectory(root)) {
            return List.of();
        }

        List<SkillFile> skills = new ArrayList<>();
        try (var stream = Files.list(root)) {
            for (Path skillDir : stream.sorted(Comparator.comparing(Path::toString)).toList()) {
                Path skillFile = skillDir.resolve(SKILL_FILE);
                if (Files.isRegularFile(skillFile)) {
                    skills.add(new SkillFile(skillDir.getFileName().toString(), Files.size(skillFile)));
                }
            }
        }
        return List.copyOf(skills);
    }

    public String read(String name) throws IOException {
        return Files.readString(resolveSkillFile(name), StandardCharsets.UTF_8);
    }

    public void write(String name, String content) throws IOException {
        Path skillFile = resolveSkillFile(name);
        Files.createDirectories(skillFile.getParent());
        Files.writeString(skillFile, content, StandardCharsets.UTF_8);
    }

    public void delete(String name) throws IOException {
        Files.deleteIfExists(resolveSkillFile(name));
    }

    private Path resolveSkillFile(String name) {
        if (name == null || name.isBlank() || !name.matches("[A-Za-z0-9._-]+")) {
            throw new IllegalArgumentException("Invalid skill name");
        }
        Path resolved = root.resolve(name).resolve(SKILL_FILE).normalize().toAbsolutePath();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("Path escapes skills directory");
        }
        return resolved;
    }
}
