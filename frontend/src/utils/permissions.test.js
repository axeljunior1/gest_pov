import { describe, expect, it } from 'vitest'
import { resolvePermissionAccess } from './permissions'

describe('resolvePermissionAccess', () => {
  const hasPermission = (code) => code === 'products.read'
  const hasAnyPermission = (...codes) => codes.some(hasPermission)

  it('autorise SUPER_ADMIN sans permission explicite', () => {
    const r = resolvePermissionAccess({
      permission: 'pos.sale.read',
      hasPermission: () => false,
      hasAnyPermission: () => false,
      isSuperAdmin: true,
    })
    expect(r.allowed).toBe(true)
  })

  it('respecte anyOf', () => {
    const r = resolvePermissionAccess({
      anyOf: ['products.read', 'pos.sale.read'],
      hasPermission,
      hasAnyPermission,
    })
    expect(r.allowed).toBe(true)
  })

  it('refuse si permission manquante', () => {
    const r = resolvePermissionAccess({
      permission: 'pos.sale.read',
      hasPermission,
      hasAnyPermission,
    })
    expect(r.allowed).toBe(false)
    expect(r.requiredLabel).toBe('pos.sale.read')
  })
})
