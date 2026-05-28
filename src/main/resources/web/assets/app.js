const state = {
  view: "chat",
  knowledgePath: "",
  skillName: "",
  promptPath: "",
  logId: ""
};

const titles = {
  chat: ["聊天", "本地 Markdown 知识库问答"],
  knowledge: ["知识库", "Markdown 文件管理"],
  skills: ["Skills", "Agent Skills 管理"],
  prompts: ["Prompt", "系统 Prompt 管理"],
  index: ["索引", "向量索引与运行状态"],
  logs: ["日志中心", "大模型 API 调用输入输出"]
};

const $ = (selector) => document.querySelector(selector);

async function api(path, options = {}) {
  const init = { ...options };
  if (init.body && typeof init.body !== "string") {
    init.body = JSON.stringify(init.body);
    init.headers = { "Content-Type": "application/json", ...(init.headers || {}) };
  }

  const response = await fetch(path, init);
  const text = await response.text();
  const data = text ? JSON.parse(text) : {};
  if (!response.ok) {
    throw new Error(data.error || `HTTP ${response.status}`);
  }
  return data;
}

function setStatus(text) {
  $("#status-pill").textContent = text;
}

function switchView(view) {
  state.view = view;
  document.querySelectorAll(".nav-item").forEach((button) => {
    button.classList.toggle("active", button.dataset.view === view);
  });
  document.querySelectorAll(".view").forEach((section) => {
    section.classList.toggle("active", section.id === `view-${view}`);
  });
  $("#view-title").textContent = titles[view][0];
  $("#view-subtitle").textContent = titles[view][1];

  if (view === "knowledge") {
    loadKnowledge();
  } else if (view === "skills") {
    loadSkills();
  } else if (view === "prompts") {
    loadPrompts();
  } else if (view === "index") {
    loadConfig();
  } else if (view === "logs") {
    loadLogs();
  }
}

function setBusy(button, busy) {
  button.disabled = busy;
}

function appendMessage(role, text) {
  const messages = $("#messages");
  const empty = messages.querySelector(".empty");
  if (empty) {
    empty.remove();
  }
  const message = document.createElement("div");
  message.className = `message ${role}`;
  message.textContent = text;
  messages.appendChild(message);
  messages.scrollTop = messages.scrollHeight;
  return message;
}

function renderSources(sources) {
  const container = $("#source-list");
  container.innerHTML = "";
  if (!sources || sources.length === 0) {
    const empty = document.createElement("div");
    empty.className = "empty";
    empty.textContent = "无";
    container.appendChild(empty);
    return;
  }

  sources.forEach((source) => {
    const chip = document.createElement("div");
    chip.className = "source-chip";
    chip.textContent = source;
    container.appendChild(chip);
  });
}

async function sendChat(event) {
  event.preventDefault();
  const input = $("#chat-input");
  const message = input.value.trim();
  if (!message) {
    return;
  }

  appendMessage("user", message);
  input.value = "";
  const pending = appendMessage("agent", "思考中...");
  const button = $("#chat-form button");
  setBusy(button, true);
  setStatus("模型回答中");

  try {
    const response = await api("/api/chat", {
      method: "POST",
      body: { message }
    });
    pending.textContent = response.answer || "";
    renderSources(response.sources || []);
    setStatus("就绪");
  } catch (error) {
    pending.textContent = `请求失败：${error.message}`;
    setStatus("请求失败");
  } finally {
    setBusy(button, false);
  }
}

function formatSize(size) {
  if (size < 1024) {
    return `${size} B`;
  }
  return `${(size / 1024).toFixed(1)} KB`;
}

function renderFileList(container, files, activeValue, valueKey, onSelect) {
  container.innerHTML = "";
  if (!files || files.length === 0) {
    const empty = document.createElement("div");
    empty.className = "empty";
    empty.textContent = "暂无文件";
    container.appendChild(empty);
    return;
  }

  files.forEach((file) => {
    const value = file[valueKey];
    const item = document.createElement("button");
    item.className = "file-item";
    item.type = "button";
    item.classList.toggle("active", activeValue === value);
    item.innerHTML = `<strong></strong><small></small>`;
    item.querySelector("strong").textContent = value;
    item.querySelector("small").textContent = formatSize(file.size || 0);
    item.addEventListener("click", () => onSelect(value));
    container.appendChild(item);
  });
}

