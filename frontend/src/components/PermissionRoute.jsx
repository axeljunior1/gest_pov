import { Link, Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { Card, Button } from './ui'
import { getDefaultAppPath, isSuperAdmin } from '../utils/auth'
import { resolvePermissionAccess } from '../utils/permissions'

export default function PermissionRoute({ permission, anyOf, redirect }) {
  const { loading, user, hasPermission, hasAnyPermission } = useAuth()

  if (loading) {
    return (
      <div className="min-h-[50vh] flex items-center justify-center text-sm text-gray-500">
        Chargement…
      </div>
    )
  }

  const { allowed, requiredLabel } = resolvePermissionAccess({
    permission,
    anyOf: anyOf ?? [],
    hasPermission,
    hasAnyPermission,
    isSuperAdmin: isSuperAdmin(user),
  })

  if (!allowed) {
    if (redirect !== false) {
      const fallback = getDefaultAppPath(user, hasPermission)
      return <Navigate to={fallback} replace />
    }
    const label = requiredLabel
    return (
      <div className="min-h-[50vh] flex items-center justify-center">
        <Card className="p-8 max-w-md text-center">
          <h2 className="text-lg font-semibold text-gray-900">Accès refusé</h2>
          <p className="text-sm text-gray-500 mt-2">
            Permission requise : <code className="text-xs bg-gray-100 px-1 rounded">{label}</code>
          </p>
          <Link to={getDefaultAppPath(user, hasPermission)} className="inline-block mt-6">
            <Button variant="secondary">Retour</Button>
          </Link>
        </Card>
      </div>
    )
  }

  return <Outlet />
}
