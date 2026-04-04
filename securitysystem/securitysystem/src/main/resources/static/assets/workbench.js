async function ensureMe() {
  await initCsrf()
  const me = await loadMe()
  if (!me) {
    location.href = "/auth"
    return null
  }
  document.getElementById("me-pill").textContent = me.emailOrUsername + " · " + me.role
  const pfName = document.getElementById("pf-name")
  const pfNo = document.getElementById("pf-no")
  const pfAirline = document.getElementById("pf-airline")
  const pfPos = document.getElementById("pf-position")
  const pfDept = document.getElementById("pf-dept")
  if (pfName) pfName.value = me.fullName || "-"
  if (pfNo) pfNo.value = me.personNo || me.emailOrUsername || "-"
  if (pfAirline) pfAirline.value = me.airline || "-"
  if (pfPos) pfPos.value = me.positionTitle || "-"
  if (pfDept) pfDept.value = me.department || ""
  if (me.role === "admin") {
    document.getElementById("admin-link").style.display = "inline"
  } else {
    const uc = document.getElementById("upload-card")
    if (uc) uc.style.display = "none"
    const grid = document.getElementById("grid")
    if (grid) grid.style.gridTemplateColumns = "1fr"
  }
  return me
}

async function refreshList() {
  const rows = await apiFetch("/api/files", { method: "GET" })
  const tbody = document.querySelector("#tbl tbody")
  tbody.innerHTML = ""
  document.getElementById("empty").style.display = rows.length ? "none" : "block"
  for (const r of rows) {
    const tr = document.createElement("tr")
    tr.innerHTML = "<td></td><td></td><td></td><td></td><td class='actions'></td>"
    tr.children[0].textContent = r.originalName
    tr.children[1].textContent = fmtBytes(r.sizeBytes)
    tr.children[2].textContent = r.policy || "-"
    tr.children[3].textContent = (r.createdAt || "").replace("T", " ").replace("Z", "")
    const a = document.createElement("a")
    a.className = "btn"
    a.textContent = "下载"
    a.href = "/api/files/" + r.id + "/download"
    tr.children[4].appendChild(a)
    tbody.appendChild(tr)
  }
}

async function onUpload() {
  const fileInput = document.getElementById("file")
  const policy = document.getElementById("policy").value
  const btn = document.getElementById("btn-upload")
  const out = document.getElementById("upload-result")
  if (!fileInput.files || !fileInput.files.length) {
    showToast("缺少文件", "请选择一个文件", "danger")
    return
  }
  btn.disabled = true
  out.textContent = ""
  try {
    const fd = new FormData()
    fd.append("file", fileInput.files[0])
    if (policy) fd.append("policy", policy)
    const resp = await apiFetch("/api/files", { method: "POST", body: fd, headers: {} })
    localStorage.setItem("qar_last_upload_id", resp.id)
    showToast("上传成功", "已保存：" + resp.id, "success")
    out.innerHTML = "记录ID：<b></b> · 策略：<span></span> · <a class='link' href='/feedback?fileId=" + encodeURIComponent(resp.id) + "&subject=" + encodeURIComponent("关于记录 " + resp.id + " 的问题") + "'>就此记录提交反馈</a>"
    out.querySelector("b").textContent = resp.id
    out.querySelector("span").textContent = resp.policy || "-"
    fileInput.value = ""
    await refreshList()
  } catch (e) {
    showToast("上传失败", e.message, "danger")
  } finally {
    btn.disabled = false
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

async function onSaveDept() {
  const btn = document.getElementById("btn-save-dept")
  const input = document.getElementById("pf-dept")
  if (!btn || !input) return
  const dept = (input.value || "").trim()
  btn.disabled = true
  try {
    await apiFetch("/api/profile/department", {
      method: "PATCH",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ department: dept })
    })
    showToast("已保存", "部门信息已更新", "success")
    const me = await loadMe()
    if (me && input) input.value = me.department || dept
  } catch (e) {
    showToast("保存失败", e.message, "danger")
  } finally {
    btn.disabled = false
  }
}

async function main() {
  const me = await ensureMe()
  if (!me) return
  if (me.role === "admin") {
    document.getElementById("btn-upload").addEventListener("click", onUpload)
  }
  document.getElementById("btn-save-dept").addEventListener("click", onSaveDept)
  document.getElementById("btn-logout").addEventListener("click", onLogout)
  await refreshList()
}

main()
