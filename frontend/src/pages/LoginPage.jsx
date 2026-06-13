import { useState } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useNotification } from '../context/NotificationContext'
import { getDefaultAppPath, isPosOnlyUser } from '../utils/auth'
import { getErrorMessage } from '../utils/errors'

export default function LoginPage() {
  const { login, isAuthenticated, user } = useAuth()
  const notify = useNotification()
  const location = useLocation()
  const [email, setEmail] = useState('admin@erp.local')
  const [password, setPassword] = useState('')
  const [submitting, setSubmitting] = useState(false)

  if (isAuthenticated) {
    const from = location.state?.from?.pathname
    const redirect = from && !isPosOnlyUser(user) ? from : getDefaultAppPath(user)
    return <Navigate to={redirect} replace />
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setSubmitting(true)
    try {
      await login(email.trim(), password)
    } catch (err) {
      notify.error(getErrorMessage(err, 'Connexion impossible'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4">
      <div className="w-full max-w-md bg-white border border-gray-200 rounded-2xl shadow-sm p-8">
        <div className="mb-8 text-center">
          <h1 className="text-2xl font-semibold tracking-tight">ERP Produits</h1>
          <p className="text-sm text-gray-500 mt-2">Connectez-vous pour accéder à l’application</p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label htmlFor="email" className="block text-sm font-medium text-gray-700 mb-1">
              Email
            </label>
            <input
              id="email"
              type="email"
              autoComplete="username"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-gray-900"
            />
          </div>

          <div>
            <label htmlFor="password" className="block text-sm font-medium text-gray-700 mb-1">
              Mot de passe
            </label>
            <input
              id="password"
              type="password"
              autoComplete="current-password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-gray-900"
            />
          </div>

          <button
            type="submit"
            disabled={submitting}
            className="w-full rounded-lg bg-gray-900 text-white py-2.5 text-sm font-medium hover:bg-gray-800 disabled:opacity-60"
          >
            {submitting ? 'Connexion…' : 'Se connecter'}
          </button>
        </form>

        <p className="text-xs text-gray-400 mt-6 text-center">
          Admin : admin@erp.local — Caissier : caissier@erp.local
        </p>
      </div>
    </div>
  )
}
