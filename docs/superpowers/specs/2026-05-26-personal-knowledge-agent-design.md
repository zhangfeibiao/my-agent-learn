# Personal Knowledge Agent Design

## 1. Goal

Build a small, framework-free Java CLI agent that helps a learner understand the implementation principles behind AI agents.

The first version focuses on a local Markdown knowledge base assistant. It should answer questions from local `.md` and `.txt` files, demonstrate lightweight RAG, load Agent Skills from `SKILL.md` instruction packs, render prompts from templates, and call a minimal MCP tool server through stdio JSON-RPC.

The project is intentionally educational. The code should expose the moving parts clearly instead of hiding them behind large frameworks.

## 2. Scope

### In Scope

- Java 21 CLI application.
- Maven project structure.
- OpenAI-compatible chat API client using JDK `HttpClient`.
- OpenAI-compatible embedding API client using JDK `HttpClient`.
- Local Markdown and text document loading from `knowledge/`.
- Lightweight chunking and retrieval.
- Local vector store backed by a JSON file.
- Prompt templates stored as Markdown files under `prompts/`.
- Agent Skills loaded from `skills/*/SKILL.md`.
- Minimal skill selection based on skill description and user query.
- Minimal MCP client over stdio JSON-RPC.
- One tiny local MCP server for file-oriented demo tools.
- Tests for core parsing, retrieval, prompt rendering, skill loading, and MCP message handling.

### Out of Scope

- Web UI.
- Spring, LangChain4j, or other agent frameworks.
- Heavy vector databases.
- Multi-agent collaboration.
- Complex planning engines.
- Long-term memory compaction.
- Authentication and permission systems.
- Full MCP protocol coverage.
- Agent Skills with scripts, resources, or executable actions.

## 3. User Experience

The user starts the CLI and asks questions in a loop:

```text
$ java -jar personal-knowledge-agent.jar

Personal Knowledge Agent
Type /index to rebuild the knowledge index, /exit to quit.

> 什么是 MCP？它和 Agent Skill 有什么区别？

Agent:
MCP 是一种外部工具协议，Agent Skill 是一种可加载的任务指令包...

Sources:
- knowledge/mcp.md#chunk-2
- knowledge/agent-skills.md#chunk-1
```

Expected commands:

- `/index`: load documents, split them, call embeddings, and save `data/vector-store.json`.
- `/skills`: print loaded Agent Skills.
- `/mcp-tools`: print tools discovered from the demo MCP server.
- `/exit`: quit.

For normal questions, the agent should:

1. Select relevant Agent Skills.
2. Retrieve relevant knowledge chunks.
3. Render a prompt from templates.
4. Ask the LLM for either a final answer or an MCP tool call.
5. Execute MCP calls when requested.
6. Ask the LLM for the final answer after tool observations.

## 4. Architecture

The project is split into small modules so each agent concept stays visible.

```text
src/main/java/com/example/agentlearn/
  cli/
  agent/
  llm/
  prompt/
  rag/
  skill/
  mcp/
  config/
  util/

knowledge/
prompts/
skills/
data/
```

### CLI

Responsibilities:

- Read user input.
- Handle slash commands.
- Print answers and diagnostics.
- Keep the interaction loop simple.

Main classes:

- `Main`
- `ConsoleSession`
- `CommandRouter`

### Agent Core

Responsibilities:

- Coordinate the agent loop.
- Own conversation state for one CLI session.
- Decide when RAG, skills, and MCP are used.
- Convert LLM responses into final answers or tool calls.

Main classes:

- `Agent`
- `AgentLoop`
- `AgentContext`
- `Message`
- `AgentResponse`
- `ToolCall`
- `Observation`

### LLM

Responsibilities:

- Send chat requests to an OpenAI-compatible API.
- Send embedding requests to an OpenAI-compatible API.
- Keep model provider details outside the agent loop.

Main classes:

- `ChatClient`
- `EmbeddingClient`
- `OpenAiCompatibleChatClient`
- `OpenAiCompatibleEmbeddingClient`
- `ChatRequest`
- `ChatResult`
- `EmbeddingResult`

