import { NavLink, useLocation } from 'react-router-dom'
import { useMemo } from 'react'
import { useAuth } from '../context/AuthContext'
import { findActiveGroup, filterVisibleGroups, isNavItemActive, navGroups } from '../config/navGroups'

export default function SectionSubNav() {
  const { pathname } = useLocation()
  const { hasPermission } = useAuth()

  const activeGroup = useMemo(() => {
    const visible = filterVisibleGroups(navGroups, hasPermission)
    return findActiveGroup(pathname, visible)
  }, [pathname, hasPermission])

  if (!activeGroup) return null

  return (
    <div className="mb-6 pb-4 border-b border-gray-200">
      <p className="text-xs font-semibold uppercase tracking-wide text-gray-400 mb-3">
        {activeGroup.label}
      </p>
      <nav className="flex flex-wrap gap-1">
        {activeGroup.items.map((item) => {
          const active = isNavItemActive(pathname, item)
          return (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === '/'}
              className={() =>
                `px-3 py-1.5 rounded-lg text-sm transition-colors ${
                  active
                    ? 'bg-gray-900 text-white font-medium'
                    : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900'
                }`
              }
            >
              {item.label}
            </NavLink>
          )
        })}
      </nav>
    </div>
  )
}
