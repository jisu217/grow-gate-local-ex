import { Navigate, Outlet } from 'react-router-dom'
import { mockAuth } from '../../mock/auth.js'

// requireCompany: 회사 계정 전용
export default function ProtectedRoute({ requireCompany = false }) {
  if (!mockAuth.isLoggedIn) {
    return <Navigate to="/login" replace />
  }
  if (requireCompany && mockAuth.accountType !== 'company') {
    return <Navigate to="/mypage" replace />
  }
  return <Outlet />
}
