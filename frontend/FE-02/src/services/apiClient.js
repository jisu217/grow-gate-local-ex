// 백엔드 API 공통 fetch 헬퍼
const configuredApiUrl = import.meta.env.VITE_API_URL?.trim() || ''
const rawApiUrl = configuredApiUrl || (typeof window !== 'undefined' ? window.location.origin : '')
const normalizedApiUrl = rawApiUrl.replace(/\/+$/, '')
const API_BASE = normalizedApiUrl.endsWith('/api') ? normalizedApiUrl : `${normalizedApiUrl}/api`
const LEGACY_TOKEN_KEY = 'gg_access_token'
const DEFAULT_TIMEOUT_MS = Number(import.meta.env.VITE_API_TIMEOUT_MS ?? 15000)
const DEFAULT_RETRY_COUNT = Number(import.meta.env.VITE_API_RETRY_COUNT ?? 2)
let memoryAccessToken = null

// accessToken은 XSS 노출면을 줄이기 위해 localStorage에 저장하지 않는다.
// 새로고침 뒤 인증은 서버가 내려준 HttpOnly cookie가 담당한다.
export function getAccessToken() {
  return memoryAccessToken
}

export function setAccessToken(token) {
  memoryAccessToken = token || null
  localStorage.removeItem(LEGACY_TOKEN_KEY)
}

export function clearAccessToken() {
  memoryAccessToken = null
  localStorage.removeItem(LEGACY_TOKEN_KEY)
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
  const timeoutId = window.setTimeout(() => controller.abort(), DEFAULT_TIMEOUT_MS)
  const hasBody = options.body !== undefined && options.body !== null

  try {
    const res = await fetch(`${API_BASE}${path}`, {
      ...options,
      credentials: options.credentials ?? 'include',
      signal: options.signal ?? controller.signal,
      headers: {
        ...(hasBody ? { 'Content-Type': 'application/json' } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
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
    window.clearTimeout(timeoutId)
  }
}

function isRetryableError(err) {
  if (!err) return false
  if (err.status === 408 || err.status === 429) return true
  if (err.status >= 500) return true
  return err.message?.includes('시간이 초과')
}

function delay(ms) {
  return new Promise((resolve) => window.setTimeout(resolve, ms))
}
