let allPersons = []

let personSortField = "createdAt"
let personSortOrder = "desc"

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
  return me
}

async function refreshPersons() {
  try {
    const rows = await apiFetch("/api/admin/persons", { method: "GET" })
    allPersons = rows || []
    applyPersonFiltersAndSort()
  } catch (e) {
    showToast("加载人员数据失败", e.message, "danger")
  }
}

function applyPersonFiltersAndSort() {
  const keyword = (document.getElementById("search-keyword").value || "").toLowerCase()
  const startDate = document.getElementById("filter-date-start").value
  const endDate = document.getElementById("filter-date-end").value

  let filtered = allPersons.filter(p => {
    const matchesKeyword =
      (p.personNo || "").toLowerCase().includes(keyword) ||
      (p.fullName || "").toLowerCase().includes(keyword) ||
      (p.department || "").toLowerCase().includes(keyword) ||
      (p.airline || "").toLowerCase().includes(keyword) ||
      (p.positionTitle || "").toLowerCase().includes(keyword)

    let matchesDate = true
    if (p.createdAt) {
      const createdDate = p.createdAt.split("T")[0]
      if (startDate && createdDate < startDate) matchesDate = false
      if (endDate && createdDate > endDate) matchesDate = false
    } else if (startDate || endDate) {
      matchesDate = false
    }

    return matchesKeyword && matchesDate
  })

  filtered.sort((a, b) => {
    let valA = a[personSortField] || ""
    let valB = b[personSortField] || ""
    if (typeof valA === "string") valA = valA.toLowerCase()
    if (typeof valB === "string") valB = valB.toLowerCase()
    if (valA < valB) return personSortOrder === "asc" ? -1 : 1
    if (valA > valB) return personSortOrder === "asc" ? 1 : -1
    return 0
  })

  renderPersonTable(filtered)
}

function renderPersonTable(data) {
  const tbody = document.querySelector("#persons tbody")
  tbody.innerHTML = ""
  const emptyMsg = document.getElementById("empty-msg");
  
  if (data.length === 0) {
    emptyMsg.style.display = "block";
    return;
  }
  emptyMsg.style.display = "none";

  for (const r of data) {
    const tr = document.createElement("tr")
    tr.innerHTML = "<td></td><td></td><td></td><td></td><td></td><td></td><td></td><td class='actions'></td>"
    tr.children[0].textContent = r.personNo
    tr.children[1].textContent = r.fullName
    tr.children[2].textContent = r.airline || "-"
    tr.children[3].textContent = r.positionTitle || "-"
    tr.children[4].textContent = r.department || "-"
    tr.children[5].textContent = r.phone || "-"
    tr.children[6].textContent = (r.createdAt || "").replace("T", " ").replace("Z", "").slice(0, 16)

    const btnEdit = document.createElement("button")
    btnEdit.className = "btn"
    btnEdit.textContent = "编辑"
    btnEdit.addEventListener("click", () => onEditPerson(r))

    const btnDel = document.createElement("button")
    btnDel.className = "btn danger"
    btnDel.textContent = "删除"
    btnDel.style.backgroundColor = "#ff4d4f"
    btnDel.style.color = "white"
    btnDel.addEventListener("click", () => onDeletePerson(r))

    tr.children[7].appendChild(btnEdit)
    tr.children[7].appendChild(btnDel)
    tbody.appendChild(tr)
  }
}

async function onEditPerson(r) {
  const name = window.prompt("姓名：", r.fullName)
  if (name === null) return
  const airline = window.prompt("航司：", r.airline)
  if (airline === null) return
  const pos = window.prompt("职位：", r.positionTitle)
  if (pos === null) return
  const dep = window.prompt("部门：", r.department)
  if (dep === null) return
  const phone = window.prompt("电话：", r.phone)
  if (phone === null) return

  try {
    await apiFetch("/api/admin/persons/" + r.id, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        fullName: name,
        airline: airline,
        positionTitle: pos,
        department: dep,
        phone: phone
      })
    })
    showToast("已更新", r.personNo, "success")
    await refreshPersons()
  } catch (e) {
    showToast("更新失败", e.message, "danger")
  }
}

async function onDeletePerson(r) {
  if (!confirm("确定要删除 " + r.personNo + " 吗？")) return
  try {
    await apiFetch("/api/admin/persons/" + r.id, { method: "DELETE" })
    showToast("已删除", r.personNo, "success")
    await refreshPersons()
  } catch (e) {
    showToast("删除失败", e.message, "danger")
  }
}

async function onAddPerson() {
  const personNo = window.prompt("工号：")
  if (!personNo) return
  const fullName = window.prompt("姓名：")
  if (!fullName) return
  const idLast4 = window.prompt("身份证后四位：")
  if (!idLast4) return
  const airline = window.prompt("航司：")
  const positionTitle = window.prompt("职位：")
  const department = window.prompt("部门：")
  const phone = window.prompt("电话：")

  try {
    await apiFetch("/api/admin/persons", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        personNo, fullName, idLast4, airline, positionTitle, department, phone
      })
    })
    showToast("已添加", personNo, "success")
    await refreshPersons()
  } catch (e) {
    showToast("添加失败", e.message, "danger")
  }
}

