const BACK_OFFICE_ROLES = new Set(['SUPER_ADMIN', 'ADMIN', 'MANAGER', 'OPERATOR', 'VIEWER'])

/** Utilisateur limité à la caisse (rôle CASHIER sans autre rôle back-office). */
export function isPosOnlyUser(user) {
  if (!user) return false
  const roles = user.roles ?? []
  if (roles.includes('CASHIER')) {
    return !roles.some((r) => BACK_OFFICE_ROLES.has(r))
  }
  const perms = user.permissions ?? []
  if (perms.length === 0) return false
  return perms.every((p) => p.startsWith('pos.'))
}

export function getDefaultAppPath(user) {
  return isPosOnlyUser(user) ? '/pos' : '/'
}
