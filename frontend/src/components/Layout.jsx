import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

const navItems = [
  { to: '/', label: 'Produits', end: true },
  { to: '/categories', label: 'Catégories' },
  { to: '/suppliers', label: 'Fournisseurs' },
  { to: '/units', label: 'Unités' },
  { to: '/stock', label: 'Stock' },
  { to: '/stock/entries', label: 'Entrées stock' },
  { to: '/alerts', label: 'Alertes' },
  { to: '/attributes', label: 'Attributs' },
  { to: '/documentation', label: 'Documentation' },
]

export default function Layout() {
  const { user, logout } = useAuth()

  return (
    <div className="min-h-screen flex">
      <aside className="w-56 bg-white border-r border-gray-200 flex flex-col">
        <div className="px-5 py-6 border-b border-gray-100">
          <h1 className="text-lg font-semibold tracking-tight">ERP Produits</h1>
          <p className="text-xs text-gray-500 mt-0.5">Modules 1, 2, 3 & auth</p>
        </div>
        <nav className="flex-1 p-3 space-y-0.5">
          {navItems.map(({ to, label, end }) => (
            <NavLink
              key={to}
              to={to}
              end={end}
              className={({ isActive }) =>
                `block px-3 py-2 rounded-lg text-sm transition-colors ${
                  isActive
                    ? 'bg-gray-900 text-white font-medium'
                    : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'
                }`
              }
            >
              {label}
            </NavLink>
          ))}
        </nav>
        <div className="p-4 border-t border-gray-100">
          <p className="text-xs text-gray-500 truncate" title={user?.email}>
            {user?.firstName} {user?.lastName}
          </p>
          <p className="text-xs text-gray-400 truncate mb-2">{user?.email}</p>
          <button
            type="button"
            onClick={logout}
            className="w-full text-left text-sm text-gray-600 hover:text-gray-900 px-3 py-2 rounded-lg hover:bg-gray-50"
          >
            Déconnexion
          </button>
        </div>
      </aside>
      <main className="flex-1 overflow-auto">
        <div className="max-w-6xl mx-auto px-8 py-8">
          <Outlet />
        </div>
      </main>
    </div>
  )
}
