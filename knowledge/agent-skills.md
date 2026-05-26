# Agent Skills

Agent Skill 是可加载的任务指令包，不等同于工具函数。

在这个项目里，Agent Skill 只支持 `SKILL.md`：

```text
skills/
  markdown-qa/
    SKILL.md
```

`SKILL.md` 包含名称、描述和说明。当用户问题与某个 Skill 相关时，Agent 会把它的说明注入 Prompt。

Agent Skill 的作用是影响模型如何完成任务，例如：

- 回答必须引用知识来源。
- 总结笔记时输出结构化摘要。
- 对比概念时先列共同点，再列差异。

它和 MCP 的核心区别是：Agent Skill 是指令，MCP 是外部动作。