Configuration should come from environment variables or a simple properties file:

- `AGENT_API_BASE_URL`
- `AGENT_API_KEY`
- `AGENT_CHAT_MODEL`
- `AGENT_EMBEDDING_MODEL`

### RAG

Responsibilities:

- Load local Markdown and text documents.
- Split documents into chunks.
- Generate and persist embeddings.
- Retrieve top-k chunks by cosine similarity.

Main classes:

- `DocumentLoader`
- `MarkdownDocumentLoader`
- `TextSplitter`
- `Chunk`
- `VectorRecord`
- `VectorStore`
- `JsonVectorStore`
- `Retriever`
- `CosineSimilarity`

The first version uses a JSON vector store:

```json
[
  {
    "id": "knowledge/mcp.md#chunk-1",
    "source": "knowledge/mcp.md",
    "text": "MCP ...",
    "embedding": [0.012, -0.031]
  }
]
```

Retrieval can use brute-force cosine similarity. This is slow for large corpora but perfect for understanding the principle.

### Prompt

Responsibilities:

- Load Markdown prompt templates.
- Render templates with variables.
- Compose system prompt, selected skill instructions, RAG context, MCP tool descriptions, conversation history, and user input.

Main classes:

- `PromptTemplate`
- `PromptRepository`
- `PromptRenderer`
- `PromptComposer`

Initial templates:

```text
prompts/
  system.md
  rag-answer.md
  mcp-tool-use.md
```

The prompt composer should make the final prompt inspectable in debug mode. This helps the learner see what the agent really sends to the model.

### Agent Skills

Agent Skills are not Java tools in this project. They are instruction packs loaded from `SKILL.md` files and injected into the prompt when relevant.

Supported v1 layout:

```text
skills/
  markdown-qa/
    SKILL.md
  summarize-notes/
    SKILL.md
  compare-notes/
    SKILL.md
```

Supported `SKILL.md` shape:

```markdown
---
name: markdown-qa
description: 当用户询问知识库内容时使用
---

# Instructions

- 优先基于知识库检索结果回答。
- 回答中引用来源文件名。
- 如果资料不足，不要编造。
```

Responsibilities:

- Scan `skills/*/SKILL.md`.
- Parse front matter fields.
- Store skill name, description, and body.
- Select relevant skills for each user query.
- Inject selected skill bodies into the prompt.

Main classes:

- `AgentSkill`
- `SkillLoader`
- `SkillRegistry`
- `SkillSelector`
- `SkillPromptComposer`

Selection can be deliberately simple in v1:

- Keyword score between user query and skill name/description.
- Always include `markdown-qa` when a normal question uses RAG.
- Later versions can replace this with embedding-based skill selection.

### MCP

MCP is treated as an external tool protocol, separate from Agent Skills.

The first version implements only the parts needed to understand the flow:

- Start a local MCP server process.
- Send JSON-RPC messages through stdin.
- Read JSON-RPC responses from stdout.
- Call `tools/list`.
- Call `tools/call`.

Main classes:

- `McpClient`
- `McpProcess`
- `McpMessage`
- `McpTool`
- `McpToolRegistry`
- `McpToolCallResult`

Demo MCP tools:

- `list_knowledge_files`: list files under `knowledge/`.
- `read_knowledge_file`: read a named file under `knowledge/`.

Security constraints:

- The demo MCP server may only access the configured `knowledge/` directory.
- Path traversal must be rejected.
- MCP errors should be returned as observations, not crash the CLI.

## 5. Agent Loop

The v1 loop is intentionally direct:

```text
receive user input
  -> classify slash command or normal question
  -> load/select Agent Skills
  -> retrieve RAG chunks
  -> discover MCP tools
  -> compose prompt
  -> call chat model
  -> if model returns MCP tool call:
       execute tool
       compose follow-up prompt with observation
       call chat model again
  -> print final answer and sources
```

