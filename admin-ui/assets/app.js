const DEFAULT_API_BASE = window.location.protocol === "file:"
  ? "http://127.0.0.1:8090"
  : window.location.port === "8090"
    ? ""
    : `${window.location.protocol}//${window.location.hostname || "127.0.0.1"}:8090`;

const state = {
  apiBase: DEFAULT_API_BASE,
  token: localStorage.getItem("adminUi.token") || "",
  user: JSON.parse(localStorage.getItem("adminUi.user") || "null"),
  currentTab: "software",
  software: [],
  categories: [],
  tags: [],
  reviews: [],
  logs: [],
  rbacUsers: [],
  rbacRoles: [],
  rbacPermissions: []
};

const pageTitles = {
  software: "软件管理",
  reviews: "审核中心",
  logs: "操作日志",
  rbac: "权限管理",
  categories: "分类管理",
  tags: "标签管理",
  diagnostics: "联调检查"
};

const $ = (selector) => document.querySelector(selector);
const $$ = (selector) => Array.from(document.querySelectorAll(selector));

function setConnectionStatus(text, tone = "ok") {
  const pill = $("#connectionStatus");
  if (!pill) return;
  pill.textContent = text;
  pill.classList.remove("warn", "error");
  if (tone === "warn") pill.classList.add("warn");
  if (tone === "error") pill.classList.add("error");
}

function headers() {
  const requestHeaders = { "Content-Type": "application/json" };
  if (state.token) requestHeaders.Authorization = `Bearer ${state.token}`;
  return requestHeaders;
}

async function request(path, options = {}) {
  const requestHeaders = headers();
  if (options.body instanceof FormData) delete requestHeaders["Content-Type"];
  let response;
  try {
    response = await fetch(`${state.apiBase}${path}`, {
      ...options,
      headers: { ...requestHeaders, ...(options.headers || {}) }
    });
  } catch (error) {
    setConnectionStatus("后端离线", "error");
    throw new Error("后端服务不可访问，请先启动 make run 后刷新页面");
  }
  const payload = await parseApiResponse(response);
  if (response.status === 401) {
    clearSession();
    showLogin("登录已过期，请重新登录");
    throw new Error(payload?.message || "登录已过期，请重新登录");
  }
  if (!response.ok) throw new Error(payload?.message || `请求失败: HTTP ${response.status}`);
  if (!payload) throw new Error("服务返回格式异常");
  if (payload.code !== 0) throw new Error(payload.message || `请求失败: ${payload.code}`);
  setConnectionStatus("后端在线", "ok");
  return payload.data;
}

async function parseApiResponse(response) {
  const contentType = response.headers.get("content-type") || "";
  if (!contentType.includes("application/json")) return null;
  try { return await response.json(); } catch (error) { return null; }
}

function showNotice(message, type = "info") {
  const notice = $("#notice");
  notice.textContent = message;
  notice.classList.toggle("error", type === "error");
  notice.classList.remove("hidden");
  window.clearTimeout(showNotice.timer);
  showNotice.timer = window.setTimeout(() => notice.classList.add("hidden"), 3200);
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}

function queryString(params) {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") search.set(key, value);
  });
  return search.toString();
}

function badgeClass(status) {
  const value = Number(status);
  if (value === 2) return "badge";
  if (value === 1 || value === 0) return "badge amber";
  if (value === 3 || value === 4) return "badge red";
  return "badge gray";
}

function reviewBadgeClass(status) {
  const value = Number(status);
  if (value === 2) return "badge";
  if (value === 0 || value === 1) return "badge amber";
  if (value === 3) return "badge red";
  return "badge gray";
}

function emptyRow(colspan, text) {
  return `<tr><td colspan="${colspan}"><div class="empty-state">${escapeHtml(text)}</div></td></tr>`;
}

function codeListText(items, field) {
  return (items || []).map((item) => item[field]).filter(Boolean).join(",");
}

function selectedNumberValues(selector) {
  const element = $(selector);
  if (!element) return [];
  return Array.from(element.selectedOptions || [])
    .map((option) => Number(option.value))
    .filter((value) => Number.isInteger(value) && value > 0);
}

