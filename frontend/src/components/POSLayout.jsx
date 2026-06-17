import { Outlet, Link } from 'react-router-dom'
import { useEffect, useState } from 'react'
import { settingsApi } from '../api'
import { useAuth } from '../context/AuthContext'
import { getBackOfficeEntryPath } from '../config/navGroups'
import { getPosRoleLabel } from '../utils/auth'
import PosWorkspaceNav from './pos/PosWorkspaceNav'

export default function POSLayout() {
  const { user, logout, hasPermission } = useAuth()
  const [companyName, setCompanyName] = useState('ERP')
  const [companyLogoUrl, setCompanyLogoUrl] = useState(null)
  const roleLabel = getPosRoleLabel(user)
  const navOptions = { userRoles: user?.roles ?? [] }
  const backOfficePath = getBackOfficeEntryPath(hasPermission, navOptions)
  const showBackOffice = backOfficePath != null

  useEffect(() => {
    settingsApi.getPublic()
      .then((s) => {
        if (s.companyName) setCompanyName(s.companyName)
        if (s.companyLogoUrl) setCompanyLogoUrl(s.companyLogoUrl)
      })
      .catch(() => {})
  }, [])

  return (
    <div className="min-h-screen bg-slate-950 text-white flex flex-col pos-theme">
      <div className="px-4 py-3 border-b border-slate-800 flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-3">
          {companyLogoUrl && (
            <img src={companyLogoUrl} alt="" className="h-8 w-8 object-contain rounded bg-white/10" />
          )}
          <div>
            <p className="font-semibold text-base">{companyName}</p>
            <p className="text-xs text-slate-500">Point de vente</p>
          </div>
        </div>
        <div className="flex flex-wrap items-center gap-3 text-sm">
          <span className="hidden sm:inline text-slate-500">
            {user?.firstName} {user?.lastName}
          </span>
          <span className="px-2.5 py-1 rounded-md bg-slate-800 border border-slate-700 text-slate-200 text-xs font-medium">
            {roleLabel}
          </span>
          {showBackOffice && (
            <Link
              to={backOfficePath}
              className="px-3 py-1.5 rounded-lg border border-slate-700 text-slate-300 hover:bg-slate-800 hover:text-white transition-colors"
            >
              ← Back-office
            </Link>
          )}
          <button
            type="button"
            onClick={logout}
            className="px-3 py-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
          >
            Déconnexion
          </button>
        </div>
      </div>
      <PosWorkspaceNav />
      <Outlet />
    </div>
  )
}
