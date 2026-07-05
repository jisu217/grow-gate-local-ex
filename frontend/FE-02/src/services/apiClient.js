// 백엔드 API 공통 fetch 헬퍼
const viteEnv = import.meta.env ?? {}
const configuredApiUrl = (viteEnv.VITE_API_URL?.trim() || viteEnv.VITE_API_BASE_URL?.trim() || '')
const browserLocation = typeof window !== 'undefined' ? window.location : null
const API_BASE = resolveApiBase(configuredApiUrl, browserLocation?.origin ?? '', browserLocation?.hostname ?? '')
const LEGACY_TOKEN_KEY = 'gg_access_token'
const DEFAULT_TIMEOUT_MS = Number(viteEnv.VITE_API_TIMEOUT_MS ?? 15000)
const DEFAULT_RETRY_COUNT = Number(viteEnv.VITE_API_RETRY_COUNT ?? 2)
let memoryAccessToken = null

export function resolveApiBase(configuredUrl, locationOrigin = '', locationHostname = '') {
  const trimmedConfiguredUrl = configuredUrl?.trim() ?? ''
  const safeConfiguredUrl = shouldIgnoreConfiguredUrl(trimmedConfiguredUrl, locationHostname)
    ? ''
    : trimmedConfiguredUrl
  const rawApiUrl = safeConfiguredUrl || locationOrigin
  const normalizedApiUrl = rawApiUrl.replace(/\/+$/, '')
  return normalizedApiUrl.endsWith('/api') ? normalizedApiUrl : `${normalizedApiUrl}/api`
}

function shouldIgnoreConfiguredUrl(configuredUrl, locationHostname) {
  if (!configuredUrl) return false
  try {
    const configured = new URL(configuredUrl)
    const configuredIsLocal = isLocalHostname(configured.hostname)
    const pageIsLocal = !locationHostname || isLocalHostname(locationHostname)
    return configuredIsLocal && !pageIsLocal
  } catch {
    return false
  }
}

function isLocalHostname(hostname) {
  return ['localhost', '127.0.0.1', '0.0.0.0', '::1'].includes(hostname)
}

// accessToken은 XSS 노출면을 줄이기 위해 localStorage에 저장하지 않는다.
// 새로고침 뒤 인증은 서버가 내려준 HttpOnly cookie가 담당한다.
export function getAccessToken() {
  return memoryAccessToken
}

export function setAccessToken(token) {
  memoryAccessToken = token || null
  getStorage()?.removeItem(LEGACY_TOKEN_KEY)
}

export function clearAccessToken() {
  memoryAccessToken = null
  getStorage()?.removeItem(LEGACY_TOKEN_KEY)
}

// 인증 필요한 API는 저장된 accessToken을 Authorization 헤더로 자동 첨부
export async function apiFetch(path, options = {}) {
  const method = (options.method ?? 'GET').toUpperCase()
  const retryCount = options.retryCount ?? (method === 'GET' ? DEFAULT_RETRY_COUNT : 0)
  let lastError = null

  for (let attempt = 0; attempt <= retryCount; attempt += 1) {
    try {
      return await apiFetchOnce(path, options)
    } catch (err) {
      lastError = err
      if (attempt >= retryCount || !isRetryableError(err)) break
      await delay(250 * (attempt + 1))
    }
  }

  throw lastError
}

async function apiFetchOnce(path, options = {}) {
  const token = getAccessToken()
  const controller = new AbortController()
  const timeoutId = getTimer().setTimeout(() => controller.abort(), DEFAULT_TIMEOUT_MS)
  const hasBody = options.body !== undefined && options.body !== null
  const csrfToken = getCookie('gg_csrf_token')
  const unsafeMethod = !['GET', 'HEAD', 'OPTIONS', 'TRACE'].includes((options.method ?? 'GET').toUpperCase())

  try {
    const res = await fetch(`${API_BASE}${path}`, {
      ...options,
      credentials: options.credentials ?? 'include',
      signal: options.signal ?? controller.signal,
      headers: {
        ...(hasBody ? { 'Content-Type': 'application/json' } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...(unsafeMethod && csrfToken ? { 'X-GG-CSRF': csrfToken } : {}),
        ...options.headers,
      },
    })

    if (!res.ok) {
      let message = `API ${res.status} ${res.statusText}: ${path}`
      try {
        const body = await res.json()
        message = body.message || body.error?.message || body.error || message
      } catch {
        // Non-JSON error bodies still surface the HTTP status above.
      }
      const error = new Error(message)
      error.status = res.status
      throw error
    }
    if (res.status === 204) return null
    return res.json()
  } catch (err) {
    if (err.name === 'AbortError') {
      throw new Error(`API 요청 시간이 초과되었습니다: ${path}`)
    }
    throw err
  } finally {
    getTimer().clearTimeout(timeoutId)
  }
}

function isRetryableError(err) {
  if (!err) return false
  if (err.status === 408 || err.status === 429) return true
  if (err.status >= 500) return true
  return err.message?.includes('시간이 초과')
}

function delay(ms) {
  return new Promise((resolve) => getTimer().setTimeout(resolve, ms))
}

function getTimer() {
  return typeof window !== 'undefined' ? window : globalThis
}

function getStorage() {
  return typeof localStorage !== 'undefined' ? localStorage : null
}

function getCookie(name) {
  if (typeof document === 'undefined') return ''
  return document.cookie
    .split(';')
    .map((part) => part.trim())
    .find((part) => part.startsWith(`${name}=`))
    ?.slice(name.length + 1) ?? ''
}
