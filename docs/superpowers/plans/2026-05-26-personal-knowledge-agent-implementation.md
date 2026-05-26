# Personal Knowledge Agent Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first working Java 21 CLI version of a local Markdown personal knowledge agent with Prompt templates, SKILL.md-only Agent Skills, lightweight RAG, and a minimal MCP demo.

**Architecture:** The implementation keeps each agent concept in a small package: `cli` owns the terminal loop, `agent` coordinates one-turn execution, `prompt` renders Markdown templates, `skill` loads and selects Agent Skills, `rag` handles document indexing and similarity search, `llm` wraps OpenAI-compatible APIs, and `mcp` wraps stdio JSON-RPC tool calls. Tests use fakes around LLM and MCP boundaries so the core can be verified without network calls.

**Tech Stack:** Java 21, Maven, JDK `HttpClient`, Jackson, JUnit 5, AssertJ.

---

## File Structure

- Create `pom.xml`: Maven Java 21 build, dependencies, test plugins, executable jar config.
- Create `src/main/java/com/example/agentlearn/cli/Main.java`: CLI entrypoint.
- Create `src/main/java/com/example/agentlearn/cli/ConsoleSession.java`: REPL loop and slash commands.
- Create `src/main/java/com/example/agentlearn/config/AppConfig.java`: environment and properties configuration.
- Create `src/main/java/com/example/agentlearn/prompt/PromptRepository.java`: load prompt Markdown files.
- Create `src/main/java/com/example/agentlearn/prompt/PromptRenderer.java`: replace `{{variable}}` placeholders.
- Create `src/main/java/com/example/agentlearn/prompt/PromptComposer.java`: compose final chat prompts.
- Create `src/main/java/com/example/agentlearn/skill/AgentSkill.java`: immutable skill model.
- Create `src/main/java/com/example/agentlearn/skill/SkillLoader.java`: parse `skills/*/SKILL.md`.
- Create `src/main/java/com/example/agentlearn/skill/SkillRegistry.java`: load and expose skills.
- Create `src/main/java/com/example/agentlearn/skill/SkillSelector.java`: simple relevance selection.
- Create `src/main/java/com/example/agentlearn/rag/Document.java`: source document model.
- Create `src/main/java/com/example/agentlearn/rag/Chunk.java`: retrieved chunk model.
- Create `src/main/java/com/example/agentlearn/rag/MarkdownDocumentLoader.java`: load `.md` and `.txt`.
- Create `src/main/java/com/example/agentlearn/rag/TextSplitter.java`: split documents into stable chunks.
- Create `src/main/java/com/example/agentlearn/rag/VectorRecord.java`: stored embedding record.
- Create `src/main/java/com/example/agentlearn/rag/JsonVectorStore.java`: persist vectors to JSON.
- Create `src/main/java/com/example/agentlearn/rag/CosineSimilarity.java`: vector ranking primitive.
- Create `src/main/java/com/example/agentlearn/rag/Retriever.java`: top-k retrieval.
- Create `src/main/java/com/example/agentlearn/rag/Indexer.java`: `/index` workflow.
- Create `src/main/java/com/example/agentlearn/llm/ChatClient.java`: chat abstraction.
- Create `src/main/java/com/example/agentlearn/llm/EmbeddingClient.java`: embedding abstraction.
- Create `src/main/java/com/example/agentlearn/llm/OpenAiCompatibleChatClient.java`: HTTP chat implementation.
- Create `src/main/java/com/example/agentlearn/llm/OpenAiCompatibleEmbeddingClient.java`: HTTP embedding implementation.
- Create `src/main/java/com/example/agentlearn/agent/Agent.java`: one-turn orchestration.
- Create `src/main/java/com/example/agentlearn/agent/AgentAnswer.java`: answer plus source ids.
- Create `src/main/java/com/example/agentlearn/agent/ModelDirective.java`: parsed LLM final/tool directive.
- Create `src/main/java/com/example/agentlearn/mcp/McpClient.java`: minimal MCP client contract.
- Create `src/main/java/com/example/agentlearn/mcp/StdioMcpClient.java`: stdio JSON-RPC implementation.
- Create `src/main/java/com/example/agentlearn/mcp/DemoMcpServer.java`: local file-only MCP demo server.
- Create seed files under `knowledge/`, `prompts/`, and `skills/`.
- Create tests under `src/test/java/com/example/agentlearn/`.

---

### Task 1: Maven Skeleton and CLI Shell

**Files:**
- Create: `pom.xml`
- Create: `src/main/java/com/example/agentlearn/cli/Main.java`
- Create: `src/main/java/com/example/agentlearn/cli/ConsoleSession.java`
- Create: `src/main/java/com/example/agentlearn/config/AppConfig.java`

