// 백엔드 API 공통 fetch 헬퍼
const API_BASE = `${import.meta.env.VITE_API_URL ?? 'http://localhost:8080'}/api`

// 진단/제품 API는 인증 필요(@AuthenticationPrincipal)하나, FE에 아직 로그인·JWT 저장
// 미연동(README TODO 참고)으로 Authorization 헤더는 로그인 연동 시 추가 예정.
export async function apiFetch(path, options = {}) {
  const res = await fetch(`${API_BASE}${path}`, {
    headers: { 'Content-Type': 'application/json', ...options.headers },
    ...options,
  })
  if (!res.ok) {
    throw new Error(`API ${res.status} ${res.statusText}: ${path}`)
  }
  if (res.status === 204) return null
  return res.json()
}
