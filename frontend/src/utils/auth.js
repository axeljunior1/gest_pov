import { getFirstAccessibleNavPath, hasBackOfficeMenuAccess } from '../config/navGroups'

const BACK_OFFICE_ROLES = new Set(['SUPER_ADMIN', 'ADMIN', 'MANAGER', 'OPERATOR', 'VIEWER'])

export function isSuperAdmin(user) {
  return (user?.roles ?? []).includes('SUPER_ADMIN')
}

/** Utilisateur limité à la caisse (rôle CASHIER sans autre rôle back-office). */
export function isPosOnlyUser(user) {
  if (!user) return false
  const roles = user.roles ?? []
  if (roles.includes('CASHIER')) {
    return !roles.some((r) => BACK_OFFICE_ROLES.has(r))
  }
  const perms = user.permissions ?? []
  if (perms.length === 0) return false
  const nonPos = perms.filter((p) => !p.startsWith('pos.') && !p.startsWith('loyalty.'))
  if (nonPos.length === 0) return true
  if (nonPos.every((p) => p.startsWith('customer.'))) {
    return !roles.some((r) => BACK_OFFICE_ROLES.has(r))
  }
  return false
}

export function canCollectPayment(user) {
  const perms = user?.permissions ?? []
  return perms.includes('pos.payment.collect') || perms.includes('pos.payment.validate')
}

export function canPrepareSales(user) {
  const perms = user?.permissions ?? []
  return perms.includes('pos.sale.send_to_payment')
    || perms.includes('pos.sale.prepare')
    || perms.includes('pos.sale.create')
}

/** Caissier pur : encaisse uniquement, ne prépare pas de ventes. */
export function isCashierOnlyUser(user) {
  if (!user) return false
  return canCollectPayment(user) && !canPrepareSales(user)
}

/** Vendeur pur : prépare des ventes, n'encaisse pas. */
export function isSellerOnlyUser(user) {
  if (!user) return false
  return canPrepareSales(user) && !canCollectPayment(user)
}

/** Accès aux deux postes (préparation + encaissement). */
export function hasDualPosRole(user) {
  if (!user) return false
  return canPrepareSales(user) && canCollectPayment(user)
}

/** Libellé du rôle POS pour l'interface. */
export function getPosRoleLabel(user) {
  if (!user) return 'POS'
  const prep = canPrepareSales(user)
  const coll = canCollectPayment(user)
  if (prep && coll) return 'Vendeur & caissier'
  if (prep) return 'Vendeur'
  if (coll) return 'Caissier'
  return 'POS'
}

export function getDefaultAppPath(user, hasPermission) {
  if (!user || !hasPermission) return '/login'

  const navOptions = { userRoles: user.roles ?? [] }

  if (isCashierOnlyUser(user)) return '/pos/pending'

  if (hasPermission('dashboard.read')) return '/dashboard'
  if (hasPermission('products.read')) return '/'
  if (canPrepareSales(user) && hasPermission('pos.sale.read')) return '/pos'
  if (hasPermission('customer.read') && !hasBackOfficeMenuAccess(hasPermission, navOptions)) {
    return '/pos'
  }

  const first = getFirstAccessibleNavPath(hasPermission, navOptions)
  if (first) return first

  if (hasPermission('pos.sale.read')) return '/pos'
  return '/documentation'
}