function setSelectOptions(selector, items, options = {}) {
  const element = $(selector);
  if (!element) return;
  const selected = new Set(Array.from(element.selectedOptions || []).map((option) => option.value));
  const placeholder = options.placeholder !== undefined
    ? `<option value="">${escapeHtml(options.placeholder)}</option>`
    : "";
  const valueOf = options.value || ((item) => item.id);
  const labelOf = options.label || ((item) => item.name || item.role_name || item.permission_name || item.id);
  element.innerHTML = placeholder + (items || []).map((item) => {
    const value = String(valueOf(item));
    return `<option value="${escapeHtml(value)}">${escapeHtml(labelOf(item))}</option>`;
  }).join("");
  Array.from(element.options || []).forEach((option) => {
    if (option.value && selected.has(option.value)) option.selected = true;
  });
}

function categoryLabel(item) {
  return `${"　".repeat(item.level || 0)}${item.name}`;
}

function categoryParentName(parentId) {
  if (!parentId) return "无";
  const parent = state.categories.find((item) => String(item.id) === String(parentId));
  return parent ? parent.name : `未知分类 ${parentId}`;
}

function renderReferenceSelectors() {
  const enabledCategories = state.categories.filter((item) => Number(item.status) === 1);
  setSelectOptions("#uploadCategorySelect", enabledCategories, {
    placeholder: "请选择分类",
    label: categoryLabel
  });
  setSelectOptions("#softwareCategory", enabledCategories, {
    placeholder: "全部分类",
    label: categoryLabel
  });
  setSelectOptions("#categoryParentSelect", enabledCategories, {
    placeholder: "无父分类",
    label: categoryLabel
  });
  setSelectOptions("#uploadTagSelect", state.tags, {
    label: (item) => `${item.name}${Number(item.is_hot) === 1 ? "（热门）" : ""}`
  });
}

function renderSoftwareSelectors() {
  setSelectOptions("#reviewAppSelect", state.software, {
    placeholder: "请选择软件",
    label: (item) => `${item.name}（${item.app_key}）`
  });
}

function renderRbacSelectors() {
  setSelectOptions("#adminUserRoleSelect", state.rbacRoles.filter((item) => Number(item.status) === 1), {
    label: (item) => `${item.role_name}（${item.role_code}）`
  });
  setSelectOptions("#adminRolePermissionSelect", state.rbacPermissions.filter((item) => Number(item.status) === 1), {
    label: (item) => `${item.permission_name}（${item.permission_code}）`
  });
}

function idsFromCodes(value, items, codeField, displayName) {
  const codes = String(value || "")
    .split(",")
    .map((item) => item.trim())
    .filter(Boolean);
  const ids = [];
  const unknown = [];
  codes.forEach((code) => {
    const matched = items.find((item) => item[codeField] === code);
    if (matched) ids.push(matched.id);
    else unknown.push(code);
  });
  if (unknown.length > 0) throw new Error(`${displayName}不存在：${unknown.join(", ")}`);
  return ids;
}

