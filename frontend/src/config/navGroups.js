export const navGroups = [
  {
    id: 'overview',
    items: [
      { to: '/dashboard', label: 'Tableau de bord', permission: 'dashboard.read' },
      { to: '/pos', label: 'Caisse POS', permission: 'pos.sale.read' },
    ],
  },
  {
    id: 'sales',
    label: 'Ventes',
    items: [
      {
        to: '/sales',
        label: 'Consultation ventes',
        permissions: ['pos.sale.read', 'pos.sale.read_own', 'analytics.sales.read', 'pos.report.read'],
        isActive: (p) => p === '/sales' || (p.startsWith('/sales/') && !p.startsWith('/sales/cancellations')),
      },
      {
        to: '/returns',
        label: 'Retours',
        permissions: ['pos.return.read', 'analytics.sales.read', 'pos.report.read'],
        isActive: (p) => p === '/returns' || p.startsWith('/returns/'),
      },
    ],
  },
  {
    id: 'analytics',
    label: 'Analytics',
    items: [
      {
        to: '/analytics',
        label: 'Vue d’ensemble',
        permissions: ['analytics.read', 'analytics.sales.read'],
        isActive: (p) => p === '/analytics' || (p.startsWith('/analytics/') && !p.startsWith('/analytics/cancellations')),
      },
      {
        to: '/analytics/cancellations',
        label: 'Ventes annulées',
        permissions: ['sales.cancellations.read', 'analytics.read', 'analytics.sales.read'],
        isActive: (p) => p === '/analytics/cancellations' || p.startsWith('/analytics/cancellations/'),
      },
    ],
  },
  {
    id: 'catalog',
    label: 'Catalogue',
    items: [
      { to: '/', label: 'Produits', permission: 'products.read', isActive: (p) => p === '/' || p.startsWith('/products/') },
      { to: '/categories', label: 'Catégories', permission: 'products.read' },
      { to: '/brands', label: 'Marques', permission: 'products.read' },
      { to: '/suppliers', label: 'Fournisseurs', permission: 'products.read' },
      { to: '/units', label: 'Unités', permission: 'products.read' },
      { to: '/attributes', label: 'Attributs', permission: 'products.read' },
    ],
  },
  {
    id: 'customers',
    label: 'Clients',
    items: [
      { to: '/customers', label: 'Liste clients', permission: 'customer.read', isActive: (p) => p === '/customers' || p.startsWith('/customers/') },
    ],
  },
  {
    id: 'stock',
    label: 'Stock',
    items: [
      { to: '/stock', label: 'Consultation', permission: 'stock.read', isActive: (p) => p === '/stock' },
      { to: '/stock/entries', label: 'Entrées', permission: 'stock_entry.read' },
      { to: '/stock/exits', label: 'Sorties', permission: 'stock_exit.read' },
      { to: '/stock/movements', label: 'Mouvements', permissions: ['stock_movement.read', 'stock.read'] },
      { to: '/stock/inventories', label: 'Inventaires', permission: 'inventory.read' },
      { to: '/alerts', label: 'Alertes', permission: 'alerts.read' },
    ],
  },
  {
    id: 'admin',
    label: 'Administration',
    items: [
      { to: '/users', label: 'Utilisateurs', permission: 'users.read' },
      { to: '/roles', label: 'Rôles', permission: 'roles.read' },
      { to: '/import-export', label: 'Import / Export', permission: 'import.read' },
      { to: '/settings', label: 'Paramètres', permission: 'settings.read' },
      { to: '/dev-tools', label: 'Outils dev', devOnly: true, roles: ['SUPER_ADMIN'] },
    ],
  },
  {
    id: 'help',
    items: [
      { to: '/documentation', label: 'Documentation' },
    ],
  },
]

export function isNavItemActive(pathname, item) {
  if (item.isActive) return item.isActive(pathname)
  return pathname === item.to || pathname.startsWith(`${item.to}/`)
}

export function findActiveGroup(pathname, groups) {
  return groups.find((g) => g.label && g.items.some((item) => isNavItemActive(pathname, item)))
}

export function canSeeNavItem(item, hasPermission, options = {}) {
  const { isDev = import.meta.env.DEV, userRoles = [] } = options
  if (item.devOnly && !isDev) return false
  if (item.roles?.length && !item.roles.some((r) => userRoles.includes(r))) return false
  if (item.permissions?.length) {
    return item.permissions.some((p) => hasPermission(p))
  }
  return !item.permission || hasPermission(item.permission)
}

export function filterVisibleGroups(groups, hasPermission, options = {}) {
  return groups
    .map((group) => ({
      ...group,
      items: group.items.filter((item) => canSeeNavItem(item, hasPermission, options)),
    }))
    .filter((group) => group.items.length > 0)
}

/** Première route accessible dans le menu (ordre sidebar). */
export function getFirstAccessibleNavPath(hasPermission, options = {}) {
  for (const group of filterVisibleGroups(navGroups, hasPermission, options)) {
    for (const item of group.items) {
      if (item.to) return item.to
    }
  }
  return null
}

/** Première route back-office (hors POS). */
export function getBackOfficeEntryPath(hasPermission, options = {}) {
  for (const group of filterVisibleGroups(navGroups, hasPermission, options)) {
    for (const item of group.items) {
      if (item.to && item.to !== '/pos') return item.to
    }
  }
  return null
}

/** L'utilisateur voit au moins un écran back-office (hors POS). */
export function hasBackOfficeMenuAccess(hasPermission, options = {}) {
  return getBackOfficeEntryPath(hasPermission, options) != null
}