- [ ] **Step 1: Write the initial CLI smoke test**

Create `src/test/java/com/example/agentlearn/config/AppConfigTest.java`:

```java
package com.example.agentlearn.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AppConfigTest {
    @Test
    void usesDefaultsWhenEnvironmentIsEmpty() {
        AppConfig config = AppConfig.from(Map.of(), Path.of("."));

        assertThat(config.knowledgeDir()).isEqualTo(Path.of("knowledge"));
        assertThat(config.skillsDir()).isEqualTo(Path.of("skills"));
        assertThat(config.promptsDir()).isEqualTo(Path.of("prompts"));
        assertThat(config.vectorStorePath()).isEqualTo(Path.of("data/vector-store.json"));
        assertThat(config.chatModel()).isEqualTo("gpt-4.1-mini");
        assertThat(config.embeddingModel()).isEqualTo("text-embedding-3-small");
    }
}
```

- [ ] **Step 2: Run the test and verify it fails**

Run: `mvn test -Dtest=AppConfigTest`

Expected: FAIL because `AppConfig` and the Maven project do not exist yet.

- [ ] **Step 3: Add the Maven build and config class**

Create `pom.xml` with Java 21, Jackson, JUnit 5, AssertJ, Surefire, and Shade plugin.

Create `AppConfig` with:

```java
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
}
```

Create minimal `Main` and `ConsoleSession` so `mvn test` compiles. The CLI should print the banner, support `/exit`, and leave other commands as friendly placeholders until later tasks wire them up.

- [ ] **Step 4: Run the test and verify it passes**

Run: `mvn test -Dtest=AppConfigTest`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add pom.xml src/main/java src/test/java
git commit -m "feat: add Maven CLI skeleton"
```

---

### Task 2: Prompt Templates and Agent Skills

**Files:**
- Create: `src/main/java/com/example/agentlearn/prompt/PromptRepository.java`
- Create: `src/main/java/com/example/agentlearn/prompt/PromptRenderer.java`
- Create: `src/main/java/com/example/agentlearn/skill/AgentSkill.java`
- Create: `src/main/java/com/example/agentlearn/skill/SkillLoader.java`
- Create: `src/main/java/com/example/agentlearn/skill/SkillRegistry.java`
- Create: `src/main/java/com/example/agentlearn/skill/SkillSelector.java`
- Test: `src/test/java/com/example/agentlearn/prompt/PromptRendererTest.java`
- Test: `src/test/java/com/example/agentlearn/skill/SkillLoaderTest.java`
- Test: `src/test/java/com/example/agentlearn/skill/SkillSelectorTest.java`

- [ ] **Step 1: Write failing tests for prompt rendering and SKILL.md parsing**

`PromptRendererTest` should assert that `{{name}}` and `{{context}}` placeholders are replaced and unknown placeholders remain visible.

`SkillLoaderTest` should create a temp `skills/markdown-qa/SKILL.md` with front matter and assert:

```java
assertThat(skill.name()).isEqualTo("markdown-qa");
assertThat(skill.description()).contains("知识库");
assertThat(skill.body()).contains("# Instructions");
```

`SkillSelectorTest` should load three in-memory skills and assert:

```java
assertThat(selected).extracting(AgentSkill::name).contains("markdown-qa");
```

- [ ] **Step 2: Run tests and verify they fail**

Run: `mvn test -Dtest=PromptRendererTest,SkillLoaderTest,SkillSelectorTest`

Expected: FAIL because prompt and skill classes do not exist yet.

- [ ] **Step 3: Implement prompt and skill classes**

Implement:

- `PromptRenderer.render(String template, Map<String, String> variables)`
- `PromptRepository.load(String name)`
- `SkillLoader.loadAll(Path skillsDir)`
- `SkillSelector.select(String userQuery, List<AgentSkill> skills, boolean usesRag)`

The selector should always include `markdown-qa` when `usesRag` is true, then include skills whose name or description tokens appear in the query.

- [ ] **Step 4: Run tests and verify they pass**

Run: `mvn test -Dtest=PromptRendererTest,SkillLoaderTest,SkillSelectorTest`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/example/agentlearn/prompt src/main/java/com/example/agentlearn/skill src/test/java/com/example/agentlearn/prompt src/test/java/com/example/agentlearn/skill
git commit -m "feat: load prompt templates and agent skills"
```

---

### Task 3: Lightweight RAG Core