async function login(event) {
  event.preventDefault();
  const message = $("#loginMessage");
  message.textContent = "";
  try {
    const response = await fetch(`${state.apiBase}/api/v1/admin/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username: $("#loginUsername").value.trim(), password: $("#loginPassword").value })
    });
    const payload = await parseApiResponse(response);
    if (!response.ok) throw new Error(payload?.message || `登录失败: HTTP ${response.status}`);
    if (!payload) throw new Error("服务返回格式异常");
    if (payload.code !== 0) throw new Error(payload.message || "登录失败");
    state.token = payload.data.access_token;
    state.user = payload.data.user;
    localStorage.setItem("adminUi.token", state.token);
    localStorage.setItem("adminUi.user", JSON.stringify(state.user));
    showApp();
    await refreshCurrent();
  } catch (error) {
    setConnectionStatus("连接失败", "error");
    message.textContent = error.message || "登录失败";
  }
}

function clearSession() {
  state.token = "";
  state.user = null;
  localStorage.removeItem("adminUi.token");
  localStorage.removeItem("adminUi.user");
}

function logout() {
  clearSession();
  $("#loginPassword").value = "";
  showLogin();
}

function showLogin(message = "") {
  $("#loginScreen").classList.remove("hidden");
  $("#appShell").classList.add("hidden");
  $("#loginUsername").value = "admin";
  $("#sessionUser").textContent = "未登录";
  $("#loginMessage").textContent = message;
  setConnectionStatus("待连接", "warn");
}

function showApp() {
  $("#loginScreen").classList.add("hidden");
  $("#appShell").classList.remove("hidden");
  $("#sessionUser").textContent = state.user ? `${state.user.username} (${state.user.user_type})` : "已登录";
  setConnectionStatus("已连接", "ok");
  switchTab(state.currentTab);
}

async function validateStoredSession() {
  if (!state.token) {
    showLogin();
    return;
  }
  try {
    state.user = await request("/api/v1/admin/auth/me");
    localStorage.setItem("adminUi.user", JSON.stringify(state.user));
    showApp();
    await refreshCurrent();
  } catch (error) {
    clearSession();
    showLogin(error.message || "登录已过期，请重新登录");
  }
}

function switchTab(tab) {
  state.currentTab = tab;
  $("#pageTitle").textContent = pageTitles[tab] || "软件商店后台";
  $$(".nav-item").forEach((item) => item.classList.toggle("active", item.dataset.tab === tab));
  $$(".tab-panel").forEach((panel) => panel.classList.toggle("active", panel.dataset.panel === tab));
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
  renderSoftwareSelectors();
  renderSoftware(page);
}

async function uploadSoftware(event) {
  event.preventDefault();
  const formData = new FormData(event.currentTarget);
  formData.set("tagIds", selectedNumberValues("#uploadTagSelect").join(","));
  if (formData.get("publishNow") === "on") formData.set("publishNow", "true");
  else formData.delete("publishNow");
  const response = await request("/api/v1/admin/software/apps", { method: "POST", body: formData });
  event.currentTarget.reset();
  showNotice(`软件「${response.name}」上传成功`);
  await loadSoftware();
  await loadSoftwareDetail(response.id);
}

function renderSoftware(page) {
  $("#softwareStats").innerHTML = `
    <span class="badge gray">总数 ${page.total ?? 0}</span>
    <span class="badge gray">第 ${page.page ?? 1} 页</span>
  `;
  const rows = state.software.map((item) => `
    <tr>
      <td><div class="primary-text">${escapeHtml(item.name)}</div><div class="subtext">${escapeHtml(item.app_key)}</div></td>
      <td><span class="badge gray">${escapeHtml(item.category_name || "-")}</span><div class="subtext">${(item.tag_names || []).map(escapeHtml).join(" / ") || "-"}</div></td>
      <td><span class="${badgeClass(item.status)}">${escapeHtml(item.status_text)}</span></td>
      <td><div>${escapeHtml(item.latest_version_name || "-")}</div><div class="subtext">安装包 ${item.package_count ?? 0}</div></td>
      <td><div class="row-actions">
        <button data-action="detail" data-id="${item.id}">详情</button>
        <button data-action="review" data-id="${item.id}">提审</button>
        <button data-action="publish" data-id="${item.id}">上架</button>
        <button class="danger" data-action="unpublish" data-id="${item.id}">下架</button>
      </div></td>
    </tr>
  `).join("");
  $("#softwareTable").innerHTML = rows || emptyRow(5, "暂无软件数据");
}

async function loadSoftwareDetail(id) {
  const item = await request(`/api/v1/admin/software/apps/${id}`);
  $("#softwareDetail").innerHTML = `
    <h3>${escapeHtml(item.name)}</h3>
    <div class="subtext">${escapeHtml(item.app_key)}</div>
    <div class="detail-list">
      <div class="detail-item"><strong>状态</strong><span class="${badgeClass(item.status)}">${escapeHtml(item.status_text)}</span></div>
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
    body: JSON.stringify({ reason: action === "publish" ? "管理台上架" : "管理台下架" })
  });
  showNotice(action === "publish" ? "软件已上架" : "软件已下架");
  await loadSoftware();
  await loadSoftwareDetail(id);
}

async function submitSoftwareReview(appId) {
  const reason = window.prompt("审核原因", "准备上架");
  if (reason === null) return;
  await request("/api/v1/admin/reviews", {
    method: "POST",
    body: JSON.stringify({ app_id: Number(appId), reason, priority: 1 })
  });
  showNotice("已提交审核");
  await loadSoftware();
}

