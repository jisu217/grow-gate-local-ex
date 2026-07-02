// 역할 기반 권한 확인 훅
import { getAuthUser } from '../services/authService.js'
import { PERMISSIONS } from '../constants/permissions.js'

export function usePermission() {
  const user = getAuthUser()
  const role = user?.role ?? (user?.userType === 'personal' ? 'owner' : null)

  // 특정 액션 권한 확인
  const can = (action) => {
    const allowed = PERMISSIONS[action]
    if (!allowed) return false
    return allowed.includes(role)
  }

  return { can, role }
}
