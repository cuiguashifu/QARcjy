async function apiFetch(path, opts) {
  const csrf = window.__csrf || null
  const headers = Object.assign({"Accept": "application/json"}, opts && opts.headers ? opts.headers : {})
  if (csrf && csrf.headerName && csrf.token) {
    headers[csrf.headerName] = csrf.token
  }
  const res = await fetch(path, Object.assign({ credentials: "include", headers }, opts || {}))
  const ct = res.headers.get("content-type") || ""
  const data = ct.includes("application/json") ? await res.json().catch(() => null) : await res.text().catch(() => "")
  if (!res.ok) {
    const msg = data && data.message ? data.message : (typeof data === "string" ? data : "request_failed")
    throw new Error(mapErrorMessage(msg))
  }
  return data
}

function mapErrorMessage(msg) {
  const m = (msg || "").trim()
  const map = {
    "emailOrUsername_required": "请输入用户名",
    "fullName_required": "请输入姓名",
    "idLast4_required": "请输入身份证后四位",
    "contact_required": "请输入联系方式",
    "airline_required": "请输入航司",
    "position_required": "请输入所属职位",
    "department_required": "请输入所属部门",
    "password_required": "请输入密码",
    "password_confirm_mismatch": "两次密码不一致",
    "user_already_exists": "该用户名已被注册",
    "admin_already_exists": "Admin 为预置唯一账号，无法重复注册",
    "profile_not_found": "档案库中未找到该学号/工号，请核对信息或联系管理员",
    "profile_mismatch": "个人信息与档案库不匹配，请核对姓名/身份证后四位/联系方式/航司/职位/部门",
    "request_already_pending": "申请已提交，请等待管理员审核",
    "request_not_pending": "该申请已处理",
    "account_pending": "账号申请审核中，暂不可登录",
    "invalid_credentials": "账号或密码错误",
    "unauthorized": "未登录或会话已过期，请重新登录",
    "forbidden": "无权限访问",
    "message_required": "请填写反馈内容",
    "message_too_long": "反馈内容过长",
    "request_failed": "请求失败，请稍后重试"
  }
  return map[m] || m || "请求失败"
}

function showToast(title, message, variant) {
  const el = document.getElementById("toast")
  if (!el) return
  el.classList.add("show")
  const t = el.querySelector(".t")
  const m = el.querySelector(".m")
  t.textContent = title
  m.textContent = message
  el.style.borderColor = variant === "danger" ? "rgba(220,38,38,.35)" : (variant === "success" ? "rgba(22,163,74,.35)" : "var(--border)")
  window.clearTimeout(window.__toastTimer)
  window.__toastTimer = window.setTimeout(() => el.classList.remove("show"), 2800)
}

async function initCsrf() {
  try {
    const token = await apiFetch("/api/csrf", { method: "GET" })
    window.__csrf = token
  } catch (e) {
    window.__csrf = null
  }
}

async function loadMe() {
  try {
    return await apiFetch("/api/auth/me", { method: "GET" })
  } catch (e) {
    return null
  }
}

function fmtBytes(n) {
  if (!Number.isFinite(n)) return "-"
  const units = ["B","KB","MB","GB"]
  let v = n
  let i = 0
  while (v >= 1024 && i < units.length - 1) {
    v = v / 1024
    i++
  }
  const p = i === 0 ? 0 : 1
  return v.toFixed(p) + " " + units[i]
}
