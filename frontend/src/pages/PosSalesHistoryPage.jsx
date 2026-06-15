import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { posApi } from '../api'
import { useAuth } from '../context/AuthContext'
import { useNotification } from '../context/NotificationContext'
import { getErrorMessage } from '../utils/errors'
import { formatPosMoney } from '../utils/posMoney'
import { saleStatusLabel } from '../utils/saleStatus'
import { canBrowseSalesBackOffice, canBrowseReturnsBackOffice } from '../utils/saleDisplay'
import {
  canCollectPayment,
  canPrepareSales,
  getPosRoleLabel,
} from '../utils/auth'
import { PosSessionChip } from '../components/pos/PosWorkspaceNav'
import { PosInvoiceModal, PosTicketModal } from '../components/pos/PosPrintModals'

function saleDateValue(sale) {
  const raw = sale.paidAt || sale.validatedAt
  return raw ? new Date(raw) : null
}

function matchesSearch(sale, query) {
  if (!query.trim()) return true
  const q = query.trim().toLowerCase()
  return (sale.saleNumber || '').toLowerCase().includes(q)
    || (sale.customerName || '').toLowerCase().includes(q)
    || (sale.customerNumber || '').toLowerCase().includes(q)
}

function matchesDateRange(sale, dateFrom, dateTo) {
  const date = saleDateValue(sale)
  if (!date) return !dateFrom && !dateTo
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

export default function PosSalesHistoryPage() {
  const { user, hasPermission, hasAnyPermission } = useAuth()
  const notify = useNotification()
  const [session, setSession] = useState(null)
  const [sales, setSales] = useState([])
  const [loading, setLoading] = useState(true)
  const [sessionOnly, setSessionOnly] = useState(true)
  const [search, setSearch] = useState('')
  const [dateFrom, setDateFrom] = useState('')
  const [dateTo, setDateTo] = useState('')
  const [onlyWithCustomer, setOnlyWithCustomer] = useState(false)
  const [ticket, setTicket] = useState(null)
  const [invoice, setInvoice] = useState(null)
  const [printing, setPrinting] = useState(null)
  const [currency, setCurrency] = useState('EUR')
  const canPrint = hasPermission('pos.ticket.print') || hasPermission('pos.ticket.reprint')
  const canOpenBackOfficeSale = canBrowseSalesBackOffice(hasPermission, hasAnyPermission)
  const canOpenBackOfficeReturns = canBrowseReturnsBackOffice(hasPermission, hasAnyPermission)
  const roleLabel = getPosRoleLabel(user)
  const scopeHint = canPrepareSales(user) && canCollectPayment(user)
    ? 'Ventes où vous êtes vendeur ou caissier'
    : canPrepareSales(user)
      ? 'Ventes que vous avez préparées'
      : 'Ventes que vous avez encaissées'

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const ctx = await posApi.context()
      const activeSession = ctx.session ?? null
      setSession(activeSession)
      setCurrency(ctx.publicSettings?.currency || 'EUR')
      const list = await posApi.listCompletedSales({
        sessionOnly: sessionOnly && !!activeSession,
        limit: 100,
      })
      setSales(list)
    } catch (e) {
      notify.error(getErrorMessage(e))
    } finally {
      setLoading(false)
    }
  }, [notify, sessionOnly])

  useEffect(() => {
    load()
  }, [load])

  const filteredSales = useMemo(() => sales.filter((s) => {
    if (onlyWithCustomer && !s.customerId) return false
    if (!matchesSearch(s, search)) return false
    if (!matchesDateRange(s, dateFrom, dateTo)) return false
    return true
  }), [sales, search, dateFrom, dateTo, onlyWithCustomer])

  const printTicket = async (saleId) => {
    setPrinting(saleId)
    try {
      setTicket(await posApi.ticket(saleId))
    } catch (e) {
      notify.error(getErrorMessage(e))
    } finally {
      setPrinting(null)
    }
  }

  const printInvoice = async (saleId) => {
    setPrinting(saleId)
    try {
      setInvoice(await posApi.invoice(saleId))
    } catch (e) {
      notify.error(getErrorMessage(e))
    } finally {
      setPrinting(null)
    }
  }

  if (!canPrint) {
    return (
      <div className="flex-1 flex items-center justify-center text-slate-400">
        Permission requise (pos.ticket.print ou pos.ticket.reprint)
      </div>
    )
  }

  return (
    <div className="flex-1 flex flex-col min-h-0">
      <header className="px-4 py-3 bg-slate-900 border-b border-slate-800 flex flex-wrap items-center gap-4">
        <div>
          <h1 className="text-lg font-semibold">Ventes passées</h1>
          <p className="text-sm text-slate-400 mt-0.5">
            Réimpression ticket ou facture · {scopeHint}
          </p>
        </div>
        {session && <PosSessionChip session={session} centralMode />}
        <div className="ml-auto flex flex-wrap items-center gap-2">
          {canOpenBackOfficeSale && (
            <Link
              to="/sales"
              className="px-3 py-2 rounded-lg border border-emerald-700/50 text-sm text-emerald-300 hover:bg-emerald-950/40"
            >
              Consultation ventes →
            </Link>
          )}
          {canOpenBackOfficeReturns && (
            <Link
              to="/returns"
              className="px-3 py-2 rounded-lg border border-amber-700/50 text-sm text-amber-200 hover:bg-amber-950/40"
            >
              Consultation retours →
            </Link>
          )}
          <Link
            to={session?.sessionType === 'CASHIER' ? '/pos/pending' : '/pos'}
            className="px-3 py-2 rounded-lg border border-slate-600 text-sm text-slate-300 hover:bg-slate-800"
          >
            ← Retour caisse
          </Link>
        </div>
      </header>

      <div className="px-4 py-3 border-b border-slate-800 space-y-3">
        <div className="flex flex-wrap items-center gap-3 text-sm">
          <span className="text-slate-500">Rôle : {roleLabel}</span>
          {session ? (
            <div className="flex rounded-lg border border-slate-700 overflow-hidden">
              <button
                type="button"
                onClick={() => setSessionOnly(true)}
                className={`px-4 py-2 text-sm ${sessionOnly ? 'bg-slate-700 text-white' : 'text-slate-400 hover:bg-slate-800'}`}
              >
                Session en cours
              </button>
              <button
                type="button"
                onClick={() => setSessionOnly(false)}
                className={`px-4 py-2 text-sm ${!sessionOnly ? 'bg-slate-700 text-white' : 'text-slate-400 hover:bg-slate-800'}`}
              >
                Toutes mes ventes
              </button>
            </div>
          ) : (
            <span className="text-slate-500">Historique de vos ventes finalisées</span>
          )}
          <button
            type="button"
            onClick={load}
            className="ml-auto px-3 py-1.5 rounded-lg bg-slate-800 text-slate-300 hover:bg-slate-700 text-sm"
          >
            Actualiser
          </button>
        </div>

        <div className="flex flex-wrap gap-2 items-end">
          <div className="min-w-[200px] flex-1">
            <label className="text-xs text-slate-500 block mb-1">Recherche</label>
            <input
              type="search"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="N° vente, client…"
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
              checked={onlyWithCustomer}
              onChange={(e) => setOnlyWithCustomer(e.target.checked)}
              className="rounded border-slate-600"
            />
            Client renseigné uniquement
          </label>
          {(search || dateFrom || dateTo || onlyWithCustomer) && (
            <button
              type="button"
              onClick={() => {
                setSearch('')
                setDateFrom('')
                setDateTo('')
                setOnlyWithCustomer(false)
              }}
              className="px-3 py-2 text-sm text-slate-400 hover:text-white"
            >
              Effacer filtres
            </button>
          )}
        </div>
      </div>

      <main className="flex-1 overflow-auto p-4">
        {loading && <p className="text-slate-400 text-sm">Chargement…</p>}
        {!loading && filteredSales.length === 0 && (
          <div className="text-center py-16 text-slate-400 text-sm max-w-md mx-auto space-y-2">
            <p>Aucune vente ne correspond aux critères.</p>
            {sales.length > 0 && (
              <p className="text-xs text-slate-500">{sales.length} vente(s) chargée(s) — affinez ou effacez les filtres.</p>
            )}
            {sessionOnly && session && sales.length === 0 && (
              <p className="text-xs text-slate-500">
                Les ventes apparaîtront ici après encaissement pendant cette session.
              </p>
            )}
          </div>
        )}
        {!loading && filteredSales.length > 0 && (
          <>
            <p className="text-xs text-slate-500 mb-3">
              {filteredSales.length} vente(s) affichée(s)
              {filteredSales.length !== sales.length && ` sur ${sales.length}`}
            </p>
            <div className="overflow-x-auto">
              <table className="w-full text-sm border-collapse">
                <thead>
                  <tr className="text-left text-slate-400 border-b border-slate-700">
                    <th className="py-2 pr-4">N° vente</th>
                    <th className="py-2 pr-4">Date</th>
                    <th className="py-2 pr-4">Client</th>
                    <th className="py-2 pr-4">Vendeur</th>
                    <th className="py-2 pr-4">Caissier</th>
                    <th className="py-2 pr-4">Statut</th>
                    <th className="py-2 pr-4">Montant</th>
                    <th className="py-2">{canOpenBackOfficeSale ? 'Actions' : 'Impression'}</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredSales.map((s) => {
                    const paidAt = saleDateValue(s)
                    const hasCustomer = Boolean(s.customerId)
                    return (
                      <tr key={s.id} className="border-b border-slate-800 hover:bg-slate-900/50">
                        <td className="py-3 pr-4 font-mono text-xs">{s.saleNumber}</td>
                        <td className="py-3 pr-4 text-slate-300">
                          {paidAt ? paidAt.toLocaleString('fr-FR') : '—'}
                        </td>
                        <td className="py-3 pr-4">
                          <div className="flex flex-wrap items-center gap-2">
                            <span>{s.customerName || '—'}</span>
                            {hasCustomer ? (
                              <span className="px-2 py-0.5 rounded text-xs bg-indigo-900/50 text-indigo-200 border border-indigo-700/40">
                                Client
                              </span>
                            ) : (
                              <span className="px-2 py-0.5 rounded text-xs bg-slate-800 text-slate-400 border border-slate-700">
                                Comptoir
                              </span>
                            )}
                          </div>
                        </td>
                        <td className="py-3 pr-4">{s.sellerName || '—'}</td>
                        <td className="py-3 pr-4">{s.cashierName || '—'}</td>
                        <td className="py-3 pr-4">
                          <span className="inline-block px-2 py-0.5 rounded bg-slate-800 text-slate-300 text-xs">
                            {saleStatusLabel(s.status)}
                          </span>
                        </td>
                        <td className="py-3 pr-4 text-emerald-400 font-medium">
                          {formatPosMoney(s.total, currency)}
                        </td>
                        <td className="py-3">
                          <div className="flex flex-wrap gap-2">
                            {canOpenBackOfficeSale && (
                              <Link
                                to={`/sales/${s.id}`}
                                className="px-3 py-1.5 bg-emerald-900/40 border border-emerald-700/50 hover:bg-emerald-900/60 rounded-lg text-xs text-emerald-200"
                              >
                                Fiche vente
                              </Link>
                            )}
                            <button
                              type="button"
                              disabled={printing === s.id}
                              onClick={() => printTicket(s.id)}
                              className="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 rounded-lg text-xs disabled:opacity-50"
                            >
                              Ticket
                            </button>
                            <button
                              type="button"
                              disabled={printing === s.id}
                              title={hasCustomer ? 'Facture client' : 'Facture comptoir (sans client identifié)'}
                              onClick={() => printInvoice(s.id)}
                              className={`px-3 py-1.5 rounded-lg text-xs disabled:opacity-50 ${
                                hasCustomer
                                  ? 'bg-indigo-900/60 border border-indigo-700/50 hover:bg-indigo-900'
                                  : 'bg-slate-800 border border-slate-600 hover:bg-slate-700'
                              }`}
                            >
                              Facture
                            </button>
                          </div>
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

      {ticket && <PosTicketModal ticket={ticket} onClose={() => setTicket(null)} />}
      {invoice && <PosInvoiceModal invoice={invoice} onClose={() => setInvoice(null)} />}
    </div>
  )
}
