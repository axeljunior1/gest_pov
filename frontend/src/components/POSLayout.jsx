import { Outlet, Link } from 'react-router-dom'
import { useEffect, useState } from 'react'
import { settingsApi, posApi } from '../api'
import { useAuth } from '../context/AuthContext'
import { isPosOnlyUser, isSellerOnlyUser } from '../utils/auth'

export default function POSLayout() {
  const { user, logout, hasPermission } = useAuth()
  const [companyName, setCompanyName] = useState('ERP')
  const [centralMode, setCentralMode] = useState(false)
  const posOnly = isPosOnlyUser(user)

  useEffect(() => {
    settingsApi.getPublic()
      .then((s) => { if (s.companyName) setCompanyName(s.companyName) })
      .catch(() => {})
    posApi.context()
      .then((ctx) => setCentralMode(
        ctx?.posConfig?.salesFlowMode === 'CENTRAL_CASHIER'
        || ctx?.posConfig?.cashHandlingMode === 'CENTRAL_CASHIER',
      ))
      .catch(() => {})
  }, [])

  return (
    <div className="min-h-screen bg-slate-950 text-white flex flex-col pos-theme">
      <div className="px-4 py-2 border-b border-slate-800 flex items-center justify-between text-xs text-slate-400">
        <span>{companyName} — Caisse</span>
        <div className="flex items-center gap-4">
          {centralMode && hasPermission('pos.payment.collect') && (
            <Link to="/pos/pending" className="hover:text-white underline underline-offset-2">
              Ventes à encaisser
            </Link>
          )}
          {centralMode && isSellerOnlyUser(user) && (
            <Link to="/pos" className="hover:text-white underline underline-offset-2">
              Préparation ventes
            </Link>
          )}
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
