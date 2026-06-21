// 인증 서비스 — 로그인 / 회원가입 / 로그아웃 / 로그인 상태
import { apiFetch } from './apiClient.js'

// 로그인 — 성공 시 토큰과 사용자 정보를 localStorage에 저장
export async function login({ email, password }) {
  const data = await apiFetch('/auth/login', {
    method: 'POST',
    auth: false, // 공개 API — 토큰 안 보냄
    body: JSON.stringify({ email, password }),
  })
  // 백엔드 응답: { accessToken, userId, userType, role, companyId }
  localStorage.setItem('accessToken', data.accessToken)
  localStorage.setItem('auth', JSON.stringify({
    isLoggedIn: true,
    accountType: data.userType, // 'personal' | 'company'
    role: data.role,            // 'owner' | 'admin' | 'editor' | 'viewer'
    userId: data.userId,
    companyId: data.companyId,
  }))
  return data
}

// 회원가입 — payload: { type, email, password, name, companyName?, businessNumber?, industry? }
export async function signup(payload) {
  return apiFetch('/auth/signup', {
    method: 'POST',
    auth: false, // 공개 API — 토큰 안 보냄
    body: JSON.stringify(payload),
  })
}

// 로그아웃 — 토큰 삭제
export async function logout() {
  try { await apiFetch('/auth/logout', { method: 'POST' }) } catch { /* 서버 실패해도 로컬은 정리 */ }
  localStorage.removeItem('accessToken')
  localStorage.removeItem('auth')
}

// 현재 로그인 상태 (없으면 null)
export function getAuth() {
  try { return JSON.parse(localStorage.getItem('auth')) } catch { return null }
}
