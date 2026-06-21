// 역할 기반 권한 확인 훅
import { mockAuth } from '../mock/auth.js'
import { PERMISSIONS } from '../mock/roles.js'

export function usePermission() {
  const role = mockAuth.role

  // 특정 액션 권한 확인
  const can = (action) => {
    const allowed = PERMISSIONS[action]
    if (!allowed) return false
    return allowed.includes(role)
  }

  return { can, role }
}
