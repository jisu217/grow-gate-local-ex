import { mockAuth } from '../mock/auth.js'
import { PERMISSIONS } from '../mock/roles.js'

// 연동 시 mockAuth.role → useAuth().role 교체
export function usePermission() {
  const role = mockAuth.role

  const can = (action) => {
    const allowed = PERMISSIONS[action]
    if (!allowed) return false
    return allowed.includes(role)
  }

  return { can, role }
}
