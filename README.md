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

- `/index`：重建 `data/vector-store.json`。
- `/mcp-tools`：列出 demo MCP server 暴露的工具。
- `/exit`：退出。

典型流程：

```text
> /index
Indexed 4 chunks into data/vector-store.json

> 什么是 Agent Skill？它和 MCP 有什么区别？
```

## Project Map

```text
src/main/java/com/example/agentlearn/
  agent/   Agent Loop 和模型响应解析
  cli/     命令行交互
  config/  环境变量配置
  llm/     OpenAI-compatible chat 和 embedding client
  mcp/     最小 stdio MCP client/server
  prompt/  Prompt 模板和组合
  rag/     Markdown 加载、chunk、向量库和检索
  skill/   SKILL.md 加载和选择
  util/    轻量 JSON 工具
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
