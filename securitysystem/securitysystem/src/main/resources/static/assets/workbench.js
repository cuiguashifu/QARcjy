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

async function checkLocalCryptoService() {
  const statusEl = document.getElementById("crypto-status")
  if (!statusEl) return false
  
  statusEl.textContent = "检查中..."
  statusEl.className = "muted"
  
  try {
    const result = await LocalCrypto.checkService()
    if (result.available) {
      statusEl.textContent = "✓ 本地加密服务已连接"
      statusEl.className = "badge ok"
      return true
    } else {
      statusEl.textContent = "✗ 本地加密服务未启动"
      statusEl.className = "badge danger"
      return false
    }
  } catch (e) {
    statusEl.textContent = "✗ 无法连接本地加密服务"
    statusEl.className = "badge danger"
    return false
  }
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
    
    const downloadBtn = document.createElement("button")
    downloadBtn.className = "btn"
    downloadBtn.textContent = "下载"
    downloadBtn.onclick = () => onDownload(r.id, r.originalName, r.wrappedKey, r.policy)
    tr.children[4].appendChild(downloadBtn)
    
    tbody.appendChild(tr)
  }
}

async function onUpload() {
  const fileInput = document.getElementById("file")
  const policy = document.getElementById("policy").value || "role:user"
  const btn = document.getElementById("btn-upload")
  const out = document.getElementById("upload-result")
  
  if (!fileInput.files || !fileInput.files.length) {
    showToast("缺少文件", "请选择一个文件", "danger")
    return
  }
  
  const me = await loadMe()
  console.log("用户信息:", me)
  console.log("角色类型:", typeof me?.role, "角色值:", me?.role)
  if (!me || me.role !== "admin") {
    showToast("权限不足", "只有管理员可以上传文件，当前角色: " + (me ? me.role : "未登录"), "danger")
    return
  }
  
  await initCsrf()
  
  const serviceAvailable = await checkLocalCryptoService()
  if (!serviceAvailable) {
    showToast("服务未就绪", "请先启动本地加密服务 (运行 start.bat)", "danger")
    return
  }
  
  btn.disabled = true
  out.textContent = "正在加密..."
  
  try {
    const file = fileInput.files[0]
    out.textContent = "正在读取文件..."
    const fileBase64 = await LocalCrypto.fileToBase64(file)
    
    out.textContent = "正在加密..."
    const encryptResult = await LocalCrypto.encrypt(fileBase64, policy)
    
    if (encryptResult.code !== 200) {
      throw new Error(encryptResult.message || "加密失败")
    }
    
    out.textContent = "正在上传密文..."
    const uploadData = {
      encryptedData: encryptResult.encryptedData,
      wrappedKey: encryptResult.wrappedKey,
      originalName: file.name,
      contentType: file.type || "application/octet-stream",
      sizeBytes: file.size,
      policy: policy
    }
    
    console.log("CSRF Token:", window.__csrf)
    console.log("Upload data:", uploadData)
    
    const resp = await apiFetch("/api/files/encrypted", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(uploadData)
    })
    
    localStorage.setItem("qar_last_upload_id", resp.id)
    showToast("上传成功", "已加密保存：" + resp.id, "success")
    out.innerHTML = "记录ID：<b></b> · 策略：<span></span> · <a class='link' href='/feedback?fileId=" + encodeURIComponent(resp.id) + "&subject=" + encodeURIComponent("关于记录 " + resp.id + " 的问题") + "'>就此记录提交反馈</a>"
    out.querySelector("b").textContent = resp.id
    out.querySelector("span").textContent = resp.policy || "-"
    fileInput.value = ""
    await refreshList()
  } catch (e) {
    showToast("上传失败", e.message, "danger")
    out.textContent = ""
  } finally {
    btn.disabled = false
  }
}

async function onDownload(fileId, originalName, wrappedKey, policy) {
  const serviceAvailable = await checkLocalCryptoService()
  if (!serviceAvailable) {
    showToast("服务未就绪", "请先启动本地加密服务", "danger")
    return
  }
  
  try {
    showToast("下载中", "正在获取密文...", "info")
    
    const resp = await apiFetch(`/api/files/${fileId}/encrypted`, { method: "GET" })
    
    showToast("解密中", "正在解密文件...", "info")
    
    const decryptResult = await LocalCrypto.decrypt(
      resp.encryptedData,
      resp.wrappedKey || wrappedKey,
      resp.policy || policy
    )
    
    if (decryptResult.code !== 200) {
      throw new Error(decryptResult.message || "解密失败")
    }
    
    const blob = LocalCrypto.base64ToBlob(decryptResult.decryptedData, resp.contentType)
    LocalCrypto.downloadBlob(blob, originalName || "download.bin")
    
    showToast("下载完成", "文件已解密并保存", "success")
  } catch (e) {
    showToast("下载失败", e.message, "danger")
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
  
  await checkLocalCryptoService()
  
  if (me.role === "admin") {
    document.getElementById("btn-upload").addEventListener("click", onUpload)
  }
  document.getElementById("btn-save-dept").addEventListener("click", onSaveDept)
  document.getElementById("btn-logout").addEventListener("click", onLogout)
  await refreshList()
}

main()
