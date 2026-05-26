package com.example.agentlearn.skill;

import com.example.agentlearn.TestSupport;

import java.util.List;

public final class SkillSelectorTest {
    public static void main(String[] args) {
        alwaysSelectsMarkdownQaForRagQuestions();
        selectsRelevantSkillsByDescription();
    }

    private static void alwaysSelectsMarkdownQaForRagQuestions() {
        List<AgentSkill> selected = new SkillSelector().select("什么是 MCP", sampleSkills(), true);

        TestSupport.assertContains(selected.stream().map(AgentSkill::name).toList(), "markdown-qa");
    }

    private static void selectsRelevantSkillsByDescription() {
        List<AgentSkill> selected = new SkillSelector().select("对比 RAG 和 MCP", sampleSkills(), true);

        TestSupport.assertContains(selected.stream().map(AgentSkill::name).toList(), "compare-notes");
    }

    private static List<AgentSkill> sampleSkills() {
        return List.of(
                new AgentSkill("markdown-qa", "当用户询问知识库内容时使用", "qa body"),
                new AgentSkill("summarize-notes", "总结 Markdown 笔记内容", "summary body"),
                new AgentSkill("compare-notes", "对比多个知识片段或概念", "compare body")
        );
    }
}
