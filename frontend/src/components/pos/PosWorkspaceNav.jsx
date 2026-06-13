import { Link, useLocation } from 'react-router-dom'
import { useCallback, useEffect, useState } from 'react'
import { posApi } from '../../api'
import { useAuth } from '../../context/AuthContext'
import {
  canCollectPayment,
  canPrepareSales,
  getPosRoleLabel,
  hasDualPosRole,
} from '../../utils/auth'

const WORKSPACES = {
  SALES: {
    id: 'sales',
    label: 'Préparation ventes',
    subtitle: 'Panier, client, envoi à la caisse',
    sessionLabel: 'Session vente',
    path: '/pos',
    icon: '🛒',
    activeRing: 'ring-indigo-500/50 border-indigo-500',
    activeBg: 'bg-indigo-950/70',
    idleBorder: 'border-slate-700 hover:border-indigo-600/60 hover:bg-slate-900/80',
    badge: 'bg-indigo-500/20 text-indigo-200 border-indigo-500/40',
    dot: 'bg-indigo-400',
  },
  CASHIER: {
    id: 'cashier',
    label: 'Encaissement',
    subtitle: 'Paiements et clôture de caisse',
    sessionLabel: 'Session caisse',
    path: '/pos/pending',
    icon: '💰',
    activeRing: 'ring-emerald-500/50 border-emerald-500',
    activeBg: 'bg-emerald-950/70',
    idleBorder: 'border-slate-700 hover:border-emerald-600/60 hover:bg-slate-900/80',
    badge: 'bg-emerald-500/20 text-emerald-200 border-emerald-500/40',
    dot: 'bg-emerald-400',
  },
}

const UNIFIED = {
  label: 'Point de vente',
  subtitle: 'Vente et encaissement sur le même écran',
  sessionLabel: 'Session caisse',
  path: '/pos',
  icon: '🏪',
}

function sessionStatus(session, expectedType) {
  if (!session) {
    return { open: false, text: 'Aucune session ouverte' }
  }
  if (session.sessionType === expectedType) {
    return {
      open: true,
      text: `${session.sessionNumber} · ${session.warehouseCode || '—'}`,
    }
  }
  const other = session.sessionType === 'SALES' ? 'vente' : 'caisse'
  return {
    open: false,
    text: `Session ${other} ouverte ailleurs`,
  }
}

function WorkspaceCard({ ws, active, session, showSessionHint }) {
  const status = sessionStatus(session, ws.id === 'sales' ? 'SALES' : 'CASHIER')
  return (
    <Link
      to={ws.path}
      className={`flex-1 min-w-[220px] rounded-xl border-2 p-4 transition-all ${
        active
          ? `${ws.activeBg} ${ws.activeRing} ring-2`
          : ws.idleBorder
      }`}
    >
      <div className="flex items-start gap-3">
        <span className="text-2xl leading-none" aria-hidden>{ws.icon}</span>
        <div className="min-w-0 flex-1">
          <p className="font-semibold text-base text-white">{ws.label}</p>
          <p className="text-sm text-slate-400 mt-0.5">{ws.subtitle}</p>
          {showSessionHint && (
            <div className="mt-3 flex items-center gap-2 text-sm">
              <span
                className={`w-2.5 h-2.5 rounded-full shrink-0 ${status.open ? ws.dot : 'bg-slate-600'}`}
                aria-hidden
              />
              <span className={status.open ? 'text-slate-200' : 'text-slate-500'}>
                {status.open ? status.text : status.text}
              </span>
            </div>
          )}
        </div>
        {active && (
          <span className={`shrink-0 px-2 py-0.5 rounded-md text-xs font-medium border ${ws.badge}`}>
            Actif
          </span>
        )}
      </div>
    </Link>
  )
}

