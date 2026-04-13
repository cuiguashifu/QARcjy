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
  const mePill = document.getElementById("me-pill")
  if (mePill) {
    mePill.textContent = me.emailOrUsername + " · " + me.role
  }
  const mePillSidebar = document.getElementById("me-pill-sidebar")
  if (mePillSidebar) {
    mePillSidebar.textContent = me.emailOrUsername + " · " + me.role
  }
  // 重新初始化侧边栏菜单，确保管理后台菜单的展开/收起功能正常工作
  if (window.jQuery && jQuery.fn.sidebarMenu) {
    jQuery('.sidebar-menu').sidebarMenu();
  }
  return me
}

async function refreshUsers() {
  const users = await apiFetch("/api/admin/users", { method: "GET" })
  const tbody = document.querySelector("#users tbody")
  tbody.innerHTML = ""
  for (const u of users) {
    const tr = document.createElement("tr")
    tr.innerHTML = "<td></td><td></td><td></td>"
    tr.children[0].textContent = u.emailOrUsername
    tr.children[1].textContent = u.role
    tr.children[2].textContent = (u.createdAt || "").replace("T", " ").replace("Z", "")
    tbody.appendChild(tr)
  }
}

async function refreshFiles() {
  const rows = await apiFetch("/api/admin/files", { method: "GET" })
  const tbody = document.querySelector("#files tbody")
  tbody.innerHTML = ""
  for (const r of rows) {
    const tr = document.createElement("tr")
    tr.innerHTML = "<td></td><td></td><td></td><td></td><td></td><td></td><td class='actions'></td>"
    tr.children[0].textContent = r.id
    tr.children[1].textContent = r.ownerLabel || r.ownerId
    tr.children[2].textContent = r.originalName
    tr.children[3].textContent = fmtBytes(r.sizeBytes)
    tr.children[4].textContent = r.policy || "-"
    tr.children[5].textContent = (r.createdAt || "").replace("T", " ").replace("Z", "")
    const a = document.createElement("a")
    a.className = "btn"
    a.textContent = "下载"
    a.href = "/api/files/" + r.id + "/download"
    tr.children[6].appendChild(a)
    tbody.appendChild(tr)
  }
}

async function refreshRequests() {
  const rows = await apiFetch("/api/admin/account-requests", { method: "GET" })
  const tbody = document.querySelector("#requests tbody")
  tbody.innerHTML = ""
  for (const r of rows) {
    const tr = document.createElement("tr")
    tr.innerHTML = "<td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td class='actions'></td>"
    tr.children[0].textContent = r.id
    tr.children[1].textContent = r.personNo
    tr.children[2].textContent = r.fullName
    tr.children[3].textContent = r.airline
    tr.children[4].textContent = r.positionTitle
    tr.children[5].textContent = r.department
    tr.children[6].textContent = r.contact
    tr.children[7].textContent = (r.createdAt || "").replace("T", " ").replace("Z", "")

    const btnApprove = document.createElement("button")
    btnApprove.className = "btn primary"
    btnApprove.textContent = "通过"
    btnApprove.addEventListener("click", async () => {
      const note = window.prompt("审批备注（可选）：", "")
      if (note === null) return
      try {
        await apiFetch("/api/admin/account-requests/" + r.id + "/approve", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ adminNote: note })
        })
        showToast("已通过", "账号已创建：" + r.personNo, "success")
        await refreshRequests()
        await refreshUsers()
      } catch (e) {
        showToast("操作失败", e.message, "danger")
      }
    })

    const btnReject = document.createElement("button")
    btnReject.className = "btn"
    btnReject.textContent = "拒绝"
    btnReject.addEventListener("click", async () => {
      const note = window.prompt("拒绝原因（可选）：", "")
      if (note === null) return
      try {
        await apiFetch("/api/admin/account-requests/" + r.id + "/reject", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ adminNote: note })
        })
        showToast("已拒绝", r.personNo, "success")
        await refreshRequests()
      } catch (e) {
        showToast("操作失败", e.message, "danger")
      }
    })

    tr.children[8].appendChild(btnApprove)
    tr.children[8].appendChild(btnReject)
    tbody.appendChild(tr)
  }
}

