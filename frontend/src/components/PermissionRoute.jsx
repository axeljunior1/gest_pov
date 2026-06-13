import { Link, Outlet } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { Card, Button } from './ui'

export default function PermissionRoute({ permission, anyOf }) {
  const { loading, hasPermission, hasAnyPermission } = useAuth()

  if (loading) {
    return (
      <div className="min-h-[50vh] flex items-center justify-center text-sm text-gray-500">
        Chargement…
      </div>
    )
  }

  const allowed = anyOf?.length
    ? hasAnyPermission(...anyOf)
    : hasPermission(permission)

  if (!allowed) {
    const label = anyOf?.join(' | ') || permission
    return (
      <div className="min-h-[50vh] flex items-center justify-center">
        <Card className="p-8 max-w-md text-center">
          <h2 className="text-lg font-semibold text-gray-900">Accès refusé</h2>
          <p className="text-sm text-gray-500 mt-2">
            Permission requise : <code className="text-xs bg-gray-100 px-1 rounded">{label}</code>
          </p>
          <Link to="/" className="inline-block mt-6">
            <Button variant="secondary">Retour à l’accueil</Button>
          </Link>
        </Card>
      </div>
    )
  }

  return <Outlet />
}
