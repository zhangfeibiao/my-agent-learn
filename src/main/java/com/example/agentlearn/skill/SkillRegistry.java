package com.example.agentlearn.skill;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public final class SkillRegistry {
    private final SkillLoader loader;
    private final Path skillsDir;

    public SkillRegistry(SkillLoader loader, Path skillsDir) {
        this.loader = loader;
        this.skillsDir = skillsDir;
    }

    public List<AgentSkill> loadAll() throws IOException {
        return loader.loadAll(skillsDir);
    }
}
