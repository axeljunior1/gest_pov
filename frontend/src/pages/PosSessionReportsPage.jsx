import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { posApi } from '../api'
import { useAuth } from '../context/AuthContext'
import { useNotification } from '../context/NotificationContext'
import { getErrorMessage } from '../utils/errors'
import { differenceSeverityStyle, formatPosDateTime, formatPosMoney } from '../utils/posMoney'
import { PosCloseReportModal } from '../components/pos/PosCloseReportView'
import { PosSessionChip } from '../components/pos/PosWorkspaceNav'

function matchesSearch(session, query) {
  if (!query.trim()) return true
  const q = query.trim().toLowerCase()
  return (session.sessionNumber || '').toLowerCase().includes(q)
    || (session.cashierName || '').toLowerCase().includes(q)
}

function matchesDateRange(session, dateFrom, dateTo) {
  const raw = session.closedAt || session.openedAt
  if (!raw) return !dateFrom && !dateTo
  const date = new Date(raw)
  if (dateFrom) {
    const from = new Date(`${dateFrom}T00:00:00`)
    if (date < from) return false
  }
  if (dateTo) {
    const to = new Date(`${dateTo}T23:59:59.999`)
    if (date > to) return false
  }
  return true
}

export default function PosSessionReportsPage() {
  const { hasPermission } = useAuth()
  const notify = useNotification()
  const [sessions, setSessions] = useState([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [dateFrom, setDateFrom] = useState('')
  const [dateTo, setDateTo] = useState('')
  const [onlyWithDifference, setOnlyWithDifference] = useState(false)
  const [currency, setCurrency] = useState('EUR')
  const [companyName, setCompanyName] = useState('')
  const [activeSession, setActiveSession] = useState(null)
  const [report, setReport] = useState(null)
  const [loadingReport, setLoadingReport] = useState(null)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const ctx = await posApi.context()
      setActiveSession(ctx.session ?? null)
      setCurrency(ctx.publicSettings?.currency || 'EUR')
      setCompanyName(ctx.publicSettings?.companyName || '')
      setSessions(await posApi.listClosedSessions({ limit: 100 }))
    } catch (e) {
      notify.error(getErrorMessage(e))
    } finally {
      setLoading(false)
    }
  }, [notify])

  useEffect(() => {
    load()
  }, [load])

  const filteredSessions = useMemo(() => sessions.filter((s) => {
    if (onlyWithDifference && Math.abs(Number(s.differenceAmount || 0)) < 0.0001) return false
    if (!matchesSearch(s, search)) return false
    if (!matchesDateRange(s, dateFrom, dateTo)) return false
    return true
  }), [sessions, search, dateFrom, dateTo, onlyWithDifference])

  const openReport = async (sessionId) => {
    setLoadingReport(sessionId)
    try {
      setReport(await posApi.sessionReport(sessionId))
    } catch (e) {
      notify.error(getErrorMessage(e))
    } finally {
      setLoadingReport(null)
    }
  }

  if (!hasPermission('pos.report.read')) {
    return (
      <div className="flex-1 flex items-center justify-center text-slate-400">
        Permission requise (pos.report.read)
      </div>
    )
  }

  return (
    <div className="flex-1 flex flex-col min-h-0">
      <header className="px-4 py-3 bg-slate-900 border-b border-slate-800 flex flex-wrap items-center gap-4">
        <div>
          <h1 className="text-lg font-semibold">Rapports de clôture</h1>
          <p className="text-sm text-slate-400 mt-0.5">
            Historique des sessions caisse fermées · impression / PDF
          </p>
        </div>
        {activeSession && <PosSessionChip session={activeSession} centralMode />}
        <div className="ml-auto flex flex-wrap items-center gap-2">
          <Link
            to={activeSession?.sessionType === 'CASHIER' ? '/pos/pending' : '/pos'}
            className="px-3 py-2 rounded-lg border border-slate-600 text-sm text-slate-300 hover:bg-slate-800"
          >
            ← Retour caisse
          </Link>
        </div>
      </header>

      <div className="px-4 py-3 border-b border-slate-800 space-y-3">
        <div className="flex flex-wrap gap-2 items-end">
          <div className="min-w-[200px] flex-1">
            <label className="text-xs text-slate-500 block mb-1">Recherche</label>
            <input
              type="search"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="N° session, caissier…"
              className="w-full text-sm rounded-lg px-3 py-2 bg-slate-800 border border-slate-600 text-slate-100"
            />
          </div>
          <div>
            <label className="text-xs text-slate-500 block mb-1">Du</label>
            <input
              type="date"
              value={dateFrom}
              onChange={(e) => setDateFrom(e.target.value)}
              className="text-sm rounded-lg px-3 py-2 bg-slate-800 border border-slate-600 text-slate-100"
            />
          </div>
          <div>
            <label className="text-xs text-slate-500 block mb-1">Au</label>
            <input
              type="date"
              value={dateTo}
              onChange={(e) => setDateTo(e.target.value)}
              className="text-sm rounded-lg px-3 py-2 bg-slate-800 border border-slate-600 text-slate-100"
            />
          </div>
          <label className="flex items-center gap-2 text-sm text-slate-300 pb-2 cursor-pointer">
            <input
              type="checkbox"
              checked={onlyWithDifference}
              onChange={(e) => setOnlyWithDifference(e.target.checked)}
              className="rounded border-slate-600"
            />
            Écarts uniquement
          </label>
          <button
            type="button"
            onClick={load}
            className="px-3 py-2 rounded-lg bg-slate-800 text-slate-300 hover:bg-slate-700 text-sm"
          >
            Actualiser
          </button>
        </div>
      </div>

      <main className="flex-1 overflow-auto p-4">
        {loading && <p className="text-slate-400 text-sm">Chargement…</p>}
        {!loading && filteredSessions.length === 0 && (
          <div className="text-center py-16 text-slate-400 text-sm">
            Aucune session fermée ne correspond aux critères.
          </div>
        )}
        {!loading && filteredSessions.length > 0 && (
          <>
            <p className="text-xs text-slate-500 mb-3">
              {filteredSessions.length} session(s) affichée(s)
              {filteredSessions.length !== sessions.length && ` sur ${sessions.length}`}
            </p>
            <div className="overflow-x-auto">
              <table className="w-full text-sm border-collapse">
                <thead>
                  <tr className="text-left text-slate-400 border-b border-slate-700">
                    <th className="py-2 pr-4">Session</th>
                    <th className="py-2 pr-4">Caissier</th>
                    <th className="py-2 pr-4">Ouverture</th>
                    <th className="py-2 pr-4">Clôture</th>
                    <th className="py-2 pr-4">Fond</th>
                    <th className="py-2 pr-4">Écart</th>
                    <th className="py-2">Rapport</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredSessions.map((s) => {
                    const diff = Number(s.differenceAmount || 0)
                    const hasDiff = Math.abs(diff) >= 0.0001
                    const severity = hasDiff
                      ? differenceSeverityStyle(Math.abs(diff) <= 20 ? 'MINOR' : 'MAJOR')
                      : differenceSeverityStyle('BALANCED')
                    return (
                      <tr key={s.id} className="border-b border-slate-800 hover:bg-slate-900/50">
                        <td className="py-3 pr-4 font-mono text-xs">{s.sessionNumber}</td>
                        <td className="py-3 pr-4">{s.cashierName || '—'}</td>
                        <td className="py-3 pr-4 text-slate-300">{formatPosDateTime(s.openedAt)}</td>
                        <td className="py-3 pr-4 text-slate-300">{formatPosDateTime(s.closedAt)}</td>
                        <td className="py-3 pr-4">{formatPosMoney(s.openingCashAmount, currency)}</td>
                        <td className="py-3 pr-4">
                          <span className={`inline-block px-2 py-0.5 rounded text-xs border ${severity.bg} ${severity.text}`}>
                            {formatPosMoney(s.differenceAmount, currency)}
                          </span>
                        </td>
                        <td className="py-3">
                          <button
                            type="button"
                            disabled={loadingReport === s.id}
                            onClick={() => openReport(s.id)}
                            className="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 rounded-lg text-xs disabled:opacity-50"
                          >
                            Voir / imprimer
                          </button>
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>
          </>
        )}
      </main>

      {report && (
        <PosCloseReportModal
          report={report}
          currency={currency}
          companyName={companyName}
          onClose={() => setReport(null)}
        />
      )}
    </div>
  )
}
