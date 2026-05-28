const DEFAULT_API_BASE = window.location.port === "8090"
  ? ""
  : `${window.location.protocol}//${window.location.hostname || "127.0.0.1"}:8090`;

const state = {
  apiBase: DEFAULT_API_BASE,
  token: localStorage.getItem("adminUi.token") || "",
  user: JSON.parse(localStorage.getItem("adminUi.user") || "null"),
  currentTab: "software",
  software: [],
  categories: [],
  tags: []
};

const $ = (selector) => document.querySelector(selector);
const $$ = (selector) => Array.from(document.querySelectorAll(selector));

function setConnectionStatus(text, tone = "warn") {
  const pill = $("#connectionStatus");
  if (!pill) return;
  pill.textContent = text;
  pill.classList.remove("warn", "error");
  if (tone === "warn") pill.classList.add("warn");
  if (tone === "error") pill.classList.add("error");
}

function headers() {
  const requestHeaders = {
    "Content-Type": "application/json"
  };
  if (state.token) {
    requestHeaders.Authorization = `Bearer ${state.token}`;
  }
  return requestHeaders;
}

async function request(path, options = {}) {
  const requestHeaders = headers();
  if (options.body instanceof FormData) {
    delete requestHeaders["Content-Type"];
  }
  let response;
  try {
    response = await fetch(`${state.apiBase}${path}`, {
      ...options,
      headers: {
        ...requestHeaders,
        ...(options.headers || {})
      }
    });
  } catch (error) {
    setConnectionStatus("后端离线", "error");
    throw new Error("后端服务不可访问，请先启动 make run-admin-service 后刷新页面");
  }
  const payload = await parseApiResponse(response);
  if (!response.ok) {
    throw new Error(payload?.message || `请求失败: HTTP ${response.status}`);
  }
  if (!payload) {
    throw new Error("服务返回格式异常");
  }
  if (payload.code !== 0) {
    throw new Error(payload.message || `请求失败: ${payload.code}`);
  }
  setConnectionStatus("后端在线", "ok");
  return payload.data;
}

async function parseApiResponse(response) {
  const contentType = response.headers.get("content-type") || "";
  if (!contentType.includes("application/json")) {
    return null;
  }
  try {
    return await response.json();
  } catch (error) {
    return null;
  }
}

function showNotice(message, type = "info") {
  const notice = $("#notice");
  notice.textContent = message;
  notice.classList.toggle("error", type === "error");
  notice.classList.remove("hidden");
  window.clearTimeout(showNotice.timer);
  showNotice.timer = window.setTimeout(() => notice.classList.add("hidden"), 3200);
}

async function login(event) {
  event.preventDefault();
  const message = $("#loginMessage");
  message.textContent = "";

  try {
    let response;
    try {
      response = await fetch(`${state.apiBase}/api/v1/admin/auth/login`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          username: $("#loginUsername").value.trim(),
          password: $("#loginPassword").value
        })
      });
    } catch (networkError) {
      throw new Error("后端服务不可访问，请先启动 make run-admin-service 后再登录");
    }
    const payload = await parseApiResponse(response);
    if (!response.ok) {
      throw new Error(payload?.message || `登录失败: HTTP ${response.status}`);
    }
    if (!payload) {
      throw new Error("服务返回格式异常");
    }
    if (payload.code !== 0) {
      throw new Error(payload.message || "登录失败");
    }

    state.token = payload.data.access_token;
    state.user = payload.data.user;
    localStorage.setItem("adminUi.token", state.token);
    localStorage.setItem("adminUi.user", JSON.stringify(state.user));
    setConnectionStatus("已连接", "ok");
    showApp();
    await refreshCurrent();
  } catch (error) {
    message.textContent = error.message || "登录失败";
  }
}

function logout() {
  state.token = "";
  state.user = null;
  localStorage.removeItem("adminUi.token");
  localStorage.removeItem("adminUi.user");
  $("#loginPassword").value = "";
  showLogin();
}

