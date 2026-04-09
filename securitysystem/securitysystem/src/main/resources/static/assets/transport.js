(() => {
  const enc = new TextEncoder()
  const dec = new TextDecoder()

  function b64ToBuf(b64) {
    const bin = atob(b64)
    const bytes = new Uint8Array(bin.length)
    for (let i = 0; i < bin.length; i++) bytes[i] = bin.charCodeAt(i)
    return bytes.buffer
  }

  function bufToB64(buf) {
    const bytes = new Uint8Array(buf)
    let bin = ""
    for (let i = 0; i < bytes.length; i++) bin += String.fromCharCode(bytes[i])
    return btoa(bin)
  }

  function aadFor(method, path) {
    const p = (path || "").split("?")[0]
    const uri = p.startsWith("http") ? (new URL(p)).pathname : p
    return enc.encode((method || "GET").toUpperCase() + " " + uri)
  }

  async function genRsa() {
    return await crypto.subtle.generateKey(
      { name: "RSA-OAEP", modulusLength: 2048, publicExponent: new Uint8Array([1, 0, 1]), hash: "SHA-256" },
      true,
      ["encrypt", "decrypt"]
    )
  }

  const state = {
    protocol: "tls",
    suite: "TLS_AES_256_GCM_SHA256",
    sessionId: null,
    aesKey: null,
    rsa: null,
    expiresAt: null
  }

  async function ensureSession() {
    if (state.sessionId && state.aesKey) return
    if (!state.rsa) state.rsa = await genRsa()
    const spki = await crypto.subtle.exportKey("spki", state.rsa.publicKey)
    const handshake = await apiFetch("/api/transport/handshake", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ clientPublicKey: bufToB64(spki), protocol: state.protocol })
    })
    const wrappedKeyBuf = b64ToBuf(handshake.wrappedKey)
    const rawAes = await crypto.subtle.decrypt({ name: "RSA-OAEP" }, state.rsa.privateKey, wrappedKeyBuf)
    state.aesKey = await crypto.subtle.importKey("raw", rawAes, { name: "AES-GCM" }, false, ["encrypt", "decrypt"])
    state.sessionId = handshake.sessionId
    state.expiresAt = handshake.expiresAt
  }

  async function encryptBody(method, path, bodyStr) {
    const iv = crypto.getRandomValues(new Uint8Array(12))
    const aad = aadFor(method, path)
    const pt = enc.encode(bodyStr || "")
    const ct = await crypto.subtle.encrypt({ name: "AES-GCM", iv, additionalData: aad }, state.aesKey, pt)
    return { iv: bufToB64(iv.buffer), ciphertext: bufToB64(ct) }
  }

  async function decryptBody(method, path, envelope) {
    const iv = new Uint8Array(b64ToBuf(envelope.iv))
    const ct = b64ToBuf(envelope.ciphertext)
    const aad = aadFor(method, path)
    const pt = await crypto.subtle.decrypt({ name: "AES-GCM", iv, additionalData: aad }, state.aesKey, ct)
    return dec.decode(pt || new ArrayBuffer(0))
  }

  async function fetchEncrypted(path, opts) {
    await ensureSession()
    const method = (opts && opts.method ? opts.method : "GET").toUpperCase()
    const headers = Object.assign({ "Accept": "application/json" }, (opts && opts.headers) ? opts.headers : {})
    const csrf = window.__csrf || null
    if (csrf && csrf.headerName && csrf.token) headers[csrf.headerName] = csrf.token
    headers["X-QAR-Encrypted"] = "1"
    headers["X-QAR-Transport"] = state.protocol
    headers["X-QAR-Session"] = state.sessionId

    let body = opts && opts.body ? opts.body : null
    let init = Object.assign({}, opts || {}, { credentials: "include", method, headers })

    if (body != null && typeof body !== "string") body = JSON.stringify(body)
    if (body != null) {
      headers["Content-Type"] = "application/json"
      const env = await encryptBody(method, path, body)
      init.body = JSON.stringify(env)
    } else {
      delete init.body
    }

    const res = await fetch(path, init)
    const ct = (res.headers.get("content-type") || "").toLowerCase()
    let data = null
    if (ct.includes("application/json")) {
      const env = await res.json().catch(() => null)
      if (env && env.iv && env.ciphertext) {
        const plain = await decryptBody(method, path, env)
        data = plain ? JSON.parse(plain) : null
      } else {
        data = env
      }
    } else {
      const txtEnv = await res.text().catch(() => "")
      data = txtEnv
    }

    if (!res.ok) {
      const msg = data && data.message ? data.message : (typeof data === "string" ? data : "request_failed")
      throw new Error(mapErrorMessage(msg))
    }
    return data
  }

  window.TransportCrypto = {
    ensureSession,
    fetch: fetchEncrypted
  }
})()
