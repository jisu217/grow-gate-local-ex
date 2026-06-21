// 인증 서비스 — 로그인/회원가입/로그아웃 + 로그인 사용자 정보 저장
import { apiFetch, setAccessToken, clearAccessToken } from './apiClient.js'

const USER_KEY = 'gg_auth_user'

// 저장된 로그인 사용자 정보 조회 (로그인 안 되어 있으면 null)
export function getAuthUser() {
  const raw = localStorage.getItem(USER_KEY)
  return raw ? JSON.parse(raw) : null
}

// 로그인 — 성공 시 accessToken과 사용자 정보를 저장
export async function login(email, password) {
  const res = await apiFetch('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  })

  setAccessToken(res.accessToken)
  const user = {
    userId: res.userId,
    userType: res.userType,
    role: res.role,
    companyId: res.companyId,
  }
  localStorage.setItem(USER_KEY, JSON.stringify(user))
  return user
}

// 회원가입 (개인/기업 공통, type 필드로 구분)
export function signup(payload) {
  return apiFetch('/auth/signup', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

// 로그아웃 — 서버 호출이 실패해도 로컬 인증 정보는 제거
export async function logout() {
  try {
    await apiFetch('/auth/logout', { method: 'POST' })
  } finally {
    clearAccessToken()
    localStorage.removeItem(USER_KEY)
  }
}