function showLogin() {
  $("#loginScreen").classList.remove("hidden");
  $("#appShell").classList.add("hidden");
  $("#loginUsername").value = "admin";
  setConnectionStatus("待连接", "warn");
  $("#serviceIndicator").textContent = "软件商店后台管理";
}

function showApp() {
  $("#loginScreen").classList.add("hidden");
  $("#appShell").classList.remove("hidden");
  $("#sessionUser").textContent = state.user ? `${state.user.username} (${state.user.user_type})` : "admin";
  $("#serviceIndicator").textContent = "软件商店后台管理";
  setConnectionStatus("已连接", "ok");
}

function queryString(params) {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") {
      search.set(key, value);
    }
  });
  return search.toString();
}

function statusClass(status) {
  if (status === 2) return "badge";
  if (status === 1 || status === 4) return "badge amber";
  return "badge gray";
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}

async function loadSoftware() {
  const qs = queryString({
    page: 1,
    page_size: 20,
    keyword: $("#softwareKeyword").value.trim(),
    status: $("#softwareStatus").value,
    category_id: $("#softwareCategory").value
  });
  const page = await request(`/api/v1/admin/software/apps?${qs}`);
  state.software = page.list || [];
  renderSoftware(page);
}

async function uploadSoftware(event) {
  event.preventDefault();
  const formData = new FormData(event.currentTarget);
  if (formData.get("publishNow") === "on") {
    formData.set("publishNow", "true");
  } else {
    formData.delete("publishNow");
  }

  const response = await request("/api/v1/admin/software/apps", {
    method: "POST",
    body: formData
  });
  event.currentTarget.reset();
  showNotice(`软件「${response.name}」上传成功`);
  await loadSoftware();
  await loadSoftwareDetail(response.id);
}

function renderSoftware(page) {
  $("#softwareStats").innerHTML = `
    <span class="badge gray">总数 ${page.total ?? 0}</span>
    <span class="badge gray">页码 ${page.page ?? 1}</span>
  `;

  const rows = state.software.map((item) => `
    <tr>
      <td>
        <div class="software-name">${escapeHtml(item.name)}</div>
        <div class="subtext">${escapeHtml(item.app_key)}</div>
      </td>
      <td>
        <span class="badge gray">${escapeHtml(item.category_name || "-")}</span>
        <div class="subtext">${(item.tag_names || []).map(escapeHtml).join(" / ")}</div>
      </td>
      <td><span class="${statusClass(item.status)}">${escapeHtml(item.status_text)}</span></td>
      <td>
        <div>${escapeHtml(item.latest_version_name || "-")}</div>
        <div class="subtext">安装包 ${item.package_count ?? 0}</div>
      </td>
      <td>
        <div class="row-actions">
          <button data-action="detail" data-id="${item.id}">详情</button>
          <button data-action="publish" data-id="${item.id}">上架</button>
          <button data-action="unpublish" data-id="${item.id}">下架</button>
        </div>
      </td>
    </tr>
  `).join("");
  $("#softwareTable").innerHTML = rows || `<tr><td colspan="5"><div class="empty-state">暂无软件数据</div></td></tr>`;
}

async function loadSoftwareDetail(id) {
  const item = await request(`/api/v1/admin/software/apps/${id}`);
  $("#softwareDetail").innerHTML = `
    <h3>${escapeHtml(item.name)}</h3>
    <div class="subtext">${escapeHtml(item.app_key)}</div>
    <div class="detail-list">
      <div class="detail-item"><strong>状态</strong><div>${escapeHtml(item.status_text)}</div></div>
      <div class="detail-item"><strong>分类</strong><div>${escapeHtml(item.category_name || "-")}</div></div>
      <div class="detail-item"><strong>系统</strong><div>${(item.supported_os_types || []).map(escapeHtml).join(" / ") || "-"}</div></div>
      <div class="detail-item"><strong>架构</strong><div>${(item.supported_archs || []).map(escapeHtml).join(" / ") || "-"}</div></div>
      <div class="detail-item"><strong>摘要</strong><div>${escapeHtml(item.summary || "-")}</div></div>
      <div class="detail-item"><strong>更新时间</strong><div>${escapeHtml(item.updated_at || "-")}</div></div>
    </div>
  `;
}