**Files:**
- Create: `src/main/java/com/example/agentlearn/rag/Document.java`
- Create: `src/main/java/com/example/agentlearn/rag/Chunk.java`
- Create: `src/main/java/com/example/agentlearn/rag/MarkdownDocumentLoader.java`
- Create: `src/main/java/com/example/agentlearn/rag/TextSplitter.java`
- Create: `src/main/java/com/example/agentlearn/rag/VectorRecord.java`
- Create: `src/main/java/com/example/agentlearn/rag/JsonVectorStore.java`
- Create: `src/main/java/com/example/agentlearn/rag/CosineSimilarity.java`
- Create: `src/main/java/com/example/agentlearn/rag/Retriever.java`
- Test: `src/test/java/com/example/agentlearn/rag/TextSplitterTest.java`
- Test: `src/test/java/com/example/agentlearn/rag/JsonVectorStoreTest.java`
- Test: `src/test/java/com/example/agentlearn/rag/CosineSimilarityTest.java`

- [ ] **Step 1: Write failing RAG tests**

Write tests for:

- Loading only `.md` and `.txt`.
- Splitting a document into chunk ids like `knowledge/mcp.md#chunk-1`.
- Saving and loading JSON vector records.
- Ranking `[1, 0]` above `[0, 1]` for query `[1, 0]`.

- [ ] **Step 2: Run tests and verify they fail**

Run: `mvn test -Dtest=TextSplitterTest,JsonVectorStoreTest,CosineSimilarityTest`

Expected: FAIL because RAG classes do not exist yet.

- [ ] **Step 3: Implement RAG classes**

Implement document loading with `Files.walk`, chunking by paragraph with a max character budget, JSON persistence with Jackson, and cosine similarity with zero-vector protection.

Use immutable records:

```java
public record Document(Path source, String text) {}
public record Chunk(String id, Path source, String text) {}
public record VectorRecord(String id, Path source, String text, List<Double> embedding) {}
```

- [ ] **Step 4: Run tests and verify they pass**

Run: `mvn test -Dtest=TextSplitterTest,JsonVectorStoreTest,CosineSimilarityTest`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/example/agentlearn/rag src/test/java/com/example/agentlearn/rag
git commit -m "feat: add lightweight RAG primitives"
```

---

### Task 4: LLM Clients and Indexing Workflow

**Files:**
- Create: `src/main/java/com/example/agentlearn/llm/ChatClient.java`
- Create: `src/main/java/com/example/agentlearn/llm/EmbeddingClient.java`
- Create: `src/main/java/com/example/agentlearn/llm/OpenAiCompatibleChatClient.java`
- Create: `src/main/java/com/example/agentlearn/llm/OpenAiCompatibleEmbeddingClient.java`
- Create: `src/main/java/com/example/agentlearn/rag/Indexer.java`
- Modify: `src/main/java/com/example/agentlearn/cli/ConsoleSession.java`
- Test: `src/test/java/com/example/agentlearn/rag/IndexerTest.java`

- [ ] **Step 1: Write failing indexer test with fake embeddings**

The test should create a temp `knowledge/agent.md`, use a fake `EmbeddingClient` that maps text length to a deterministic vector, run `Indexer.rebuild()`, and assert the JSON vector store contains records.

- [ ] **Step 2: Run the test and verify it fails**

Run: `mvn test -Dtest=IndexerTest`

Expected: FAIL because LLM interfaces and `Indexer` do not exist yet.

- [ ] **Step 3: Implement LLM interfaces, HTTP clients, and indexer**

Implement HTTP clients against:

- `POST /chat/completions`
- `POST /embeddings`

Throw `IllegalStateException` if live clients are used without `AGENT_API_KEY`.

Wire `/index` in `ConsoleSession` to rebuild the vector store.

- [ ] **Step 4: Run the test and verify it passes**

Run: `mvn test -Dtest=IndexerTest`

Expected: PASS without network calls.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/example/agentlearn/llm src/main/java/com/example/agentlearn/rag/Indexer.java src/main/java/com/example/agentlearn/cli/ConsoleSession.java src/test/java/com/example/agentlearn/rag/IndexerTest.java
git commit -m "feat: add indexing workflow"
```

---

### Task 5: Agent Loop and QA Flow

**Files:**
- Create: `src/main/java/com/example/agentlearn/agent/Agent.java`
- Create: `src/main/java/com/example/agentlearn/agent/AgentAnswer.java`
- Create: `src/main/java/com/example/agentlearn/agent/ModelDirective.java`
- Create: `src/main/java/com/example/agentlearn/prompt/PromptComposer.java`
- Modify: `src/main/java/com/example/agentlearn/cli/ConsoleSession.java`
- Test: `src/test/java/com/example/agentlearn/agent/AgentTest.java`

- [ ] **Step 1: Write failing agent flow tests**

Use fake `ChatClient`, fake `Retriever`, and in-memory skills to assert:

- The agent includes selected skill instructions.
- The agent includes RAG context.
- Sources are returned with the answer.
- Invalid JSON model output is treated as final text.

