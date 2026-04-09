async function ensureAdmin() {
  await initCsrf()
  const me = await loadMe()
  if (!me) {
    location.href = "/auth"
    return null
  }
  if (me.role !== "admin") {
    location.href = "/workbench"
    return null
  }
  document.getElementById("me-pill").textContent = me.emailOrUsername + " · " + me.role
  await TransportCrypto.ensureSession()
  return me
}

function bufToB64(buf) {
  const bytes = new Uint8Array(buf)
  let bin = ""
  for (let i = 0; i < bytes.length; i++) bin += String.fromCharCode(bytes[i])
  return btoa(bin)
}

async function fileToB64(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(bufToB64(reader.result))
    reader.onerror = reject
    reader.readAsArrayBuffer(file)
  })
}

const state = { sortBy: "createdAt", sortDir: "desc", columns: [] }

function setSortHeader(thead, sortBy, sortDir) {
  const ths = thead.querySelectorAll("th.sortable")
  ths.forEach(th => {
    th.classList.remove("asc", "desc")
    if (th.dataset.sort === sortBy) th.classList.add(sortDir === "desc" ? "desc" : "asc")
  })
}

function buildColumns(rows) {
  const cols = ["id", "createdAt"]
  for (const r of rows) {
    const d = r.data || {}
    for (const k of Object.keys(d)) {
      if (!cols.includes(k)) cols.push(k)
    }
  }
  return cols
}

function cellText(v) {
  if (v == null) return ""
  if (typeof v === "object") return JSON.stringify(v)
  return String(v)
}

async function refresh() {
  const rows = await TransportCrypto.fetch(`/api/admin/qar-table/rows?sortBy=${encodeURIComponent(state.sortBy)}&sortDir=${encodeURIComponent(state.sortDir)}`, { method: "GET" })
  const table = document.getElementById("qar-table")
  const thead = table.querySelector("thead")
  const tbody = table.querySelector("tbody")
  tbody.innerHTML = ""

  document.getElementById("empty-msg").style.display = rows.length ? "none" : "block"
  state.columns = buildColumns(rows)

  thead.innerHTML = ""
  const trh = document.createElement("tr")
  for (const c of state.columns) {
    const th = document.createElement("th")
    th.textContent = c
    th.dataset.sort = c
    th.className = "sortable"
    th.addEventListener("click", async () => {
      if (state.sortBy === c) state.sortDir = state.sortDir === "asc" ? "desc" : "asc"
      else { state.sortBy = c; state.sortDir = "asc" }
      await refresh()
    })
    trh.appendChild(th)
  }
  thead.appendChild(trh)
  setSortHeader(thead, state.sortBy, state.sortDir)

  for (const r of rows) {
    const tr = document.createElement("tr")
    for (const c of state.columns) {
      const td = document.createElement("td")
      if (c === "id") td.textContent = r.id || ""
      else if (c === "createdAt") td.textContent = (r.createdAt || "").replace("T", " ").replace("Z", "")
      else td.textContent = cellText((r.data || {})[c])
      tr.appendChild(td)
    }
    tbody.appendChild(tr)
  }
}

async function onSaveRow() {
  const raw = (document.getElementById("row-json").value || "").trim()
  if (!raw) {
    showToast("缺少数据", "请输入JSON", "danger")
    return
  }
  let data = null
  try {
    data = JSON.parse(raw)
  } catch (e) {
    showToast("格式错误", "JSON无法解析", "danger")
    return
  }
  const id = (document.getElementById("row-id").value || "").trim()
  try {
    if (id) {
      await TransportCrypto.fetch(`/api/admin/qar-table/rows/${encodeURIComponent(id)}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ data })
      })
      showToast("已写入", "记录已更新", "success")
    } else {
      await TransportCrypto.fetch("/api/admin/qar-table/rows", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ data })
      })
      showToast("已新增", "记录已创建", "success")
    }
    await refresh()
  } catch (e) {
    showToast("保存失败", e.message, "danger")
  }
}

function renderPreview(preview) {
  const thead = document.querySelector("#preview-table thead")
  const tbody = document.querySelector("#preview-table tbody")
  thead.innerHTML = ""
  tbody.innerHTML = ""
  const cols = preview.columns || []
  const rows = preview.rows || []
  document.getElementById("preview-info").textContent = cols.length ? `列数 ${cols.length} · 预览行数 ${rows.length}` : "未解析到数据"
  if (!cols.length) return
  const trh = document.createElement("tr")
  for (const c of cols) {
    const th = document.createElement("th")
    th.textContent = c
    trh.appendChild(th)
  }
  thead.appendChild(trh)
  for (const r of rows) {
    const tr = document.createElement("tr")
    for (const c of cols) {
      const td = document.createElement("td")
      td.textContent = cellText(r[c])
      tr.appendChild(td)
    }
    tbody.appendChild(tr)
  }
}

async function onPreviewXlsx() {
  const fileInput = document.getElementById("xlsx-file")
  if (!fileInput.files || !fileInput.files.length) {
    showToast("缺少文件", "请选择xlsx文件", "danger")
    return
  }
  const file = fileInput.files[0]
  try {
    const b64 = await fileToB64(file)
    const preview = await TransportCrypto.fetch("/api/admin/qar-table/xlsx/preview-b64?maxRows=20", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ filename: file.name, dataBase64: b64 })
    })
    renderPreview(preview)
    showToast("预览完成", "已解析xlsx", "success")
  } catch (e) {
    showToast("预览失败", e.message, "danger")
  }
}

async function onImportXlsx() {
  const fileInput = document.getElementById("xlsx-file")
  if (!fileInput.files || !fileInput.files.length) {
    showToast("缺少文件", "请选择xlsx文件", "danger")
    return
  }
  const file = fileInput.files[0]
  try {
    const b64 = await fileToB64(file)
    const resp = await TransportCrypto.fetch("/api/admin/qar-table/xlsx/import-b64", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ filename: file.name, dataBase64: b64 })
    })
    showToast("导入完成", `已写入 ${resp.imported || 0} 行`, "success")
    await refresh()
  } catch (e) {
    showToast("导入失败", e.message, "danger")
  }
}

async function onLogout() {
  const btn = document.getElementById("btn-logout")
  btn.disabled = true
  try {
    await apiFetch("/api/auth/logout", { method: "POST" })
    location.href = "/auth"
  } catch (e) {
    showToast("退出失败", e.message, "danger")
  } finally {
    btn.disabled = false
  }
}

async function main() {
  const me = await ensureAdmin()
  if (!me) return
  document.getElementById("btn-logout").addEventListener("click", onLogout)
  document.getElementById("btn-refresh").addEventListener("click", refresh)
  document.getElementById("btn-save-row").addEventListener("click", onSaveRow)
  document.getElementById("btn-preview").addEventListener("click", onPreviewXlsx)
  document.getElementById("btn-import").addEventListener("click", onImportXlsx)
  await refresh()
}

main()

