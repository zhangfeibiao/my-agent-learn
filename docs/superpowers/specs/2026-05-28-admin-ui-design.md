# Admin UI Design

## Goal

Add a local, no-framework web admin console for the personal knowledge agent.

## Scope

- Keep the existing CLI.
- Add a JDK `HttpServer` backend.
- Add native HTML/CSS/JavaScript frontend assets.
- Support local use only on `localhost`.
- Reuse existing `Agent`, `Indexer`, `SkillLoader`, prompt files, and knowledge files.

## First Version Features

- Chat page: ask questions, show answer, show sources separately.
- Knowledge page: list Markdown/text files, view/edit/create/delete files.
- Skills page: list `skills/*/SKILL.md`, view/edit files.
- Prompts page: list `prompts/*.md`, view/edit files.
- Index page: show vector store status, run incremental index, run full reindex.

## Architecture

```text
Browser
  -> native app.js fetch()
  -> JDK HttpServer
  -> existing Agent / Indexer / file system services
```

Static resources live under `src/main/resources/web` and are copied into `target/classes` by `scripts/package.sh`.

The server exposes:

```text
GET    /
GET    /assets/app.css
GET    /assets/app.js
GET    /api/config
POST   /api/chat
POST   /api/index
POST   /api/reindex
GET    /api/knowledge
GET    /api/knowledge/file?path=...
PUT    /api/knowledge/file
DELETE /api/knowledge/file?path=...
GET    /api/skills
GET    /api/skills/file?name=...
PUT    /api/skills/file
GET    /api/prompts
GET    /api/prompts/file?name=...
PUT    /api/prompts/file
```

## Safety

- File APIs may only access configured `knowledge/`, `skills/`, and `prompts/` directories.
- Path traversal is rejected.
- `.env`, `data/vector-store.json`, and other non-target files are not exposed.

## Out of Scope

- Authentication.
- Multi-user sessions.
- Streaming responses.
- Rich Markdown preview.
- Drag-and-drop upload.
- Production deployment.
