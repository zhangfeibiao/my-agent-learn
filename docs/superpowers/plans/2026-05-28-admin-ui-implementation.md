# Admin UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a local no-framework web admin console for chat, knowledge, skills, prompts, and indexing.

**Architecture:** Add a JDK `HttpServer` entrypoint that serves static resources and JSON APIs. The backend reuses existing `Agent`, `Indexer`, `SkillLoader`, and filesystem-backed knowledge/prompt/skill files. The frontend is native HTML/CSS/JS with left navigation and module-specific panels.

**Tech Stack:** Java 17, JDK `HttpServer`, native HTML/CSS/JavaScript, existing JDK-only test scripts.

---

## Tasks

- [ ] Add a file access service with path traversal protection and tests.
- [ ] Add a reusable application runtime factory for Agent/Indexer construction.
- [ ] Add a web server entrypoint and JSON API handlers.
- [ ] Add native static UI assets.
- [ ] Add `scripts/web.sh`, package resource copying, and README docs.
- [ ] Verify tests, package, help, and browser smoke.
