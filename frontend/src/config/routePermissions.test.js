import { describe, expect, it } from 'vitest'
import { resolvePermissionAccess } from '../utils/permissions'
import { isSuperAdmin } from '../utils/auth'

/** Garde-fous alignés sur App.jsx (consultation back-office) */
const PROTECTED_ROUTES = [
  {
    id: 'sales-browse',
    anyOf: ['pos.sale.read', 'pos.sale.read_own', 'analytics.sales.read', 'pos.report.read'],
  },
  {
    id: 'returns-browse',
    anyOf: ['pos.return.read', 'analytics.sales.read', 'pos.report.read'],
  },
  {
    id: 'pos-register',
    permission: 'pos.sale.read',
  },
  {
    id: 'products',
    permission: 'products.read',
  },
]

function accessFor(user, route) {
  const permissions = user.permissions ?? []
  const superAdmin = isSuperAdmin(user)
  const hasPermission = (code) => superAdmin || permissions.includes(code)
  const hasAnyPermission = (...codes) => superAdmin || codes.some(hasPermission)
  return resolvePermissionAccess({
    permission: route.permission,
    anyOf: route.anyOf ?? [],
    hasPermission,
    hasAnyPermission,
    isSuperAdmin: superAdmin,
  }).allowed
}

describe('routes protégées (consultation ventes / retours)', () => {
  it('autorise pos.sale.read sur /sales', () => {
    expect(accessFor({ permissions: ['pos.sale.read'] }, PROTECTED_ROUTES[0])).toBe(true)
  })

  it('autorise pos.return.read sur /returns', () => {
    expect(accessFor({ permissions: ['pos.return.read'] }, PROTECTED_ROUTES[1])).toBe(true)
  })

  it('refuse un caissier sans permission browse', () => {
    expect(accessFor({ permissions: ['pos.payment.collect'] }, PROTECTED_ROUTES[0])).toBe(false)
    expect(accessFor({ permissions: ['pos.payment.collect'] }, PROTECTED_ROUTES[1])).toBe(false)
  })

  it('SUPER_ADMIN accède à toutes les routes listées', () => {
    const user = { roles: ['SUPER_ADMIN'], permissions: [] }
    for (const route of PROTECTED_ROUTES) {
      expect(accessFor(user, route)).toBe(true)
    }
  })
})