async function changeSoftwareStatus(id, action) {
  await request(`/api/v1/admin/software/apps/${id}/${action}`, {
    method: "POST",
    body: JSON.stringify({ reason: action === "publish" ? "测试环境上架" : "测试环境下架" })
  });
  showNotice(action === "publish" ? "软件已上架" : "软件已下架");
  await loadSoftware();
  await loadSoftwareDetail(id);
}

async function loadCategories() {
  const tree = await request("/api/v1/admin/categories/tree");
  state.categories = flattenCategories(tree);
  renderCategories();
}

function flattenCategories(items, level = 0) {
  return (items || []).flatMap((item) => [
    { ...item, level },
    ...flattenCategories(item.children || [], level + 1)
  ]);
}

function renderCategories() {
  $("#categoryList").innerHTML = state.categories.map((item) => `
    <article class="tile">
      <h3>${"&nbsp;".repeat(item.level * 4)}${escapeHtml(item.name)}</h3>
      <div class="tile-meta">
        <span>ID: ${item.id}</span>
        <span>父级: ${item.parent_id || "-"}</span>
        <span>排序: ${item.sort_order ?? 0}</span>
        <span>状态: ${item.status === 1 ? "启用" : "禁用"}</span>
      </div>
      <div class="row-actions">
        <button data-category-action="toggle" data-id="${item.id}">启停</button>
        <button data-category-action="delete" data-id="${item.id}">删除</button>
      </div>
    </article>
  `).join("") || `<div class="empty-state">暂无分类数据</div>`;
}

async function createCategory(event) {
  event.preventDefault();
  const form = new FormData(event.currentTarget);
  await request("/api/v1/admin/categories", {
    method: "POST",
    body: JSON.stringify({
      name: form.get("name"),
      parent_id: form.get("parent_id") || null,
      sort_order: Number(form.get("sort_order") || 0)
    })
  });
  event.currentTarget.reset();
  showNotice("分类已新增");
  await loadCategories();
}

async function loadTags() {
  state.tags = await request("/api/v1/admin/tags");
  renderTags();
}

function renderTags() {
  $("#tagList").innerHTML = state.tags.map((item) => `
    <article class="tile">
      <h3>${escapeHtml(item.name)}</h3>
      <div class="tile-meta">
        <span>ID: ${item.id}</span>
        <span>热门: ${item.is_hot === 1 ? "是" : "否"}</span>
        <span>关联软件: ${item.app_count ?? 0}</span>
        <span>${escapeHtml(item.description || "无描述")}</span>
      </div>
      <div class="row-actions">
        <button data-tag-action="hot" data-id="${item.id}">热门</button>
        <button data-tag-action="delete" data-id="${item.id}">删除</button>
      </div>
    </article>
  `).join("") || `<div class="empty-state">暂无标签数据</div>`;
}

async function createTag(event) {
  event.preventDefault();
  const form = new FormData(event.currentTarget);
  await request("/api/v1/admin/tags", {
    method: "POST",
    body: JSON.stringify({
      name: form.get("name"),
      description: form.get("description"),
      is_hot: form.get("is_hot") === "on" ? 1 : 0
    })
  });
  event.currentTarget.reset();
  showNotice("标签已新增");
  await loadTags();
}

