function cellText(v) {
  if (v == null) return ""
  if (typeof v === "object") return JSON.stringify(v)
  return String(v)
}

const state = {
  files: [],
  fileId: null,
  fileLabel: "",
  sheets: [],
  sheetIndex: 0,
  keyword: "",
  dateStart: "",
  dateEnd: "",
  timeSortDir: "desc"
}

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

function findTimeColumn(cols) {
  const list = cols || []
  const prefers = ["时间", "timestamp", "time", "日期", "date"]
  for (const p of prefers) {
    const c = list.find(x => (x || "").toLowerCase().includes(p))
    if (c) return c
  }
  return null
}

function matchesKeyword(row, keyword) {
  if (!keyword) return true
  const k = keyword.toLowerCase()
  for (const v of Object.values(row || {})) {
    if ((v == null ? "" : String(v)).toLowerCase().includes(k)) return true
  }
  return false
}

function toDateKey(v) {
  if (!v) return ""
  const s = String(v)
  if (s.includes("T")) return s.split("T")[0]
  if (/^\d{4}-\d{2}-\d{2}/.test(s)) return s.slice(0, 10)
  if (/^\d{8}$/.test(s)) return `${s.slice(0, 4)}-${s.slice(4, 6)}-${s.slice(6, 8)}`
  if (/^\d{4}\/\d{1,2}\/\d{1,2}/.test(s)) {
    const parts = s.split(/[^\d]+/).filter(Boolean)
    if (parts.length >= 3) {
      const y = parts[0]
      const m = parts[1].padStart(2, "0")
      const d = parts[2].padStart(2, "0")
      return `${y}-${m}-${d}`
    }
  }
  if (/^\d{4}年\d{1,2}月\d{1,2}日/.test(s)) {
    const parts = s.replace("年", "-").replace("月", "-").replace("日", "").split("-")
    if (parts.length >= 3) {
      const y = parts[0]
      const m = (parts[1] || "").padStart(2, "0")
      const d = (parts[2] || "").padStart(2, "0")
      return `${y}-${m}-${d}`
    }
  }
  return ""
}

function timeSortValue(v) {
  if (v == null) return null
  if (typeof v === "number") return v
  const s = String(v)
  const ms = Date.parse(s)
  if (!Number.isNaN(ms)) return ms
  const dk = toDateKey(s)
  if (dk) return Date.parse(dk + "T00:00:00Z")
  return s
}

function matchesDateRow(row, timeKey, startDate, endDate) {
  if (!startDate && !endDate) return true
  if (!timeKey) return true
  const d = toDateKey(row[timeKey])
  if (!d) return false
  if (startDate && d < startDate) return false
  if (endDate && d > endDate) return false
  return true
}

function currentSheet() {
  const sheets = state.sheets || []
  return sheets[state.sheetIndex] || null
}

function renderSheetTabs() {
  const wrap = document.getElementById("sheet-tabs")
  wrap.innerHTML = ""
  const sheets = state.sheets || []
  for (let i = 0; i < sheets.length; i++) {
    const s = sheets[i]
    const b = document.createElement("button")
    b.className = "btn" + (i === state.sheetIndex ? " primary" : "")
    b.textContent = s.name || ("Sheet" + (i + 1))
    b.addEventListener("click", () => {
      state.sheetIndex = i
      renderAll()
    })
    wrap.appendChild(b)
  }
}

function getFilteredSortedRows(sheet) {
  const cols = sheet.columns || []
  const rows = sheet.rows || []
  const timeKey = findTimeColumn(cols)
  const keyword = (state.keyword || "").trim()
  const startDate = state.dateStart || ""
  const endDate = state.dateEnd || ""
  const filtered = rows.filter(r => matchesKeyword(r, keyword) && matchesDateRow(r, timeKey, startDate, endDate))
  if (!timeKey) return filtered
  filtered.sort((a, b) => {
    const va = timeSortValue(a[timeKey])
    const vb = timeSortValue(b[timeKey])
    if (va == null && vb == null) return 0
    if (va == null) return 1
    if (vb == null) return -1
    if (va < vb) return state.timeSortDir === "asc" ? -1 : 1
    if (va > vb) return state.timeSortDir === "asc" ? 1 : -1
    return 0
  })
  return filtered
}