async function createReview(event) {
  event.preventDefault();
  const form = new FormData(event.currentTarget);
  const body = {
    app_id: Number(form.get("appId")),
    reason: form.get("reason") || "准备上架",
    priority: Number(form.get("priority") || 1)
  };
  if (form.get("versionId")) body.version_id = Number(form.get("versionId"));
  await request("/api/v1/admin/reviews", { method: "POST", body: JSON.stringify(body) });
  event.currentTarget.reset();
  resetReviewVersionSelect();
  showNotice("审核任务已创建");
  await loadReviews();
}

function resetReviewVersionSelect() {
  setSelectOptions("#reviewVersionSelect", [], { placeholder: "软件级审核" });
}

async function loadReviewVersionOptions(appId) {
  if (!appId) {
    resetReviewVersionSelect();
    return;
  }
  const versions = await request(`/api/v1/admin/software/apps/${appId}/versions`);
  setSelectOptions("#reviewVersionSelect", versions || [], {
    placeholder: "软件级审核",
    label: (item) => `${item.version_name}（versionCode ${item.version_code}，${item.status_text || "未知状态"}）`
  });
}

async function loadReviews() {
  const qs = queryString({
    page: 1,
    page_size: 20,
    status: $("#reviewStatus").value,
    keyword: $("#reviewKeyword").value.trim()
  });
  const page = await request(`/api/v1/admin/reviews?${qs}`);
  state.reviews = page.list || [];
  renderReviews();
}

function renderReviews() {
  const rows = state.reviews.map((item) => `
    <tr>
      <td><div class="primary-text">${escapeHtml(item.title)}</div><div class="subtext">ID ${item.id} · ${escapeHtml(item.submitted_at || "-")}</div></td>
      <td><div>${escapeHtml(item.app_name || `App ${item.app_id}`)}</div><div class="subtext">${item.version_id ? `版本 ${escapeHtml(item.version_name || item.version_id)}` : "软件级审核"}</div></td>
      <td><span class="${reviewBadgeClass(item.status)}">${escapeHtml(item.status_text)}</span></td>
      <td>${escapeHtml(item.priority_text || "普通")}</td>
      <td>${escapeHtml(item.reviewer_id || "-")}</td>
      <td><div class="row-actions">
        <button data-review-action="detail" data-id="${item.id}">详情</button>
        <button data-review-action="assign" data-id="${item.id}">分配</button>
        <button data-review-action="approve" data-id="${item.id}">通过</button>
        <button class="danger" data-review-action="reject" data-id="${item.id}">驳回</button>
      </div></td>
    </tr>
  `).join("");
  $("#reviewTable").innerHTML = rows || emptyRow(6, "暂无审核任务");
}

async function handleReviewAction(id, action) {
  if (action === "detail") {
    const item = await request(`/api/v1/admin/reviews/${id}`);
    const histories = (item.histories || []).map((h) => `${h.action}: ${h.from_status_text || "-"} -> ${h.to_status_text || "-"} ${h.comment || ""}`).join("\n");
    window.alert(`${item.title}\n状态：${item.status_text}\n原因：${item.submit_reason || "-"}\n意见：${item.review_comment || "-"}\n\n${histories || "暂无历史"}`);
    return;
  }
  if (action === "assign") {
    if (state.rbacUsers.length === 0) await loadRbac();
    const reviewerUsername = window.prompt("审核人账号", state.user?.username || "");
    if (!reviewerUsername) return;
    const reviewer = state.rbacUsers.find((item) => item.username === reviewerUsername.trim());
    if (!reviewer) throw new Error(`审核人账号不存在：${reviewerUsername}`);
    await request(`/api/v1/admin/reviews/${id}/assign`, { method: "POST", body: JSON.stringify({ reviewer_id: reviewer.id }) });
    showNotice("审核任务已分配");
  }
  if (action === "approve" || action === "reject") {
    const comment = window.prompt(action === "approve" ? "通过意见" : "驳回原因", action === "approve" ? "检查通过" : "信息不完整");
    if (comment === null) return;
    await request(`/api/v1/admin/reviews/${id}/${action}`, { method: "POST", body: JSON.stringify({ comment }) });
    showNotice(action === "approve" ? "审核已通过" : "审核已驳回");
  }
  await loadReviews();
  await loadSoftware();
}

