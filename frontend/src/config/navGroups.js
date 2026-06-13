export const navGroups = [
  {
    id: 'overview',
    items: [
      { to: '/dashboard', label: 'Tableau de bord', permission: 'dashboard.read' },
      { to: '/analytics', label: 'Analytics', permissions: ['analytics.read', 'analytics.sales.read'] },
      { to: '/pos', label: 'Caisse POS', permission: 'pos.sale.read' },
    ],
  },
  {
    id: 'catalog',
    label: 'Catalogue',
    items: [
      { to: '/', label: 'Produits', isActive: (p) => p === '/' || p.startsWith('/products/') },
      { to: '/categories', label: 'Catégories' },
      { to: '/suppliers', label: 'Fournisseurs' },
      { to: '/units', label: 'Unités' },
      { to: '/attributes', label: 'Attributs' },
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
      { to: '/stock', label: 'Consultation', isActive: (p) => p === '/stock' },
      { to: '/stock/entries', label: 'Entrées' },
      { to: '/stock/exits', label: 'Sorties', permission: 'stock_exit.read' },
      { to: '/stock/movements', label: 'Mouvements', permission: 'stock_movement.read' },
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

export function filterVisibleGroups(groups, hasPermission) {
  return groups
    .map((group) => ({
      ...group,
      items: group.items.filter((item) => {
        if (item.permissions?.length) {
          return item.permissions.some((p) => hasPermission(p))
        }
        return !item.permission || hasPermission(item.permission)
      }),
    }))
    .filter((group) => group.items.length > 0)
}
