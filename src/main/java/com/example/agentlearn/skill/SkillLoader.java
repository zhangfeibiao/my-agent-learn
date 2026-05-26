package com.example.agentlearn.skill;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public final class SkillLoader {
    public List<AgentSkill> loadAll(Path skillsDir) throws IOException {
        if (!Files.isDirectory(skillsDir)) {
            return List.of();
        }

        List<AgentSkill> skills = new ArrayList<>();
        try (Stream<Path> paths = Files.list(skillsDir)) {
            for (Path skillDir : paths.sorted(Comparator.comparing(Path::toString)).toList()) {
                Path skillFile = skillDir.resolve("SKILL.md");
                if (Files.isRegularFile(skillFile)) {
                    parse(skillFile).ifPresent(skills::add);
                }
            }
        }
        return List.copyOf(skills);
    }

    private Optional<AgentSkill> parse(Path skillFile) throws IOException {
        String content = Files.readString(skillFile, StandardCharsets.UTF_8);
        if (!content.startsWith("---")) {
            return Optional.empty();
        }

        int frontMatterEnd = content.indexOf("\n---", 3);
        if (frontMatterEnd < 0) {
            return Optional.empty();
        }

        String frontMatter = content.substring(3, frontMatterEnd).trim();
        String body = content.substring(frontMatterEnd + 4).stripLeading();
        String name = "";
        String description = "";

        for (String line : frontMatter.split("\\R")) {
            int separator = line.indexOf(':');
            if (separator < 0) {
                continue;
            }
            String key = line.substring(0, separator).trim();
            String value = line.substring(separator + 1).trim();
            if ("name".equals(key)) {
                name = value;
            } else if ("description".equals(key)) {
                description = value;
            }
        }

        if (name.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new AgentSkill(name, description, body));
    }
}