async function loadLogs() {
  const qs = queryString({
    page: 1,
    page_size: 20,
    action: $("#logAction").value.trim(),
    resource_type: $("#logResourceType").value.trim(),
    resource_id: $("#logResourceId").value,
    detail_keyword: $("#logKeyword").value.trim()
  });
  const page = await request(`/api/v1/admin/operation-logs?${qs}`);
  state.logs = page.list || [];
  renderLogs();
}

function renderLogs() {
  const rows = state.logs.map((item) => `
    <tr>
      <td>${escapeHtml(item.created_at_str || item.created_at || "-")}</td>
      <td><div>${escapeHtml(item.username || "-")}</div><div class="subtext">${escapeHtml(item.user_type || "-")} / ${escapeHtml(item.user_id || "-")}</div></td>
      <td><span class="badge blue">${escapeHtml(item.action)}</span></td>
      <td><div>${escapeHtml(resourceTypeText(item.resource_type))}</div><div class="subtext">${escapeHtml(item.resource_name || "-")} ${item.resource_id ? `#${escapeHtml(item.resource_id)}` : ""}</div></td>
      <td>${escapeHtml(item.display_detail || item.detail || "-")}</td>
      <td>${escapeHtml(item.ip || "-")}</td>
    </tr>
  `).join("");
  $("#logTable").innerHTML = rows || emptyRow(6, "暂无操作日志");
}

function resourceTypeText(value) {
  const names = {
    software: "软件",
    version: "版本",
    package: "安装包",
    review: "审核任务",
    category: "分类",
    tag: "标签",
    rbac: "权限配置"
  };
  return names[value] || value || "-";
}

async function loadRbac() {
  const [users, roles, permissions] = await Promise.all([
    request("/api/v1/admin/rbac/users"),
    request("/api/v1/admin/rbac/roles"),
    request("/api/v1/admin/rbac/permissions?status=1")
  ]);
  state.rbacUsers = users || [];
  state.rbacRoles = roles || [];
  state.rbacPermissions = permissions || [];
  renderRbac();
}

function renderRbac() {
  renderRbacSelectors();
  const permissionCount = $("#rbacPermissionCount");
  if (permissionCount) permissionCount.textContent = `共 ${state.rbacPermissions.length} 项`;
  const userRows = state.rbacUsers.map((item) => {
    const roles = item.roles || [];
    return `
      <tr>
        <td><div class="primary-text">${escapeHtml(item.username)}</div><div class="subtext">${escapeHtml(item.display_name || "-")}</div></td>
        <td><span class="${Number(item.status) === 1 ? "badge" : "badge red"}">${Number(item.status) === 1 ? "启用" : "禁用"}</span></td>
        <td><div>${roles.map((role) => escapeHtml(role.role_name)).join(" / ") || "-"}</div><div class="subtext">${codeListText(roles, "role_code") || "-"}</div></td>
        <td><div class="row-actions">
          <button data-rbac-user-action="roles" data-id="${item.id}">分配角色</button>
          <button data-rbac-user-action="password" data-id="${item.id}">重置密码</button>
          <button class="${Number(item.status) === 1 ? "danger" : ""}" data-rbac-user-action="status" data-status="${Number(item.status) === 1 ? 0 : 1}" data-id="${item.id}">${Number(item.status) === 1 ? "禁用" : "启用"}</button>
        </div></td>
      </tr>
    `;
  }).join("");
  $("#rbacUserTable").innerHTML = userRows || emptyRow(4, "暂无管理员账号");

  const roleRows = state.rbacRoles.map((item) => {
    const permissions = item.permissions || [];
    return `
      <tr>
        <td><div class="primary-text">${escapeHtml(item.role_name)}</div><div class="subtext">${escapeHtml(item.role_code)}</div></td>
        <td><span class="${Number(item.status) === 1 ? "badge" : "badge red"}">${Number(item.status) === 1 ? "启用" : "禁用"}</span></td>
        <td><div>${permissions.map((permission) => `<span class="badge gray">${escapeHtml(permission.permission_code)}</span>`).join(" ") || "-"}</div><div class="subtext">${permissions.map((permission) => escapeHtml(permission.permission_name)).join(" / ") || "-"}</div></td>
        <td><div class="row-actions">
          <button data-rbac-role-action="permissions" data-id="${item.id}">分配权限</button>
          <button class="${Number(item.status) === 1 ? "danger" : ""}" data-rbac-role-action="status" data-status="${Number(item.status) === 1 ? 0 : 1}" data-id="${item.id}">${Number(item.status) === 1 ? "禁用" : "启用"}</button>
        </div></td>
      </tr>
    `;
  }).join("");
  $("#rbacRoleTable").innerHTML = roleRows || emptyRow(4, "暂无角色");

  $("#rbacPermissionList").innerHTML = state.rbacPermissions.map((item) => `
    <article class="tile">
      <h3>${escapeHtml(item.permission_name)}</h3>
      <div class="tile-meta">
        <span>模块: ${escapeHtml(item.module)}</span>
        <span>编码: ${escapeHtml(item.permission_code)}</span>
      </div>
      <div class="subtext">${escapeHtml(item.description || "-")}</div>
    </article>
  `).join("") || `<div class="empty-state">暂无权限点</div>`;
}

async function createAdminUser(event) {
  event.preventDefault();
  const form = new FormData(event.currentTarget);
  await request("/api/v1/admin/rbac/users", {
    method: "POST",
    body: JSON.stringify({
      username: form.get("username"),
      display_name: form.get("display_name"),
      password: form.get("password"),
      role_ids: selectedNumberValues("#adminUserRoleSelect")
    })
  });
  event.currentTarget.reset();
  showNotice("管理员账号已新增");
  await loadRbac();
}

async function createAdminRole(event) {
  event.preventDefault();
  const form = new FormData(event.currentTarget);
  await request("/api/v1/admin/rbac/roles", {
    method: "POST",
    body: JSON.stringify({
      role_code: form.get("role_code"),
      role_name: form.get("role_name"),
      description: form.get("description"),
      permission_ids: selectedNumberValues("#adminRolePermissionSelect")
    })
  });
  event.currentTarget.reset();
  showNotice("角色已新增");
  await loadRbac();
}

async function handleRbacUserAction(id, action, status) {
  const user = state.rbacUsers.find((item) => String(item.id) === String(id));
  if (!user) return;
  if (action === "roles") {
    const value = window.prompt("角色编码，多个用英文逗号分隔", codeListText(user.roles, "role_code"));
    if (value === null) return;
    await request(`/api/v1/admin/rbac/users/${id}/roles`, {
      method: "PUT",
      body: JSON.stringify({ role_ids: idsFromCodes(value, state.rbacRoles, "role_code", "角色编码") })
    });
    showNotice("账号角色已更新");
  }
  if (action === "password") {
    const password = window.prompt("新密码，至少6位");
    if (password === null) return;
    await request(`/api/v1/admin/rbac/users/${id}/password`, { method: "POST", body: JSON.stringify({ password }) });
    showNotice("账号密码已重置");
  }
  if (action === "status") {
    await request(`/api/v1/admin/rbac/users/${id}/status`, { method: "POST", body: JSON.stringify({ status: Number(status) }) });
    showNotice(Number(status) === 1 ? "账号已启用" : "账号已禁用");
  }
  await loadRbac();
}

async function handleRbacRoleAction(id, action, status) {
  const role = state.rbacRoles.find((item) => String(item.id) === String(id));
  if (!role) return;
  if (action === "permissions") {
    const value = window.prompt("权限编码，多个用英文逗号分隔", codeListText(role.permissions, "permission_code"));
    if (value === null) return;
    await request(`/api/v1/admin/rbac/roles/${id}/permissions`, {
      method: "PUT",
      body: JSON.stringify({ permission_ids: idsFromCodes(value, state.rbacPermissions, "permission_code", "权限编码") })
    });
    showNotice("角色权限已更新");
  }
  if (action === "status") {
    await request(`/api/v1/admin/rbac/roles/${id}`, {
      method: "PUT",
      body: JSON.stringify({
        role_name: role.role_name,
        description: role.description,
        status: Number(status)
      })
    });
    showNotice(Number(status) === 1 ? "角色已启用" : "角色已禁用");
  }
  await loadRbac();
}

async function loadCategories() {
  const tree = await request("/api/v1/admin/categories/tree");
  state.categories = flattenCategories(tree);
  renderCategories();
}

function flattenCategories(items, level = 0) {
  return (items || []).flatMap((item) => [{ ...item, level }, ...flattenCategories(item.children || [], level + 1)]);
}

function renderCategories() {
  $("#categoryList").innerHTML = state.categories.map((item) => `
    <article class="tile"><h3>${"　".repeat(item.level)}${escapeHtml(item.name)}</h3><div class="tile-meta"><span>父级 ${escapeHtml(categoryParentName(item.parent_id))}</span><span>排序 ${item.sort_order ?? 0}</span><span>状态 ${item.status === 1 ? "启用" : "禁用"}</span></div><div class="row-actions"><button data-category-action="toggle" data-id="${item.id}">启停</button><button class="danger" data-category-action="delete" data-id="${item.id}">删除</button></div></article>
  `).join("") || `<div class="empty-state">暂无分类数据</div>`;
  renderReferenceSelectors();
}

async function createCategory(event) {
  event.preventDefault();
  const form = new FormData(event.currentTarget);
  await request("/api/v1/admin/categories", { method: "POST", body: JSON.stringify({ name: form.get("name"), parent_id: form.get("parent_id") || null, sort_order: Number(form.get("sort_order") || 0) }) });
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
    <article class="tile"><h3>${escapeHtml(item.name)}</h3><div class="tile-meta"><span>热门 ${item.is_hot === 1 ? "是" : "否"}</span><span>关联软件 ${item.app_count ?? 0}</span><span>${escapeHtml(item.description || "无描述")}</span></div><div class="row-actions"><button data-tag-action="hot" data-id="${item.id}">热门</button><button class="danger" data-tag-action="delete" data-id="${item.id}">删除</button></div></article>
  `).join("") || `<div class="empty-state">暂无标签数据</div>`;
  renderReferenceSelectors();
}

