import { Navigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import ProductsPage from '../pages/ProductsPage'
import { getDefaultAppPath } from '../utils/auth'

/** Route d'accueil : produits si autorisé, sinon redirection selon permissions. */
export default function HomeRedirect() {
  const { user, hasPermission } = useAuth()
  const target = getDefaultAppPath(user, hasPermission)

  if (target === '/') {
    return <ProductsPage />
  }

  return <Navigate to={target} replace />
}
