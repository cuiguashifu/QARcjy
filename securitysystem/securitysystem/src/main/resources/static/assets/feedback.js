function qs(name) {
  const u = new URL(location.href)
  return u.searchParams.get(name)
}

function statusBadge(status) {
  const s = (status || "").toLowerCase()
  if (s === "resolved") return "<span class='badge ok'>已解决</span>"
  if (s === "in_progress") return "<span class='badge'>处理中</span>"
  return "<span class='badge warn'>新反馈</span>"
}

async function ensureMe() {
  await initCsrf()
  const me = await loadMe()
  if (!me) {
    location.href = "/auth"
    return null
  }
  document.getElementById("me-pill").textContent = me.emailOrUsername + " · " + me.role
  if (me.role === "admin") {
    document.getElementById("admin-link").style.display = "inline"
    document.getElementById("admin-data-link").style.display = "inline"
  }
  return me
}

async function refreshList() {
  const rows = await apiFetch("/api/feedback", { method: "GET" })
  const tbody = document.querySelector("#tbl tbody")
  tbody.innerHTML = ""
  document.getElementById("empty").style.display = rows.length ? "none" : "block"
  for (const r of rows) {
    const tr = document.createElement("tr")
    tr.innerHTML = "<td></td><td></td><td></td><td></td><td></td>"
    const title = r.subject || (r.message || "").slice(0, 16) || "(无主题)"
    tr.children[0].textContent = title
    tr.children[1].textContent = r.type || "-"
    tr.children[2].innerHTML = statusBadge(r.status)
    tr.children[3].textContent = r.relatedFileId || "-"
    tr.children[4].textContent = (r.updatedAt || r.createdAt || "").replace("T", " ").replace("Z", "")
    tbody.appendChild(tr)
  }
}

function fillFromQueryOrLast() {
  const fileId = qs("fileId") || localStorage.getItem("qar_last_upload_id") || ""
  if (fileId) {
    document.getElementById("relatedFileId").value = fileId
  }
  const preset = qs("subject")
  if (preset) {
    document.getElementById("subject").value = preset
  }
}

async function onSubmit() {
  const btn = document.getElementById("btn-submit")
  const hint = document.getElementById("submit-hint")
  btn.disabled = true
  hint.textContent = ""
  try {
    const payload = {
      type: document.getElementById("type").value,
      contact: document.getElementById("contact").value || null,
      subject: document.getElementById("subject").value || null,
      relatedFileId: document.getElementById("relatedFileId").value || null,
      message: document.getElementById("message").value || ""
    }
    const resp = await apiFetch("/api/feedback", { method: "POST", body: JSON.stringify(payload), headers: { "Content-Type": "application/json" } })
    showToast("提交成功", "反馈ID：" + resp.id, "success")
    hint.textContent = "我们已收到你的反馈，状态为“新反馈”。你可以在右侧看到处理进展。"
    document.getElementById("message").value = ""
    await refreshList()
  } catch (e) {
    showToast("提交失败", e.message, "danger")
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

function onFillLast() {
  const id = localStorage.getItem("qar_last_upload_id") || ""
  if (!id) {
    showToast("没有记录", "请先在工作台上传一次数据", "danger")
    return
  }
  document.getElementById("relatedFileId").value = id
  showToast("已填入", "关联记录ID：" + id, "success")
}

async function onCopyEnv() {
  const me = await loadMe()
  const env = {
    user: me ? me.emailOrUsername : "-",
    role: me ? me.role : "-",
    time: new Date().toISOString(),
    ua: navigator.userAgent,
    lastUploadId: localStorage.getItem("qar_last_upload_id") || ""
  }
  const text = Object.entries(env).map(([k, v]) => k + ": " + v).join("\n")
  try {
    await navigator.clipboard.writeText(text)
    showToast("已复制", "环境信息已复制到剪贴板", "success")
  } catch (e) {
    showToast("复制失败", "浏览器不支持剪贴板权限", "danger")
  }
}

async function main() {
  const me = await ensureMe()
  if (!me) return
  fillFromQueryOrLast()
  document.getElementById("btn-submit").addEventListener("click", onSubmit)
  document.getElementById("btn-logout").addEventListener("click", onLogout)
  document.getElementById("btn-refresh").addEventListener("click", refreshList)
  document.getElementById("btn-fill-last").addEventListener("click", onFillLast)
  document.getElementById("btn-copy-env").addEventListener("click", onCopyEnv)
  await refreshList()
}

main()

