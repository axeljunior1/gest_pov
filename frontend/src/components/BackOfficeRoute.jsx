import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { hasBackOfficeMenuAccess } from '../config/navGroups'
import { getDefaultAppPath, isPosOnlyUser } from '../utils/auth'

export default function BackOfficeRoute() {
  const { user, loading, hasPermission } = useAuth()

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center text-sm text-gray-500">
        Chargement…
      </div>
    )
  }

  if (isPosOnlyUser(user)) {
    return <Navigate to={getDefaultAppPath(user, hasPermission)} replace />
  }

  const navOptions = { userRoles: user?.roles ?? [] }
  if (!hasBackOfficeMenuAccess(hasPermission, navOptions) && hasPermission('pos.sale.read')) {
    return <Navigate to="/pos" replace />
  }

  return <Outlet />
}