function renderTable() {
  const sheet = currentSheet()
  const table = document.getElementById("qar-table")
  const thead = table.querySelector("thead")
  const tbody = table.querySelector("tbody")
  tbody.innerHTML = ""
  thead.innerHTML = ""

  if (!sheet) {
    document.getElementById("empty-msg").style.display = "block"
    return
  }

  const cols = sheet.columns || []
  const rows = getFilteredSortedRows(sheet)
  document.getElementById("empty-msg").style.display = rows.length ? "none" : "block"

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

function renderMeta(sheet) {
  const el = document.getElementById("sheet-meta")
  if (!sheet) {
    el.textContent = ""
    return
  }
  const total = sheet.totalRows != null ? sheet.totalRows : (sheet.rows || []).length
  const shown = (sheet.rows || []).length
  el.textContent = `总行数 ${total} · 已加载 ${shown}`
}

function renderAll() {
  renderSheetTabs()
  const sheet = currentSheet()
  renderMeta(sheet)
  renderTable()
}

async function refreshFiles() {
  const files = await TransportCrypto.fetch("/api/admin/flight-xlsx/files", { method: "GET" })
  state.files = files || []
  const sel = document.getElementById("file-select")
  sel.innerHTML = ""
  if (!state.files.length) {
    const opt = document.createElement("option")
    opt.value = ""
    opt.textContent = "暂无xlsx文件（请先在工作台上传）"
    sel.appendChild(opt)
    state.fileId = null
    state.sheets = []
    renderAll()
    return
  }

  for (const f of state.files) {
    const opt = document.createElement("option")
    opt.value = f.id
    const t = (f.createdAt || "").replace("T", " ").replace("Z", "")
    opt.textContent = `${f.originalName || f.id} · ${t}`
    sel.appendChild(opt)
  }

  const wanted = state.fileId && state.files.find(x => x.id === state.fileId) ? state.fileId : state.files[0].id
  sel.value = wanted
  state.fileId = wanted
  await loadPreview()
}

async function loadPreview() {
  if (!state.fileId) {
    state.sheets = []
    renderAll()
    return
  }
  try {
    const resp = await TransportCrypto.fetch(`/api/admin/flight-xlsx/files/${encodeURIComponent(state.fileId)}/preview?maxRows=20000`, { method: "GET" })
    state.fileLabel = resp.originalName || ""
    state.sheets = resp.sheets || []
    state.sheetIndex = 0
    renderAll()
  } catch (e) {
    state.sheets = []
    renderAll()
    showToast("加载失败", e.message, "danger")
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

function onResetFilters() {
  document.getElementById("filter-keyword").value = ""
  document.getElementById("filter-date-start").value = ""
  document.getElementById("filter-date-end").value = ""
  state.keyword = ""
  state.dateStart = ""
  state.dateEnd = ""
  renderAll()
}

async function main() {
  const me = await ensureAdmin()
  if (!me) return
  document.getElementById("btn-logout").addEventListener("click", onLogout)
  document.getElementById("btn-refresh").addEventListener("click", refreshFiles)
  document.getElementById("btn-reset-filters").addEventListener("click", onResetFilters)
  document.getElementById("filter-keyword").addEventListener("input", () => {
    state.keyword = document.getElementById("filter-keyword").value || ""
    renderAll()
  })
  document.getElementById("filter-date-start").addEventListener("change", () => {
    state.dateStart = document.getElementById("filter-date-start").value || ""
    renderAll()
  })
  document.getElementById("filter-date-end").addEventListener("change", () => {
    state.dateEnd = document.getElementById("filter-date-end").value || ""
    renderAll()
  })
  document.getElementById("btn-time-asc").addEventListener("click", () => {
    state.timeSortDir = "asc"
    renderAll()
  })
  document.getElementById("btn-time-desc").addEventListener("click", () => {
    state.timeSortDir = "desc"
    renderAll()
  })
  document.getElementById("file-select").addEventListener("change", async (e) => {
    state.fileId = e.target.value
    await loadPreview()
  })
  await refreshFiles()
}

main()
