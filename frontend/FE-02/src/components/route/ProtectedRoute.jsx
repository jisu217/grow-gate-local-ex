import { Navigate, Outlet } from 'react-router-dom'
import { getAuth } from '../../services/authService.js'

// requireCompany: 회사 계정 전용
// 로그인·권한 보호 라우트 컴포넌트
export default function ProtectedRoute({ requireCompany = false }) {
  const auth = getAuth() // 로그인 시 저장된 정보 (없으면 null)
  if (!auth?.isLoggedIn) {
    return <Navigate to="/login" replace />
  }
  if (requireCompany && auth.accountType !== 'company') {
    return <Navigate to="/mypage" replace />
  }
  return <Outlet />
}