async function loadKnowledge() {
  try {
    const data = await api("/api/knowledge");
    renderFileList($("#knowledge-list"), data.files, state.knowledgePath, "path", readKnowledge);
    if (!state.knowledgePath && data.files.length > 0) {
      await readKnowledge(data.files[0].path);
    }
  } catch (error) {
    setStatus(`知识库加载失败：${error.message}`);
  }
}

async function readKnowledge(path) {
  try {
    const data = await api(`/api/knowledge/file?path=${encodeURIComponent(path)}`);
    state.knowledgePath = data.path;
    $("#knowledge-path").value = data.path;
    $("#knowledge-editor").value = data.content;
    await loadKnowledge();
  } catch (error) {
    setStatus(`文件读取失败：${error.message}`);
  }
}

async function saveKnowledge() {
  const button = $("#save-knowledge");
  setBusy(button, true);
  try {
    const path = $("#knowledge-path").value.trim();
    const content = $("#knowledge-editor").value;
    await api("/api/knowledge/file", { method: "PUT", body: { path, content } });
    state.knowledgePath = path;
    await loadKnowledge();
    setStatus("知识文件已保存");
  } catch (error) {
    setStatus(`保存失败：${error.message}`);
  } finally {
    setBusy(button, false);
  }
}

async function deleteKnowledge() {
  const path = $("#knowledge-path").value.trim();
  if (!path) {
    return;
  }
  const button = $("#delete-knowledge");
  setBusy(button, true);
  try {
    await api(`/api/knowledge/file?path=${encodeURIComponent(path)}`, { method: "DELETE" });
    state.knowledgePath = "";
    $("#knowledge-path").value = "";
    $("#knowledge-editor").value = "";
    await loadKnowledge();
    setStatus("知识文件已删除");
  } catch (error) {
    setStatus(`删除失败：${error.message}`);
  } finally {
    setBusy(button, false);
  }
}

async function loadSkills() {
  try {
    const data = await api("/api/skills");
    renderFileList($("#skill-list"), data.files, state.skillName, "name", readSkill);
    if (!state.skillName && data.files.length > 0) {
      await readSkill(data.files[0].name);
    }
  } catch (error) {
    setStatus(`Skills 加载失败：${error.message}`);
  }
}

async function readSkill(name) {
  try {
    const data = await api(`/api/skills/file?name=${encodeURIComponent(name)}`);
    state.skillName = data.name;
    $("#skill-name").value = data.name;
    $("#skill-editor").value = data.content;
    await loadSkills();
  } catch (error) {
    setStatus(`Skill 读取失败：${error.message}`);
  }
}

async function saveSkill() {
  const button = $("#save-skill");
  setBusy(button, true);
  try {
    const name = $("#skill-name").value.trim();
    const content = $("#skill-editor").value;
    await api("/api/skills/file", { method: "PUT", body: { name, content } });
    state.skillName = name;
    await loadSkills();
    setStatus("Skill 已保存");
  } catch (error) {
    setStatus(`保存失败：${error.message}`);
  } finally {
    setBusy(button, false);
  }
}

async function deleteSkill() {
  const name = $("#skill-name").value.trim();
  if (!name) {
    return;
  }
  const button = $("#delete-skill");
  setBusy(button, true);
  try {
    await api(`/api/skills/file?name=${encodeURIComponent(name)}`, { method: "DELETE" });
    state.skillName = "";
    $("#skill-name").value = "";
    $("#skill-editor").value = "";
    await loadSkills();
    setStatus("Skill 已删除");
  } catch (error) {
    setStatus(`删除失败：${error.message}`);
  } finally {
    setBusy(button, false);
  }
}

async function loadPrompts() {
  try {
    const data = await api("/api/prompts");
    renderFileList($("#prompt-list"), data.files, state.promptPath, "path", readPrompt);
    if (!state.promptPath && data.files.length > 0) {
      await readPrompt(data.files[0].path);
    }
  } catch (error) {
    setStatus(`Prompt 加载失败：${error.message}`);
  }
}

