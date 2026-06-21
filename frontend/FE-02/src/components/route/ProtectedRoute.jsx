import { Navigate, Outlet } from 'react-router-dom'
import { getAuthUser } from '../../services/authService.js'

// requireCompany: 회사 계정 전용
// 로그인·권한 보호 라우트 컴포넌트
export default function ProtectedRoute({ requireCompany = false }) {
  const user = getAuthUser()
  if (!user) {
    return <Navigate to="/login" replace />
  }
  if (requireCompany && user.userType !== 'company') {
    return <Navigate to="/mypage" replace />
  }
  return <Outlet />
}
