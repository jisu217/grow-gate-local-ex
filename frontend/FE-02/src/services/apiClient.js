// 백엔드 API 공통 fetch 헬퍼
// 로컬 개발: Vite 프록시(/api → http://localhost:8080) 사용 → 포트/CORS 영향 없음
// 배포 등으로 VITE_API_URL이 지정되면 그 주소로 직접 호출
const API_BASE = import.meta.env.VITE_API_URL
  ? `${import.meta.env.VITE_API_URL}/api`
  : '/api'

// 저장된 로그인 토큰 꺼내기
export function getToken() {
  return localStorage.getItem('accessToken')
}

export async function apiFetch(path, options = {}) {
  // auth: false 면 토큰을 안 붙임 (로그인·회원가입 같은 공개 API용)
  const { auth = true, headers, ...rest } = options
  const token = getToken()
  const res = await fetch(`${API_BASE}${path}`, {
    ...rest,
    headers: {
      'Content-Type': 'application/json',
      ...(auth && token ? { Authorization: `Bearer ${token}` } : {}),
      ...headers,
    },
  })
  if (!res.ok) {
    throw new Error(`API ${res.status} ${res.statusText}: ${path}`)
  }
  if (res.status === 204) return null
  return res.json()
}
