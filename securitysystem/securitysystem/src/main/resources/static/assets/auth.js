async function switchTab(which) {
  document.getElementById("tab-login").classList.toggle("active", which === "login")
  document.getElementById("tab-register").classList.toggle("active", which === "register")
  document.getElementById("panel-login").style.display = which === "login" ? "flex" : "none"
  document.getElementById("panel-register").style.display = which === "register" ? "flex" : "none"
}

async function refreshMe() {
  const me = await loadMe()
  const logged = document.getElementById("logged-in")
  if (me && me.id) {
    logged.style.display = "flex"
    document.getElementById("me-badge").textContent = "已登录：" + me.emailOrUsername + "（" + me.role + "）"
    document.getElementById("panel-login").style.display = "none"
    document.getElementById("panel-register").style.display = "none"
    return
  }
  logged.style.display = "none"
  await switchTab("login")
}

async function onLogin() {
  const u = (document.getElementById("login-username").value || "").trim()
  const p = document.getElementById("login-password").value || ""
  const btn = document.getElementById("btn-login")
  if (!u) {
    showToast("缺少学号/工号", "请输入学号/工号", "danger")
    return
  }
  if (!p) {
    showToast("缺少密码", "请输入密码", "danger")
    return
  }
  btn.disabled = true
  try {
    await apiFetch("/api/auth/login", {
      method: "POST",
      headers: {"Content-Type": "application/json"},
      body: JSON.stringify({ emailOrUsername: u, password: p })
    })
    showToast("登录成功", "会话 Cookie 已写入", "success")
    location.href = "/workbench"
  } catch (e) {
    showToast("登录失败", e.message, "danger")
  } finally {
    btn.disabled = false
  }
}

async function onRegister() {
  const u = (document.getElementById("reg-username").value || "").trim()
  const fullName = (document.getElementById("reg-fullname").value || "").trim()
  const idLast4 = (document.getElementById("reg-idlast4").value || "").trim()
  const contact = (document.getElementById("reg-contact").value || "").trim()
  const airline = (document.getElementById("reg-airline").value || "").trim()
  const positionTitle = (document.getElementById("reg-position").value || "").trim()
  const department = (document.getElementById("reg-dept").value || "").trim()
  const p = document.getElementById("reg-password").value || ""
  const p2 = document.getElementById("reg-password2").value || ""
  const btn = document.getElementById("btn-register")
  if (!u) {
    showToast("缺少学号/工号", "请输入学号/工号", "danger")
    return
  }
  if (!fullName) {
    showToast("缺少姓名", "请输入姓名", "danger")
    return
  }
  if (!idLast4 || idLast4.length !== 4) {
    showToast("身份证后四位", "请输入 4 位数字", "danger")
    return
  }
  if (!contact) {
    showToast("缺少联系方式", "请输入联系方式", "danger")
    return
  }
  if (!airline) {
    showToast("缺少航司", "请输入航司", "danger")
    return
  }
  if (!positionTitle) {
    showToast("缺少职位", "请输入所属职位", "danger")
    return
  }
  if (!department) {
    showToast("缺少部门", "请输入所属部门", "danger")
    return
  }
  if (!p) {
    showToast("缺少密码", "请输入密码", "danger")
    return
  }
  if (p !== p2) {
    showToast("两次密码不一致", "请确认两次输入一致", "danger")
    return
  }
  btn.disabled = true
  try {
    const res = await apiFetch("/api/auth/register", {
      method: "POST",
      headers: {"Content-Type": "application/json"},
      body: JSON.stringify({ emailOrUsername: u, fullName, idLast4, contact, airline, positionTitle, department, password: p, passwordConfirm: p2 })
    })
    
    // Download private key
    if (res.privateKey) {
      const blob = new Blob([res.privateKey], { type: "text/plain" });
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `private_key_${u}.pem`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
      showToast("申请已提交", "请妥善保管自动下载的私钥文件，审核通过后需使用该私钥解密下载数据", "success")
    } else {
      showToast("申请已提交", "请等待管理员审核通过后再登录", "success")
    }
    
    await switchTab("login")
  } catch (e) {
    showToast("注册失败", e.message, "danger")
  } finally {
    btn.disabled = false
  }
}

async function onLogout() {
  const btn = document.getElementById("btn-logout")
  btn.disabled = true
  try {
    await apiFetch("/api/auth/logout", { method: "POST" })
    showToast("已退出", "会话已失效", "success")
    await refreshMe()
  } catch (e) {
    showToast("退出失败", e.message, "danger")
  } finally {
    btn.disabled = false
  }
}

async function main() {
  await initCsrf()
  document.getElementById("tab-login").addEventListener("click", () => switchTab("login"))
  document.getElementById("tab-register").addEventListener("click", () => switchTab("register"))
  document.getElementById("btn-login").addEventListener("click", onLogin)
  document.getElementById("btn-register").addEventListener("click", onRegister)
  document.getElementById("btn-logout").addEventListener("click", onLogout)
  await refreshMe()
}

main()
