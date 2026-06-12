// 임시 인증 상태 — 연동 시 AuthContext/useAuth로 교체
export const mockAuth = {
  isLoggedIn: true,
  accountType: 'company', // 'personal' | 'company'
  role: 'admin',          // 'owner' | 'admin' | 'editor' | 'viewer'
}
