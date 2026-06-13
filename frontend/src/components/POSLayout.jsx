import { Outlet, Link } from 'react-router-dom'
import { useEffect, useState } from 'react'
import { settingsApi } from '../api'
import { useAuth } from '../context/AuthContext'
import { isPosOnlyUser } from '../utils/auth'

export default function POSLayout() {
  const { user, logout } = useAuth()
  const [companyName, setCompanyName] = useState('ERP')
  const posOnly = isPosOnlyUser(user)

  useEffect(() => {
    settingsApi.getPublic()
      .then((s) => { if (s.companyName) setCompanyName(s.companyName) })
      .catch(() => {})
  }, [])

  return (
    <div className="min-h-screen bg-slate-950 text-white flex flex-col">
      <div className="px-4 py-2 border-b border-slate-800 flex items-center justify-between text-xs text-slate-400">
        <span>{companyName} — Caisse</span>
        <div className="flex items-center gap-4">
          {!posOnly && (
            <Link to="/" className="hover:text-white underline underline-offset-2">
              ← Retour back-office
            </Link>
          )}
          <button type="button" onClick={logout} className="hover:text-white">
            Déconnexion
          </button>
        </div>
      </div>
      <Outlet />
    </div>
  )
}
