import { Navigate, Outlet } from 'react-router-dom'
import { mockAuth } from '../../mock/auth.js'

// requireCompany: 회사 계정 전용
// 로그인·권한 보호 라우트 컴포넌트
export default function ProtectedRoute({ requireCompany = false }) {
  if (!mockAuth.isLoggedIn) {
    return <Navigate to="/login" replace />
  }
  if (requireCompany && mockAuth.accountType !== 'company') {
    return <Navigate to="/mypage" replace />
  }
  return <Outlet />
}