export default function PosWorkspaceNav() {
  const location = useLocation()
  const { user, hasPermission } = useAuth()
  const [centralMode, setCentralMode] = useState(false)
  const [session, setSession] = useState(null)

  const canPrepare = canPrepareSales(user)
  const canCollect = canCollectPayment(user)
  const dualRole = hasDualPosRole(user)
  const roleLabel = getPosRoleLabel(user)
  const canReprint = hasPermission('pos.ticket.print') || hasPermission('pos.ticket.reprint')

  const refresh = useCallback(async () => {
    try {
      const ctx = await posApi.context()
      setCentralMode(
        ctx?.posConfig?.salesFlowMode === 'CENTRAL_CASHIER'
        || ctx?.posConfig?.cashHandlingMode === 'CENTRAL_CASHIER',
      )
      setSession(ctx.session ?? null)
    } catch {
      /* ignore — nav reste utilisable */
    }
  }, [])

  useEffect(() => {
    refresh()
    const t = setInterval(refresh, 20000)
    const onSessionChange = () => refresh()
    window.addEventListener('pos-session-changed', onSessionChange)
    return () => {
      clearInterval(t)
      window.removeEventListener('pos-session-changed', onSessionChange)
    }
  }, [refresh, location.pathname])

  const onSales = location.pathname === '/pos' || location.pathname === '/pos/'
  const onCashier = location.pathname.startsWith('/pos/pending')

  if (!canPrepare && !canCollect) return null

  /** Poste choisi : masquer les cartes de sélection jusqu'à fermeture de session. */
  if (session) return null

  if (!centralMode) {
    const status = sessionStatus(session, 'CASHIER')
    return (
      <div className="px-4 py-3 bg-slate-900/80 border-b border-slate-800">
        <div className="flex flex-wrap items-center gap-3">
          <div className={`flex-1 min-w-[240px] rounded-xl border-2 p-4 ${WORKSPACES.CASHIER.activeBg} ${WORKSPACES.CASHIER.activeRing} ring-2`}>
            <div className="flex items-start gap-3">
              <span className="text-2xl" aria-hidden>{UNIFIED.icon}</span>
              <div>
                <p className="font-semibold text-base">{UNIFIED.label}</p>
                <p className="text-sm text-slate-400 mt-0.5">{UNIFIED.subtitle}</p>
                <div className="mt-2 flex items-center gap-2 text-sm">
                  <span className={`w-2.5 h-2.5 rounded-full ${status.open ? WORKSPACES.CASHIER.dot : 'bg-slate-600'}`} />
                  <span className={status.open ? 'text-emerald-200' : 'text-slate-500'}>
                    {status.open ? status.text : status.text}
                  </span>
                </div>
              </div>
            </div>
          </div>
          <div className="text-sm text-slate-400">
            Rôle&nbsp;: <span className="text-slate-200 font-medium">{roleLabel}</span>
          </div>
          {canReprint && (
            <Link
              to="/pos/history"
              className="px-4 py-2 rounded-lg border border-slate-600 text-sm text-slate-200 hover:bg-slate-800"
            >
              Ventes passées
            </Link>
          )}
        </div>
      </div>
    )
  }

  const showSales = canPrepare
  const showCashier = canCollect

  return (
    <div className="px-4 py-3 bg-slate-900/80 border-b border-slate-800">
      <div className="flex flex-wrap items-center justify-between gap-3 mb-3">
        <p className="text-sm text-slate-400">
          Mode caisse centrale — choisissez votre poste
        </p>
        <div className="flex items-center gap-2">
          <span className="text-xs uppercase tracking-wide text-slate-500">Votre rôle</span>
          <span className="px-3 py-1 rounded-lg bg-slate-800 border border-slate-600 text-sm font-medium text-slate-100">
            {roleLabel}
          </span>
          {dualRole && (
            <span className="hidden sm:inline text-xs text-slate-500">
              Accès aux deux écrans
            </span>
          )}
        </div>
      </div>
      <div className="flex flex-wrap gap-3">
        {showSales && (
          <WorkspaceCard
            ws={WORKSPACES.SALES}
            active={onSales}
            session={session}
            showSessionHint
          />
        )}
        {showCashier && (
          <WorkspaceCard
            ws={WORKSPACES.CASHIER}
            active={onCashier}
            session={session}
            showSessionHint
          />
        )}
      </div>
      {canReprint && (
        <div className="mt-3 flex justify-end">
          <Link
            to="/pos/history"
            className="px-4 py-2 rounded-lg border border-slate-600 text-sm text-slate-200 hover:bg-slate-800"
          >
            Consulter les ventes passées
          </Link>
        </div>
      )}
    </div>
  )
}

/** Bandeau compact de session pour les en-têtes de page POS. */
export function PosSessionChip({ session, expectedType, centralMode }) {
  if (!session) return null

  const isSales = session.sessionType === 'SALES'
  const meta = isSales ? WORKSPACES.SALES : WORKSPACES.CASHIER
  const wrongType = expectedType && session.sessionType !== expectedType

  return (
    <div className={`inline-flex flex-wrap items-center gap-2 px-3 py-1.5 rounded-lg border text-sm ${
      wrongType
        ? 'border-amber-600/50 bg-amber-950/40 text-amber-100'
        : `${meta.badge} border`
    }`}>
      <span className="text-base leading-none" aria-hidden>{meta.icon}</span>
      <span className="font-semibold">{meta.sessionLabel}</span>
      <span className="text-slate-300 font-mono text-xs">{session.sessionNumber}</span>
      {!wrongType && session.warehouseCode && (
        <span className="text-slate-400 text-xs">· {session.warehouseCode}</span>
      )}
      {wrongType && centralMode && (
        <span className="text-xs text-amber-200/90">— mauvais écran pour cette session</span>
      )}
    </div>
  )
}

export function PosSessionTypeBadge({ type, size = 'md' }) {
  const meta = type === 'SALES' ? WORKSPACES.SALES : WORKSPACES.CASHIER
  const sizes = size === 'lg'
    ? 'px-5 py-3 text-base gap-3'
    : 'px-3 py-1.5 text-sm gap-2'
  return (
    <span className={`inline-flex items-center rounded-xl border font-semibold ${meta.badge} border ${sizes}`}>
      <span className="text-xl leading-none" aria-hidden>{meta.icon}</span>
      {meta.sessionLabel}
    </span>
  )
}
