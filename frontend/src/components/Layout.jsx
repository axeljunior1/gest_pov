import { NavLink, Outlet, useLocation } from 'react-router-dom'
import { useEffect, useMemo, useState } from 'react'
import { useAuth } from '../context/AuthContext'
import { settingsApi } from '../api'
import SectionSubNav from './SectionSubNav'
import { filterVisibleGroups, isNavItemActive, navGroups } from '../config/navGroups'

function NavItem({ item, pathname }) {
  const active = isNavItemActive(pathname, item)
  return (
    <NavLink
      to={item.to}
      end={item.to === '/'}
      className={() =>
        `block px-3 py-1.5 rounded-lg text-sm transition-colors ${
          active
            ? 'bg-gray-900 text-white font-medium'
            : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'
        }`
      }
    >
      {item.label}
    </NavLink>
  )
}

export default function Layout() {
  const { user, logout, hasPermission } = useAuth()
  const location = useLocation()
  const [companyName, setCompanyName] = useState('ERP Produits')

  const visibleGroups = useMemo(
    () => filterVisibleGroups(navGroups, hasPermission, { userRoles: user?.roles ?? [] }),
    [hasPermission, user?.roles],
  )

  const activeGroupId = useMemo(
    () => visibleGroups.find((g) => g.items.some((item) => isNavItemActive(location.pathname, item)))?.id,
    [visibleGroups, location.pathname],
  )

  const [expanded, setExpanded] = useState(() => new Set(
    navGroups.filter((g) => g.label).map((g) => g.id),
  ))

  useEffect(() => {
    if (activeGroupId) {
      setExpanded((prev) => new Set([...prev, activeGroupId]))
    }
  }, [activeGroupId])

  useEffect(() => {
    settingsApi.getPublic()
      .then((s) => { if (s.companyName) setCompanyName(s.companyName) })
      .catch(() => {})
  }, [])

  const toggleGroup = (id) => {
    setExpanded((prev) => {
      const next = new Set(prev)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })
  }

  return (
    <div className="min-h-screen flex">
      <aside className="w-56 bg-white border-r border-gray-200 flex flex-col shrink-0">
        <div className="px-5 py-6 border-b border-gray-100">
          <h1 className="text-lg font-semibold tracking-tight">{companyName}</h1>
          <p className="text-xs text-gray-500 mt-0.5">Gestion produits & stock</p>
        </div>
        <nav className="flex-1 p-3 space-y-3 overflow-y-auto">
          {visibleGroups.map((group) => {
            if (!group.label) {
              return (
                <ul key={group.id} className="space-y-0.5">
                  {group.items.map((item) => (
                    <li key={item.to}>
                      <NavItem item={item} pathname={location.pathname} />
                    </li>
                  ))}
                </ul>
              )
            }

            const isOpen = expanded.has(group.id)
            const groupActive = group.items.some((item) => isNavItemActive(location.pathname, item))

            return (
              <div key={group.id}>
                <button
                  type="button"
                  onClick={() => toggleGroup(group.id)}
                  className={`w-full flex items-center justify-between px-3 py-1.5 rounded-lg text-xs font-semibold uppercase tracking-wide transition-colors ${
                    groupActive ? 'text-gray-900' : 'text-gray-400 hover:text-gray-600'
                  }`}
                >
                  <span>{group.label}</span>
                  <span className="text-[10px]">{isOpen ? '▾' : '▸'}</span>
                </button>
                {isOpen && (
                  <ul className="mt-0.5 ml-2 pl-2 border-l border-gray-100 space-y-0.5">
                    {group.items.map((item) => (
                      <li key={item.to}>
                        <NavItem item={item} pathname={location.pathname} />
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            )
          })}
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
          <SectionSubNav />
          <Outlet />
        </div>
      </main>
    </div>
  )
}