async function runDiagnostics() {
  const output = $("#diagnosticsOutput");
  output.textContent = "检查中...";
  const lines = [];
  const checks = [
    ["健康检查", "/api/v1/admin/health"],
    ["模块列表", "/api/v1/admin/modules"],
    ["分类列表", "/api/v1/admin/categories"],
    ["标签列表", "/api/v1/admin/tags"]
  ];
  for (const [name, path] of checks) {
    try {
      const data = await request(path);
      lines.push(`[OK] ${name}\n${JSON.stringify(data, null, 2)}`);
    } catch (error) {
      lines.push(`[FAIL] ${name}\n${error.message}`);
    }
  }
  output.textContent = lines.join("\n\n");
}

async function refreshCurrent() {
  try {
    if (state.currentTab === "software") await loadSoftware();
    if (state.currentTab === "categories") await loadCategories();
    if (state.currentTab === "tags") await loadTags();
    if (state.currentTab === "diagnostics") await runDiagnostics();
  } catch (error) {
    showNotice(error.message, "error");
  }
}

function bindEvents() {
  $("#loginUsername").value = "admin";
  $("#loginForm").addEventListener("submit", login);
  $("#logoutButton").addEventListener("click", logout);
  $("#refreshCurrent").addEventListener("click", refreshCurrent);
  $("#searchSoftware").addEventListener("click", () => refreshCurrent());
  $("#softwareUploadForm").addEventListener("submit", (event) => uploadSoftware(event).catch((error) => showNotice(error.message, "error")));
  $("#reloadCategories").addEventListener("click", () => refreshCurrent());
  $("#reloadTags").addEventListener("click", () => refreshCurrent());
  $("#runDiagnostics").addEventListener("click", runDiagnostics);
  $("#categoryForm").addEventListener("submit", (event) => createCategory(event).catch((error) => showNotice(error.message, "error")));
  $("#tagForm").addEventListener("submit", (event) => createTag(event).catch((error) => showNotice(error.message, "error")));

  $$(".nav-item").forEach((button) => {
    button.addEventListener("click", async () => {
      state.currentTab = button.dataset.tab;
      $$(".nav-item").forEach((item) => item.classList.toggle("active", item === button));
      $$(".tab-panel").forEach((panel) => panel.classList.toggle("active", panel.dataset.panel === state.currentTab));
      await refreshCurrent();
    });
  });

  $("#softwareTable").addEventListener("click", async (event) => {
    const button = event.target.closest("button[data-action]");
    if (!button) return;
    try {
      if (button.dataset.action === "detail") await loadSoftwareDetail(button.dataset.id);
      if (button.dataset.action === "publish") await changeSoftwareStatus(button.dataset.id, "publish");
      if (button.dataset.action === "unpublish") await changeSoftwareStatus(button.dataset.id, "unpublish");
    } catch (error) {
      showNotice(error.message, "error");
    }
  });

  $("#categoryList").addEventListener("click", async (event) => {
    const button = event.target.closest("button[data-category-action]");
    if (!button) return;
    try {
      const action = button.dataset.categoryAction;
      const id = button.dataset.id;
      if (action === "toggle") await request(`/api/v1/admin/categories/${id}/toggle-status`, { method: "POST" });
      if (action === "delete") await request(`/api/v1/admin/categories/${id}`, { method: "DELETE" });
      showNotice("分类已更新");
      await loadCategories();
    } catch (error) {
      showNotice(error.message, "error");
    }
  });

  $("#tagList").addEventListener("click", async (event) => {
    const button = event.target.closest("button[data-tag-action]");
    if (!button) return;
    try {
      const action = button.dataset.tagAction;
      const id = button.dataset.id;
      if (action === "hot") await request(`/api/v1/admin/tags/${id}/toggle-hot`, { method: "POST" });
      if (action === "delete") await request(`/api/v1/admin/tags/${id}`, { method: "DELETE" });
      showNotice("标签已更新");
      await loadTags();
    } catch (error) {
      showNotice(error.message, "error");
    }
  });
}

bindEvents();
if (state.token) {
  showApp();
  refreshCurrent();
} else {
  showLogin();
}
