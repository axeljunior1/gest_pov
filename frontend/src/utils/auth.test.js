import { describe, expect, it } from 'vitest'
import { isSuperAdmin } from './auth'
import { canBrowseSalesBackOffice } from './saleDisplay'

describe('isSuperAdmin', () => {
  it('détecte le rôle SUPER_ADMIN', () => {
    expect(isSuperAdmin({ roles: ['SUPER_ADMIN'] })).toBe(true)
    expect(isSuperAdmin({ roles: ['ADMIN'] })).toBe(false)
  })
})

describe('canBrowseSalesBackOffice', () => {
  const hasPermission = (p) => p === 'pos.sale.read'
  const hasAnyPermission = (...codes) => codes.some(hasPermission)

  it('autorise avec pos.sale.read', () => {
    expect(canBrowseSalesBackOffice(hasPermission, hasAnyPermission)).toBe(true)
  })

  it('refuse sans permission browse', () => {
    expect(canBrowseSalesBackOffice(() => false, () => false)).toBe(false)
  })
})