async function readPrompt(path) {
  try {
    const data = await api(`/api/prompts/file?path=${encodeURIComponent(path)}`);
    state.promptPath = data.path;
    $("#prompt-path").value = data.path;
    $("#prompt-editor").value = data.content;
    await loadPrompts();
  } catch (error) {
    setStatus(`Prompt 读取失败：${error.message}`);
  }
}

async function savePrompt() {
  const button = $("#save-prompt");
  setBusy(button, true);
  try {
    const path = $("#prompt-path").value.trim();
    const content = $("#prompt-editor").value;
    await api("/api/prompts/file", { method: "PUT", body: { path, content } });
    state.promptPath = path;
    await loadPrompts();
    setStatus("Prompt 已保存");
  } catch (error) {
    setStatus(`保存失败：${error.message}`);
  } finally {
    setBusy(button, false);
  }
}

async function deletePrompt() {
  const path = $("#prompt-path").value.trim();
  if (!path) {
    return;
  }
  const button = $("#delete-prompt");
  setBusy(button, true);
  try {
    await api(`/api/prompts/file?path=${encodeURIComponent(path)}`, { method: "DELETE" });
    state.promptPath = "";
    $("#prompt-path").value = "";
    $("#prompt-editor").value = "";
    await loadPrompts();
    setStatus("Prompt 已删除");
  } catch (error) {
    setStatus(`删除失败：${error.message}`);
  } finally {
    setBusy(button, false);
  }
}

async function loadConfig() {
  try {
    const config = await api("/api/config");
    const stats = [
      ["知识文件", config.knowledgeFileCount],
      ["Skills", config.skillCount],
      ["Prompts", config.promptFileCount],
      ["Vector Store", config.vectorStoreExists ? "已生成" : "未生成"],
      ["Chat Model", config.chatModel],
      ["Embedding Model", config.embeddingModel],
      ["API Base", config.apiBaseUrl],
      ["知识目录", config.knowledgeDir],
      ["日志文件", config.llmLogPath]
    ];

    const container = $("#config-stats");
    container.innerHTML = "";
    stats.forEach(([label, value]) => {
      const item = document.createElement("div");
      item.className = "stat";
      item.innerHTML = `<span></span><strong></strong>`;
      item.querySelector("span").textContent = label;
      item.querySelector("strong").textContent = String(value);
      container.appendChild(item);
    });
    setStatus("状态已刷新");
  } catch (error) {
    setStatus(`状态加载失败：${error.message}`);
  }
}

function renderIndexResult(result) {
  $("#index-result").textContent = JSON.stringify(result, null, 2);
}

async function runIndex(path, button) {
  setBusy(button, true);
  setStatus(path === "/api/reindex" ? "完整重建中" : "增量构建中");
  try {
    const result = await api(path, { method: "POST" });
    renderIndexResult(result);
    await loadConfig();
    setStatus("索引完成");
  } catch (error) {
    renderIndexResult({ error: error.message });
    setStatus(`索引失败：${error.message}`);
  } finally {
    setBusy(button, false);
  }
}