async function createTag(event) {
  event.preventDefault();
  const form = new FormData(event.currentTarget);
  await request("/api/v1/admin/tags", { method: "POST", body: JSON.stringify({ name: form.get("name"), description: form.get("description"), is_hot: form.get("is_hot") === "on" ? 1 : 0 }) });
  event.currentTarget.reset();
  showNotice("标签已新增");
  await loadTags();
}

async function runDiagnostics() {
  const output = $("#diagnosticsOutput");
  output.textContent = "检查中...";
  const checks = [
    ["健康检查", "/api/v1/admin/health"],
    ["模块列表", "/api/v1/admin/modules"],
    ["软件列表", "/api/v1/admin/software/apps"],
    ["审核任务", "/api/v1/admin/reviews"],
    ["操作日志", "/api/v1/admin/operation-logs"],
    ["权限用户", "/api/v1/admin/rbac/users"],
    ["权限角色", "/api/v1/admin/rbac/roles"],
    ["分类列表", "/api/v1/admin/categories"],
    ["标签列表", "/api/v1/admin/tags"]
  ];
  const lines = [];
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

async function ensureReferenceData() {
  const tasks = [];
  if (state.categories.length === 0) tasks.push(loadCategories());
  if (state.tags.length === 0) tasks.push(loadTags());
  if (tasks.length > 0) await Promise.all(tasks);
  renderReferenceSelectors();
}

async function ensureSoftwareOptions() {
  if (state.software.length === 0) await loadSoftware();
  else renderSoftwareSelectors();
}

async function refreshCurrent() {
  try {
    if (state.currentTab === "software") {
      await ensureReferenceData();
      await loadSoftware();
    }
    if (state.currentTab === "reviews") {
      await ensureSoftwareOptions();
      await loadReviews();
    }
    if (state.currentTab === "logs") await loadLogs();
    if (state.currentTab === "rbac") await loadRbac();
    if (state.currentTab === "categories") await loadCategories();
    if (state.currentTab === "tags") await loadTags();
    if (state.currentTab === "diagnostics") await runDiagnostics();
  } catch (error) {
    showNotice(error.message, "error");
  }
}

function bindEvents() {
  $("#loginUsername").value = "admin";
  $("#apiDocLink").href = state.apiBase ? `${state.apiBase}/swagger-ui.html` : "/swagger-ui.html";
  $("#loginForm").addEventListener("submit", login);
  $("#logoutButton").addEventListener("click", logout);
  $("#refreshCurrent").addEventListener("click", refreshCurrent);
  $("#searchSoftware").addEventListener("click", refreshCurrent);
  $("#softwareUploadForm").addEventListener("submit", (event) => uploadSoftware(event).catch((error) => showNotice(error.message, "error")));
  $("#reviewCreateForm").addEventListener("submit", (event) => createReview(event).catch((error) => showNotice(error.message, "error")));
  $("#reviewAppSelect").addEventListener("change", (event) => {
    loadReviewVersionOptions(event.target.value).catch((error) => showNotice(error.message, "error"));
  });
  $("#reloadReviews").addEventListener("click", loadReviews);
  $("#searchReviews").addEventListener("click", loadReviews);
  $("#reloadLogs").addEventListener("click", loadLogs);
  $("#searchLogs").addEventListener("click", loadLogs);
  $("#reloadRbac").addEventListener("click", loadRbac);
  $("#reloadCategories").addEventListener("click", loadCategories);
  $("#reloadTags").addEventListener("click", loadTags);
  $("#runDiagnostics").addEventListener("click", runDiagnostics);
  $("#adminUserForm").addEventListener("submit", (event) => createAdminUser(event).catch((error) => showNotice(error.message, "error")));
  $("#adminRoleForm").addEventListener("submit", (event) => createAdminRole(event).catch((error) => showNotice(error.message, "error")));
  $("#categoryForm").addEventListener("submit", (event) => createCategory(event).catch((error) => showNotice(error.message, "error")));
  $("#tagForm").addEventListener("submit", (event) => createTag(event).catch((error) => showNotice(error.message, "error")));

  $$(".nav-item").forEach((button) => {
    button.addEventListener("click", async () => {
      switchTab(button.dataset.tab);
      await refreshCurrent();
    });
  });

  $("#softwareTable").addEventListener("click", async (event) => {
    const button = event.target.closest("button[data-action]");
    if (!button) return;
    try {
      if (button.dataset.action === "detail") await loadSoftwareDetail(button.dataset.id);
      if (button.dataset.action === "review") await submitSoftwareReview(button.dataset.id);
      if (button.dataset.action === "publish") await changeSoftwareStatus(button.dataset.id, "publish");
      if (button.dataset.action === "unpublish") await changeSoftwareStatus(button.dataset.id, "unpublish");
    } catch (error) {
      showNotice(error.message, "error");
    }
  });

  $("#reviewTable").addEventListener("click", async (event) => {
    const button = event.target.closest("button[data-review-action]");
    if (!button) return;
    try { await handleReviewAction(button.dataset.id, button.dataset.reviewAction); }
    catch (error) { showNotice(error.message, "error"); }
  });

  $("#rbacUserTable").addEventListener("click", async (event) => {
    const button = event.target.closest("button[data-rbac-user-action]");
    if (!button) return;
    try { await handleRbacUserAction(button.dataset.id, button.dataset.rbacUserAction, button.dataset.status); }
    catch (error) { showNotice(error.message, "error"); }
  });

  $("#rbacRoleTable").addEventListener("click", async (event) => {
    const button = event.target.closest("button[data-rbac-role-action]");
    if (!button) return;
    try { await handleRbacRoleAction(button.dataset.id, button.dataset.rbacRoleAction, button.dataset.status); }
    catch (error) { showNotice(error.message, "error"); }
  });

  $("#categoryList").addEventListener("click", async (event) => {
    const button = event.target.closest("button[data-category-action]");
    if (!button) return;
    try {
      const id = button.dataset.id;
      if (button.dataset.categoryAction === "toggle") await request(`/api/v1/admin/categories/${id}/toggle-status`, { method: "POST" });
      if (button.dataset.categoryAction === "delete") await request(`/api/v1/admin/categories/${id}`, { method: "DELETE" });
      showNotice("分类已更新");
      await loadCategories();
    } catch (error) { showNotice(error.message, "error"); }
  });

  $("#tagList").addEventListener("click", async (event) => {
    const button = event.target.closest("button[data-tag-action]");
    if (!button) return;
    try {
      const id = button.dataset.id;
      if (button.dataset.tagAction === "hot") await request(`/api/v1/admin/tags/${id}/toggle-hot`, { method: "POST" });
      if (button.dataset.tagAction === "delete") await request(`/api/v1/admin/tags/${id}`, { method: "DELETE" });
      showNotice("标签已更新");
      await loadTags();
    } catch (error) { showNotice(error.message, "error"); }
  });
}

bindEvents();
validateStoredSession();
