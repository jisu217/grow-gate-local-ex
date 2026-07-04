import { useEffect, useState } from 'react'
import { Navigate, Outlet } from 'react-router-dom'
import { getAuthUser, refreshAuthUser } from '../../services/authService.js'
import StatusMessage from '../ui/StatusMessage.jsx'

// requireCompany: 회사 계정 전용
// 로그인·권한 보호 라우트 컴포넌트
export default function ProtectedRoute({ requireCompany = false }) {
  const [user, setUser] = useState(getAuthUser())
  const [state, setState] = useState('checking')

  useEffect(() => {
    let mounted = true

    refreshAuthUser()
      .then((freshUser) => {
        if (!mounted) return
        setUser(freshUser)
        setState('ok')
      })
      .catch(() => {
        if (!mounted) return
        setUser(null)
        setState('anonymous')
      })

    return () => {
      mounted = false
    }
  }, [])

  if (state === 'checking') {
    return <StatusMessage type="loading" message="권한 정보를 확인하는 중입니다." />
  }

  if (!user) {
    return <Navigate to="/login" replace />
  }
  if (requireCompany && user.userType !== 'company') {
    return <Navigate to="/mypage" replace />
  }
  return <Outlet />
}