function formatDateTime(value) {
  if (!value) {
    return "";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return date.toLocaleString();
}

function logStatus(log) {
  return log.error ? "失败" : "成功";
}

function renderLogList(logs) {
  const container = $("#log-list");
  container.innerHTML = "";
  if (!logs || logs.length === 0) {
    const empty = document.createElement("div");
    empty.className = "empty";
    empty.textContent = "暂无日志";
    container.appendChild(empty);
    return;
  }

  logs.forEach((log) => {
    const item = document.createElement("button");
    item.className = "log-item";
    item.type = "button";
    item.dataset.id = log.id;
    item.classList.toggle("active", state.logId === log.id);
    item.innerHTML = `
      <div class="log-item-row"><strong></strong><span></span></div>
      <div class="log-model"></div>
      <div class="log-meta"></div>
    `;
    item.querySelector("strong").textContent = `${log.type} · ${log.durationMs}ms`;
    item.querySelector("span").textContent = logStatus(log);
    item.querySelector("span").className = log.error ? "log-status failed" : "log-status ok";
    item.querySelector(".log-model").textContent = log.model;
    item.querySelector(".log-meta").textContent = `${formatDateTime(log.startedAt)} · ${log.sessionId}`;
    item.addEventListener("click", () => readLog(log.id));
    container.appendChild(item);
  });
}

async function loadLogs() {
  try {
    const data = await api("/api/llm-logs");
    renderLogList(data.logs || []);
    if (!state.logId && data.logs && data.logs.length > 0) {
      await readLog(data.logs[0].id, false);
    } else if (!data.logs || data.logs.length === 0) {
      $("#log-detail").textContent = "";
      $("#log-detail-hint").textContent = "选择一条日志";
    }
    setStatus("日志已刷新");
  } catch (error) {
    setStatus(`日志加载失败：${error.message}`);
  }
}

async function readLog(id, refreshList = true) {
  try {
    const data = await api(`/api/llm-logs/detail?id=${encodeURIComponent(id)}`);
    state.logId = id;
    $("#log-detail").textContent = JSON.stringify(data.log, null, 2);
    $("#log-detail-hint").textContent = `${data.log.type} · ${data.log.id}`;
    document.querySelectorAll(".log-item").forEach((item) => item.classList.toggle("active", item.dataset.id === id));
    if (refreshList) {
      await loadLogs();
    }
  } catch (error) {
    setStatus(`日志详情加载失败：${error.message}`);
  }
}

async function clearLogs() {
  const button = $("#clear-logs");
  setBusy(button, true);
  try {
    await api("/api/llm-logs", { method: "DELETE" });
    state.logId = "";
    $("#log-detail").textContent = "";
    $("#log-detail-hint").textContent = "选择一条日志";
    await loadLogs();
    setStatus("日志已清空");
  } catch (error) {
    setStatus(`清空日志失败：${error.message}`);
  } finally {
    setBusy(button, false);
  }
}

function bindEvents() {
  document.querySelectorAll(".nav-item").forEach((button) => {
    button.addEventListener("click", () => switchView(button.dataset.view));
  });

  $("#chat-form").addEventListener("submit", sendChat);
  $("#clear-chat").addEventListener("click", () => {
    $("#messages").innerHTML = '<div class="empty">暂无消息</div>';
    renderSources([]);
  });

  $("#new-knowledge").addEventListener("click", () => {
    state.knowledgePath = "";
    $("#knowledge-path").value = "notes/new-note.md";
    $("#knowledge-editor").value = "# New Note\n";
  });
  $("#refresh-knowledge").addEventListener("click", loadKnowledge);
  $("#save-knowledge").addEventListener("click", saveKnowledge);
  $("#delete-knowledge").addEventListener("click", deleteKnowledge);

  $("#new-skill").addEventListener("click", () => {
    state.skillName = "";
    $("#skill-name").value = "new-skill";
    $("#skill-editor").value = "---\nname: new-skill\ndescription: Describe when this skill should be selected.\n---\n\nUse this skill when...\n";
  });
  $("#refresh-skills").addEventListener("click", loadSkills);
  $("#save-skill").addEventListener("click", saveSkill);
  $("#delete-skill").addEventListener("click", deleteSkill);

  $("#new-prompt").addEventListener("click", () => {
    state.promptPath = "";
    $("#prompt-path").value = "custom.md";
    $("#prompt-editor").value = "";
  });
  $("#refresh-prompts").addEventListener("click", loadPrompts);
  $("#save-prompt").addEventListener("click", savePrompt);
  $("#delete-prompt").addEventListener("click", deletePrompt);

  $("#refresh-config").addEventListener("click", loadConfig);
  $("#run-index").addEventListener("click", (event) => runIndex("/api/index", event.currentTarget));
  $("#run-reindex").addEventListener("click", (event) => runIndex("/api/reindex", event.currentTarget));

  $("#refresh-logs").addEventListener("click", loadLogs);
  $("#clear-logs").addEventListener("click", clearLogs);
}

function init() {
  bindEvents();
  $("#messages").innerHTML = '<div class="empty">暂无消息</div>';
  renderSources([]);
  switchView("chat");
}

init();
