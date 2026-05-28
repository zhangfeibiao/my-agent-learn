# Personal Knowledge Agent

一个不依赖 Agent 框架的 Java CLI 学习项目，用来理解 AI Agent 的核心实现过程。

第一版支持：

- 本地 Markdown / text 知识库。
- 轻量 RAG：文档加载、chunk、embedding、JSON 向量库、余弦相似度。
- Prompt 模板。
- Agent Skills：只加载 `skills/*/SKILL.md` 指令包。
- 最小 MCP：stdio JSON-RPC、`tools/list`、`tools/call`。

## Requirements

当前实现使用 Java 17。项目内有轻量脚本，不需要全局 Maven：

```bash
./scripts/test.sh
./scripts/package.sh
./scripts/run.sh
./scripts/web.sh
```

如果你的机器有 Java 21，也可以把 `pom.xml` 和脚本里的 release 从 17 调整到 21。

## Configure

复制 `.env.example` 为 `.env`，填入你的 API Key。`./scripts/run.sh` 会自动加载 `.env`：

```bash
cp .env.example .env
```

默认示例使用硅基流动的 OpenAI-compatible API：

```bash
AGENT_API_BASE_URL=https://api.siliconflow.cn/v1
AGENT_CHAT_MODEL=deepseek-ai/DeepSeek-V4-Flash
AGENT_EMBEDDING_MODEL=netease-youdao/bce-embedding-base_v1
```

## Run

```bash
./scripts/package.sh
./scripts/run.sh
```

CLI 命令：

- `/index`：增量更新 `data/vector-store.json`，只对新增或修改文件重新 embedding。
- `/reindex`：强制全量重建 `data/vector-store.json`。
- `/mcp-tools`：列出 demo MCP server 暴露的工具。
- `/exit`：退出。

典型流程：

```text
> /index
Updated 4 chunks into data/vector-store.json

> 什么是 Agent Skill？它和 MCP 有什么区别？
```

## Web UI

第一版 Web UI 仍然不使用框架，后端用 JDK 自带 `HttpServer`，前端是原生 HTML/CSS/JS。

```bash
./scripts/web.sh
```

默认地址是 `http://127.0.0.1:8080/`。如果要换端口：

```bash
AGENT_WEB_PORT=18080 ./scripts/web.sh
```

当前 UI 支持：

- 聊天会话：调用同一个 Agent 问答流程。
- 知识库管理：查看、新建、编辑、删除 `knowledge` 下的 `.md/.txt`。
- Skills 管理：查看、新建、编辑、删除 `skills/*/SKILL.md`。
- Prompt 管理：查看、新建、编辑、删除 `prompts` 下的 `.md`。
- 索引管理：增量构建和完整重建 `data/vector-store.json`。
- 日志中心：查看每次 chat / embedding API 调用的输入、输出、耗时、唯一 id 和 session id。

## LLM Call Logs

大模型调用日志默认写入 `data/llm-call-logs.jsonl`，可以通过 `.env` 覆盖：

```bash
AGENT_LLM_LOG_PATH=data/llm-call-logs.jsonl
```

每条日志包含 `id`、`sessionId`、`type`、`model`、`endpoint`、`stream`、`startedAt`、`endedAt`、`durationMs`、`request`、`response` 和 `error`。当前实现是同步 HTTP 调用；日志结构已经保留 `sessionId` 和 `stream` 字段，后续接 SSE 时可以把同一次流式输出按同一个调用 id 或 session id 聚合。

## Incremental Indexing

`/index` 会先读取旧的 `data/vector-store.json`。如果某个文件的 `sourceHash`、`embeddingModel` 和 `splitterVersion` 都没有变化，就直接复用旧 JSON 里的 embedding 向量；如果文件新增或修改，就重新切分并调用 embedding 模型；如果文件被删除，对应 records 会从新索引中移除。

最后仍会整体重写一次 `data/vector-store.json`，但不会对未变化文件重复调用 embedding。

## Project Map

```text
src/main/java/com/example/agentlearn/
  agent/   Agent Loop 和模型响应解析
  cli/     命令行交互
  config/  环境变量配置
  llm/     OpenAI-compatible chat 和 embedding client
  llm/logging/ LLM 调用日志记录
  mcp/     最小 stdio MCP client/server
  prompt/  Prompt 模板和组合
  rag/     Markdown 加载、chunk、向量库和检索
  runtime/ CLI 和 Web 共用的 Agent 装配
  skill/   SKILL.md 加载和选择
  util/    轻量 JSON 工具
  web/     JDK HttpServer 后台和管理 UI API
src/main/resources/web/
  原生 HTML/CSS/JS 管理台
```

## How The Pieces Fit

```text
User question
  -> select Agent Skills
  -> embed question
  -> retrieve top-k chunks from JSON vector store
  -> compose prompt
  -> ask chat model
  -> optionally call one MCP tool
  -> ask chat model for final answer
  -> print answer and sources
```

Agent Skill 和 MCP 的边界：

- Agent Skill 是 Prompt-time instruction pack。
- MCP Tool 是 process boundary 上的 executable capability。
