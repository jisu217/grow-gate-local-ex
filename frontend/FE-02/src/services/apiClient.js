// 백엔드 API 공통 fetch 헬퍼
const API_BASE = `${import.meta.env.VITE_API_URL ?? 'http://localhost:8080'}/api`
const TOKEN_KEY = 'gg_access_token'

// accessToken 저장/조회/삭제 (authService에서 로그인·로그아웃 시 호출)
export function getAccessToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function setAccessToken(token) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearAccessToken() {
  localStorage.removeItem(TOKEN_KEY)
}

// 인증 필요한 API는 저장된 accessToken을 Authorization 헤더로 자동 첨부
export async function apiFetch(path, options = {}) {
  const token = getAccessToken()
  const res = await fetch(`${API_BASE}${path}`, {
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers,
    },
    ...options,
  })
  if (!res.ok) {
    throw new Error(`API ${res.status} ${res.statusText}: ${path}`)
  }
  if (res.status === 204) return null
  return res.json()
}
