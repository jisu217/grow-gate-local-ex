import fs from 'node:fs'
import path from 'node:path'
import assert from 'node:assert/strict'

const protectedRoutePath = path.resolve('src/components/route/ProtectedRoute.jsx')
const source = fs.readFileSync(protectedRoutePath, 'utf8')

assert(
  !source.includes("if (!token)") && !source.includes('if (!user || !token)'),
  'ProtectedRoute must not require the in-memory accessToken; refresh after reload relies on HttpOnly cookie.',
)

assert(
  source.includes('refreshAuthUser()'),
  'ProtectedRoute must call refreshAuthUser() so /api/users/me can restore the session from the HttpOnly cookie.',
)

console.log('auth session regression checks passed')