async function refreshLogs() {
  const rows = await apiFetch("/api/admin/audit-logs", { method: "GET" })
  const tbody = document.querySelector("#logs tbody")
  tbody.innerHTML = ""
  for (const r of rows) {
    const tr = document.createElement("tr")
    tr.innerHTML = "<td></td><td></td><td></td><td></td><td></td><td></td><td></td>"
    tr.children[0].textContent = (r.createdAt || "").replace("T", " ").replace("Z", "")
    tr.children[1].textContent = r.personNo || "-"
    tr.children[2].textContent = r.method || "-"
    tr.children[3].textContent = r.path || "-"
    tr.children[4].textContent = String(r.statusCode || "")
    tr.children[5].textContent = (r.durationMs != null ? (r.durationMs + "ms") : "-")
    tr.children[6].textContent = r.ip || "-"
    tbody.appendChild(tr)
  }
}

function statusBadge(status) {
  const s = (status || "").toLowerCase()
  if (s === "resolved") return "<span class='badge ok'>已解决</span>"
  if (s === "in_progress") return "<span class='badge'>处理中</span>"
  return "<span class='badge warn'>新反馈</span>"
}

async function refreshFeedback() {
  const rows = await apiFetch("/api/admin/feedback", { method: "GET" })
  const tbody = document.querySelector("#feedback tbody")
  tbody.innerHTML = ""
  for (const r of rows) {
    const tr = document.createElement("tr")
    tr.innerHTML = "<td></td><td></td><td></td><td></td><td></td><td></td><td class='actions'></td>"
    tr.children[0].textContent = r.id
    tr.children[1].textContent = r.ownerId
    tr.children[2].textContent = r.subject || (r.message || "").slice(0, 18) || "(无主题)"
    tr.children[3].textContent = r.type || "-"
    tr.children[4].innerHTML = statusBadge(r.status)
    tr.children[5].textContent = (r.updatedAt || r.createdAt || "").replace("T", " ").replace("Z", "")

    const btnReply = document.createElement("button")
    btnReply.className = "btn"
    btnReply.textContent = "回复"
    btnReply.addEventListener("click", async () => {
      const v = window.prompt("输入回复内容（留空表示清除回复）：", r.adminReply || "")
      if (v === null) return
      try {
        await apiFetch("/api/admin/feedback/" + r.id, {
          method: "PATCH",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ adminReply: v })
        })
        showToast("已更新", "回复已保存", "success")
        await refreshFeedback()
      } catch (e) {
        showToast("操作失败", e.message, "danger")
      }
    })

    const btnInProgress = document.createElement("button")
    btnInProgress.className = "btn"
    btnInProgress.textContent = "标记处理中"
    btnInProgress.addEventListener("click", async () => {
      try {
        await apiFetch("/api/admin/feedback/" + r.id, {
          method: "PATCH",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ status: "IN_PROGRESS" })
        })
        await refreshFeedback()
      } catch (e) {
        showToast("操作失败", e.message, "danger")
      }
    })

    const btnResolved = document.createElement("button")
    btnResolved.className = "btn primary"
    btnResolved.textContent = "标记已解决"
    btnResolved.addEventListener("click", async () => {
      try {
        await apiFetch("/api/admin/feedback/" + r.id, {
          method: "PATCH",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ status: "RESOLVED" })
        })
        await refreshFeedback()
      } catch (e) {
        showToast("操作失败", e.message, "danger")
      }
    })

    tr.children[6].appendChild(btnReply)
    tr.children[6].appendChild(btnInProgress)
    tr.children[6].appendChild(btnResolved)
    tbody.appendChild(tr)
  }
}

async function onExport() {
  showToast("开始导出", "服务器将解密并打包为zip", "success")
  window.location.href = "/api/admin/files/export"
}

async function onLogout() {
  const btn = document.getElementById("btn-logout-sidebar")
  if (!btn) return
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
  
  const btnExport = document.getElementById("btn-export")
  if (btnExport) {
    btnExport.addEventListener("click", onExport)
  }
  const btnLogout = document.getElementById("btn-logout-sidebar")
  if (btnLogout) {
    btnLogout.addEventListener("click", onLogout)
  }
  const btnRefreshFeedback = document.getElementById("btn-refresh-feedback")
  if (btnRefreshFeedback) {
    btnRefreshFeedback.addEventListener("click", refreshFeedback)
  }
  const btnRefreshRequests = document.getElementById("btn-refresh-requests")
  if (btnRefreshRequests) {
    btnRefreshRequests.addEventListener("click", refreshRequests)
  }
  const btnRefreshLogs = document.getElementById("btn-refresh-logs")
  if (btnRefreshLogs) {
    btnRefreshLogs.addEventListener("click", refreshLogs)
  }
  
  await refreshUsers()
  await refreshRequests()
  await refreshFiles()
  await refreshFeedback()
  await refreshLogs()
}

main()