- [ ] **Step 2: Run tests and verify they fail**

Run: `mvn test -Dtest=AgentTest`

Expected: FAIL because agent classes do not exist yet.

- [ ] **Step 3: Implement agent loop**

Implement one-turn flow:

```text
select skills -> retrieve chunks -> compose prompt -> chat -> parse final answer -> return answer and sources
```

Keep MCP optional in this task. Add the extension point but no stdio process yet.

- [ ] **Step 4: Run tests and verify they pass**

Run: `mvn test -Dtest=AgentTest`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/example/agentlearn/agent src/main/java/com/example/agentlearn/prompt/PromptComposer.java src/main/java/com/example/agentlearn/cli/ConsoleSession.java src/test/java/com/example/agentlearn/agent
git commit -m "feat: add RAG question answering loop"
```

---

### Task 6: Minimal MCP Demo

**Files:**
- Create: `src/main/java/com/example/agentlearn/mcp/McpClient.java`
- Create: `src/main/java/com/example/agentlearn/mcp/McpTool.java`
- Create: `src/main/java/com/example/agentlearn/mcp/McpToolCallResult.java`
- Create: `src/main/java/com/example/agentlearn/mcp/StdioMcpClient.java`
- Create: `src/main/java/com/example/agentlearn/mcp/DemoMcpServer.java`
- Modify: `src/main/java/com/example/agentlearn/agent/Agent.java`
- Modify: `src/main/java/com/example/agentlearn/cli/ConsoleSession.java`
- Test: `src/test/java/com/example/agentlearn/mcp/McpMessageTest.java`

- [ ] **Step 1: Write failing MCP message tests**

Assert JSON-RPC request/response serialization for `tools/list` and `tools/call`.

- [ ] **Step 2: Run tests and verify they fail**

Run: `mvn test -Dtest=McpMessageTest`

Expected: FAIL because MCP classes do not exist yet.

- [ ] **Step 3: Implement MCP client/server demo**

Implement:

- `McpClient.listTools()`
- `McpClient.callTool(String name, Map<String, Object> arguments)`
- `DemoMcpServer` tools: `list_knowledge_files`, `read_knowledge_file`
- Path validation that rejects traversal outside `knowledge/`
- `/mcp-tools` command

Allow one MCP call round when `ModelDirective` is `mcp_tool_call`.

- [ ] **Step 4: Run tests and verify they pass**

Run: `mvn test -Dtest=McpMessageTest`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/example/agentlearn/mcp src/main/java/com/example/agentlearn/agent src/main/java/com/example/agentlearn/cli src/test/java/com/example/agentlearn/mcp
git commit -m "feat: add minimal MCP demo"
```

---

### Task 7: Seed Content and Documentation

**Files:**
- Create: `knowledge/ai-agent.md`
- Create: `knowledge/rag.md`
- Create: `knowledge/mcp.md`
- Create: `knowledge/agent-skills.md`
- Create: `skills/markdown-qa/SKILL.md`
- Create: `skills/summarize-notes/SKILL.md`
- Create: `skills/compare-notes/SKILL.md`
- Create: `prompts/system.md`
- Create: `prompts/rag-answer.md`
- Create: `prompts/mcp-tool-use.md`
- Create: `.env.example`
- Create: `README.md`

- [ ] **Step 1: Add seed files**

Seed files should explain AI Agent, RAG, MCP, and Agent Skills in short Markdown notes. Skill files should have front matter and clear instructions. Prompt templates should make the final prompt inspectable and require source-aware answers.

- [ ] **Step 2: Run all tests**

Run: `mvn test`

Expected: PASS.

- [ ] **Step 3: Run CLI help smoke check**

Run: `mvn -q -DskipTests package`

Expected: PASS and jar created under `target/`.

- [ ] **Step 4: Commit**

```bash
git add knowledge skills prompts .env.example README.md
git commit -m "docs: add seed knowledge and usage guide"
```

---

## Self-Review

Spec coverage:

- CLI: covered by Tasks 1, 4, 5, and 6.
- Prompt templates: covered by Tasks 2, 5, and 7.
- SKILL.md-only Agent Skills: covered by Tasks 2 and 7.
- RAG: covered by Tasks 3 and 4.
- OpenAI-compatible LLM and embeddings: covered by Task 4.
- Minimal MCP client and demo tools: covered by Task 6.
- Tests: covered in every implementation task.
- Docs and learning notes: covered by Task 7.

Placeholder scan:

- No unresolved placeholders or intentionally vague implementation tasks remain.

Type consistency:

- `AgentSkill`, `Chunk`, `VectorRecord`, `ChatClient`, `EmbeddingClient`, `McpClient`, and `ModelDirective` are named consistently across tasks.