The LLM response format should be simple JSON when tool use is possible:

```json
{
  "type": "final",
  "answer": "..."
}
```

or:

```json
{
  "type": "mcp_tool_call",
  "tool": "list_knowledge_files",
  "arguments": {}
}
```

If parsing fails, the raw text can be treated as a final answer. This keeps the first version forgiving.

## 6. Error Handling

The CLI should stay alive when individual operations fail.

Expected handling:

- Missing API key: print a clear configuration error.
- Empty knowledge directory: explain that `/index` has no documents.
- Missing vector store: suggest running `/index`.
- Embedding failure: stop indexing and show the failed file/chunk.
- Chat failure: show provider error and preserve the session.
- Invalid `SKILL.md`: skip that skill and print a warning in debug mode.
- MCP process failure: disable MCP tools for that turn and continue with RAG-only answering.
- Invalid MCP tool arguments: return a tool observation describing the validation error.

## 7. Testing Strategy

Use focused unit tests for the educational core:

- `SkillLoaderTest`: parses front matter and body.
- `SkillSelectorTest`: selects expected skills for sample queries.
- `TextSplitterTest`: produces stable chunks with source ids.
- `JsonVectorStoreTest`: saves and loads records.
- `CosineSimilarityTest`: ranks known vectors correctly.
- `PromptRendererTest`: replaces variables and preserves Markdown.
- `McpMessageTest`: serializes and parses JSON-RPC messages.
- `AgentLoopTest`: uses fake LLM, fake retriever, and fake MCP client to verify control flow.

External LLM calls should be wrapped behind interfaces so tests do not require network access.

## 8. Initial Files

Seed knowledge files:

```text
knowledge/
  ai-agent.md
  rag.md
  mcp.md
  agent-skills.md
```

Seed skills:

```text
skills/
  markdown-qa/SKILL.md
  summarize-notes/SKILL.md
  compare-notes/SKILL.md
```

Seed prompts:

```text
prompts/
  system.md
  rag-answer.md
  mcp-tool-use.md
```

## 9. Milestones

### Milestone 1: Project Skeleton

- Create Maven Java 21 project.
- Add Jackson and JUnit dependencies.
- Add package structure.
- Add config loader.
- Add CLI loop with `/exit`.

### Milestone 2: Prompt and Agent Skills

- Add prompt template loading.
- Add `SKILL.md` loading.
- Add simple skill selection.
- Print selected skills in debug mode.

### Milestone 3: RAG

- Add document loader and splitter.
- Add embedding client.
- Add JSON vector store.
- Add `/index`.
- Add top-k retrieval.

### Milestone 4: Basic QA

- Add chat client.
- Compose RAG answer prompt.
- Answer questions with sources.
- Add fake-client tests for agent flow.

### Milestone 5: MCP Demo

- Add minimal stdio MCP client.
- Add demo MCP file server.
- Add `/mcp-tools`.
- Allow one MCP tool call round before final answer.

### Milestone 6: Polish and Learning Notes

- Add README explaining the agent architecture.
- Add diagrams for Agent Loop, RAG, Skill, and MCP.
- Add sample `.env.example`.
- Add troubleshooting notes.

## 10. Design Decisions

- Use Java 21 and Maven because they are familiar and explicit.
- Avoid Spring and LangChain4j so the learner can see the agent loop directly.
- Use JSON vector storage because it is inspectable and lightweight.
- Use brute-force vector search because it teaches similarity retrieval without database complexity.
- Treat Agent Skills as prompt-time instruction packs, not executable tools.
- Keep MCP separate from skills so the distinction between instruction and external action stays clear.
- Allow only one MCP tool round in v1 to avoid building a complex planner too early.

## 11. Open Extension Points

After v1 works, the project can evolve in small steps:

- Add embedding-based skill selection.
- Add SQLite vector storage.
- Add script-backed Agent Skills.
- Add multiple MCP servers.
- Add streaming output.
- Add a web UI.
- Add a planner that supports multiple tool rounds.
- Add conversation memory summarization.
