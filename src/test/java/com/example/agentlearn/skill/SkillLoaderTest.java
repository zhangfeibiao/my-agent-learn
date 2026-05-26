package com.example.agentlearn.skill;

import com.example.agentlearn.TestSupport;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class SkillLoaderTest {
    public static void main(String[] args) throws Exception {
        parsesSkillFrontMatterAndBody();
    }

    private static void parsesSkillFrontMatterAndBody() throws Exception {
        Path root = Files.createTempDirectory("skills");
        Path skillDir = Files.createDirectories(root.resolve("markdown-qa"));
        Files.writeString(skillDir.resolve("SKILL.md"), """
                ---
                name: markdown-qa
                description: 当用户询问知识库内容时使用
                ---

                # Instructions

                - 优先基于知识库回答。
                """);

        List<AgentSkill> skills = new SkillLoader().loadAll(root);

        TestSupport.assertEquals(skills.size(), 1);
        AgentSkill skill = skills.get(0);
        TestSupport.assertEquals(skill.name(), "markdown-qa");
        TestSupport.assertContains(skill.description(), "知识库");
        TestSupport.assertContains(skill.body(), "# Instructions");
    }
}