async function onLogout() {
  try {
    await apiFetch("/api/auth/logout", { method: "POST" })
    location.href = "/auth"
  } catch (e) {
    showToast("退出失败", e.message, "danger")
  }
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

function cellText(v) {
  if (v == null) return ""
  if (typeof v === "object") return JSON.stringify(v)
  return String(v)
}

function buildQarColumns(rows) {
  if (qarPreviewColumns && qarPreviewColumns.length) {
    const cols = ["id", "createdAt", ...qarPreviewColumns]
    return Array.from(new Set(cols))
  }
  const cols = ["id", "createdAt"]
  for (const r of rows) {
    const d = r.data || {}
    for (const k of Object.keys(d)) {
      if (!cols.includes(k)) cols.push(k)
    }
  }
  return cols
}

function renderQarPreview(preview) {
  const thead = document.querySelector("#qar-preview-table thead")
  const tbody = document.querySelector("#qar-preview-table tbody")
  thead.innerHTML = ""
  tbody.innerHTML = ""
  const cols = preview.columns || []
  const rows = preview.rows || []
  qarPreviewColumns = cols
  document.getElementById("qar-preview-info").textContent = cols.length ? `列数 ${cols.length} · 预览行数 ${rows.length}` : "未解析到数据"
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

function setQarSortHeader(thead) {
  const ths = thead.querySelectorAll("th.sortable")
  ths.forEach(th => {
    th.classList.remove("asc", "desc")
    if (th.dataset.sort === qarSortBy) th.classList.add(qarSortDir === "desc" ? "desc" : "asc")
  })
}

async function refreshQar() {
  try {
    const rows = await TransportCrypto.fetch(`/api/admin/qar-table/rows?sortBy=${encodeURIComponent(qarSortBy)}&sortDir=${encodeURIComponent(qarSortDir)}`, { method: "GET" })
    qarRows = rows || []
    qarColumns = buildQarColumns(qarRows)
    renderQarTable()
  } catch (e) {
    showToast("加载QAR表格失败", e.message, "danger")
  }
}

function renderQarTable() {
  const table = document.getElementById("qar-table")
  const thead = table.querySelector("thead")
  const tbody = table.querySelector("tbody")
  tbody.innerHTML = ""
  document.getElementById("qar-empty-msg").style.display = qarRows.length ? "none" : "block"

  thead.innerHTML = ""
  const trh = document.createElement("tr")
  for (const c of qarColumns) {
    const th = document.createElement("th")
    th.textContent = c
    th.dataset.sort = c
    th.className = "sortable"
    th.addEventListener("click", async () => {
      if (qarSortBy === c) qarSortDir = qarSortDir === "asc" ? "desc" : "asc"
      else { qarSortBy = c; qarSortDir = "asc" }
      await refreshQar()
    })
    trh.appendChild(th)
  }
  thead.appendChild(trh)
  setQarSortHeader(thead)

  for (const r of qarRows) {
    const tr = document.createElement("tr")
    for (const c of qarColumns) {
      const td = document.createElement("td")
      if (c === "id") td.textContent = r.id || ""
      else if (c === "createdAt") td.textContent = (r.createdAt || "").replace("T", " ").replace("Z", "")
      else td.textContent = cellText((r.data || {})[c])
      tr.appendChild(td)
    }
    tbody.appendChild(tr)
  }
}

async function onQarSaveRow() {
  const raw = (document.getElementById("qar-row-json").value || "").trim()
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
  const id = (document.getElementById("qar-row-id").value || "").trim()
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
    await refreshQar()
  } catch (e) {
    showToast("保存失败", e.message, "danger")
  }
}

async function onQarPreviewXlsx() {
  const fileInput = document.getElementById("qar-xlsx-file")
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
    renderQarPreview(preview)
    showToast("预览完成", "已解析xlsx", "success")
  } catch (e) {
    showToast("预览失败", e.message, "danger")
  }
}

async function onQarImportXlsx() {
  const fileInput = document.getElementById("qar-xlsx-file")
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
    await refreshQar()
  } catch (e) {
    showToast("导入失败", e.message, "danger")
  }
}

async function main() {
  const me = await ensureAdmin()
  if (!me) return

  document.getElementById("btn-logout").addEventListener("click", onLogout)

  // Person event listeners
  document.getElementById("btn-refresh-persons").addEventListener("click", refreshPersons)
  document.getElementById("btn-add-person").addEventListener("click", onAddPerson)
  document.getElementById("search-keyword").addEventListener("input", applyPersonFiltersAndSort)
  document.getElementById("filter-date-start").addEventListener("change", applyPersonFiltersAndSort)
  document.getElementById("filter-date-end").addEventListener("change", applyPersonFiltersAndSort)
  document.getElementById("btn-reset-filters").addEventListener("click", () => {
    document.getElementById("search-keyword").value = "";
    document.getElementById("filter-date-start").value = "";
    document.getElementById("filter-date-end").value = "";
    applyPersonFiltersAndSort();
  });

  document.querySelectorAll("table#persons th.sortable").forEach(th => {
    th.addEventListener("click", () => {
      const field = th.dataset.sort;
      if (personSortField === field) {
        personSortOrder = personSortOrder === 'asc' ? 'desc' : 'asc';
      } else {
        personSortField = field;
        personSortOrder = 'asc';
      }
      document.querySelectorAll("table#persons th.sortable").forEach(t => t.className = "sortable");
      th.classList.add(personSortOrder);
      applyPersonFiltersAndSort();
    });
  });

  await refreshPersons()
}

main()
