import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { isPosOnlyUser } from '../utils/auth'

export default function BackOfficeRoute() {
  const { user, loading } = useAuth()

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center text-sm text-gray-500">
        Chargement…
      </div>
    )
  }

  if (isPosOnlyUser(user)) {
    return <Navigate to="/pos" replace />
  }

  return <Outlet />
}
