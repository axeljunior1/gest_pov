import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { posApi } from '../api'
import { useAuth } from '../context/AuthContext'
import { useNotification } from '../context/NotificationContext'
import { getErrorMessage } from '../utils/errors'
import { isCashierOnlyUser } from '../utils/auth'
import { saleStatusLabel } from '../utils/saleStatus'
import { formatPosMoney } from '../utils/posMoney'
import CashSessionCloseModal from '../components/pos/CashSessionCloseModal'
import CashSessionOpenModal from '../components/pos/CashSessionOpenModal'

function PaymentModal({ sale, currency, onClose, onPaid }) {
  const [payments, setPayments] = useState([{ method: 'CASH', amount: sale?.total || 0 }])
  const [cashReceived, setCashReceived] = useState('')
  const [loading, setLoading] = useState(false)
  const notify = useNotification()

  const total = Number(sale?.total || 0)
  const paid = payments.reduce((s, p) => s + Number(p.amount || 0), 0)
  const change = payments.some((p) => p.method === 'CASH') && cashReceived
    ? Math.max(0, Number(cashReceived) - total)
    : Math.max(0, paid - total)

  const submit = async () => {
    setLoading(true)
    try {
      const result = await posApi.validateSale(sale.id, {
        payments: payments.map((p) => ({ method: p.method, amount: Number(p.amount) })),
        cashReceived: cashReceived ? Number(cashReceived) : undefined,
      })
      onPaid(result)
    } catch (e) {
      notify.error(getErrorMessage(e))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="fixed inset-0 bg-black/70 flex items-center justify-center z-50 p-4">
      <div className="bg-slate-900 border border-slate-700 rounded-xl w-full max-w-lg p-6">
        <h3 className="text-lg font-semibold mb-1">Encaisser — {sale?.saleNumber}</h3>
        <p className="text-sm text-slate-400 mb-4">Vendeur : {sale?.sellerName || sale?.cashierName} · Total {formatPosMoney(total, currency)}</p>
        {payments.map((p, i) => (
          <div key={i} className="flex gap-2 mb-2">
            <select
              className="rounded-lg px-3 py-2 text-sm flex-1 border border-slate-600 bg-slate-800"
              value={p.method}
              onChange={(e) => {
                const next = [...payments]
                next[i].method = e.target.value
                setPayments(next)
              }}
            >
              <option value="CASH">Espèces</option>
              <option value="CARD">Carte</option>
              <option value="MOBILE_MONEY">Mobile money</option>
              <option value="BANK_TRANSFER">Virement</option>
              <option value="OTHER">Autre</option>
            </select>
            <input
              type="number"
              className="rounded-lg px-3 py-2 text-sm w-32 border border-slate-600 bg-slate-800"
              value={p.amount}
              onChange={(e) => {
                const next = [...payments]
                next[i].amount = e.target.value
                setPayments(next)
              }}
            />
          </div>
        ))}
        <div className="mb-4">
          <label className="text-xs text-slate-400 block mb-1">Montant reçu (espèces)</label>
          <input
            type="number"
            className="w-full rounded-lg px-3 py-2 border border-slate-600 bg-slate-800"
            value={cashReceived}
            onChange={(e) => setCashReceived(e.target.value)}
            placeholder="Optionnel"
          />
          {change > 0 && (
            <p className="text-emerald-400 text-sm mt-2">Monnaie à rendre : {formatPosMoney(change, currency)}</p>
          )}
        </div>
        <div className="flex gap-2 justify-end">
          <button type="button" onClick={onClose} className="px-4 py-2 rounded-lg bg-slate-700 text-sm">Annuler</button>
          <button type="button" disabled={loading || paid < total} onClick={submit}
            className="px-4 py-2 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-sm font-medium disabled:opacity-50">
            Valider paiement
          </button>
        </div>
      </div>
    </div>
  )
}

function TicketModal({ ticket, onClose }) {
  if (!ticket) return null
  return (
    <div className="fixed inset-0 bg-black/70 flex items-center justify-center z-50 p-4">
      <div className="pos-light-panel rounded-xl w-full max-w-sm p-6 font-mono text-sm">
        <p className="font-bold text-center">{ticket.companyName}</p>
        <p className="text-center text-xs text-gray-500">{ticket.registerName}</p>
        <p className="text-center text-xs mb-3">Ticket {ticket.ticketNumber}</p>
        <hr className="my-2 border-dashed" />
        {ticket.lines?.map((l, i) => (
          <div key={i} className="flex justify-between gap-2 py-0.5">
            <span>{l.productNom} x{l.quantity}</span>
            <span>{Number(l.lineTotal).toFixed(2)}</span>
          </div>
        ))}
        <hr className="my-2 border-dashed" />
        <div className="flex justify-between font-bold">
          <span>TOTAL</span>
          <span>{Number(ticket.total).toFixed(2)} {ticket.currency}</span>
        </div>
        <div className="flex gap-2 mt-4">
          <button type="button" onClick={() => window.print()} className="flex-1 py-2 bg-gray-900 text-white rounded-lg text-xs">Imprimer</button>
          <button type="button" onClick={onClose} className="flex-1 py-2 bg-gray-200 rounded-lg text-xs">Fermer</button>
        </div>
      </div>
    </div>
  )
}

export default function PosPendingPaymentsPage() {
  const { user, hasPermission } = useAuth()
  const notify = useNotification()
  const [context, setContext] = useState(null)
  const [session, setSession] = useState(null)
  const [pending, setPending] = useState([])
  const [loading, setLoading] = useState(true)
  const [openingSession, setOpeningSession] = useState(false)
  const [showOpenModal, setShowOpenModal] = useState(false)
  const [showCloseModal, setShowCloseModal] = useState(false)
  const [selected, setSelected] = useState(null)
  const [detail, setDetail] = useState(null)
  const [ticket, setTicket] = useState(null)

  const currency = context?.publicSettings?.currency || 'EUR'
  const cashierOnly = isCashierOnlyUser(user)

  const refresh = useCallback(async () => {
    setLoading(true)
    try {
      const ctx = await posApi.context()
      setContext(ctx)
      setSession(ctx.session)
      if (hasPermission('pos.payment.collect') && ctx.session?.sessionType === 'CASHIER') {
        const list = await posApi.listPendingPayments()
        setPending(list)
      } else {
        setPending([])
      }
    } catch (e) {
      notify.error(getErrorMessage(e))
    } finally {
      setLoading(false)
    }
  }, [hasPermission, notify])

  const openCashierSession = async (openingCashAmount) => {
    setOpeningSession(true)
    try {
      const s = await posApi.openSession({ sessionType: 'CASHIER', openingCashAmount })
      setSession(s)
      setShowOpenModal(false)
      notify.success('Session caisse ouverte')
      await refresh()
    } catch (e) {
      notify.error(getErrorMessage(e))
    } finally {
      setOpeningSession(false)
    }
  }

  const onSessionClosed = async () => {
    setShowCloseModal(false)
    setSession(null)
    setPending([])
    await refresh()
  }
  useEffect(() => {
    refresh()
    const t = setInterval(refresh, 30000)
    return () => clearInterval(t)
  }, [refresh])

  const openDetail = async (sale) => {
    try {
      const full = await posApi.getSale(sale.id)
      setDetail(full)
    } catch (e) {
      notify.error(getErrorMessage(e))
    }
  }

  const cancelPending = async (saleId) => {
    if (!window.confirm('Annuler cette vente en attente ?')) return
    try {
      await posApi.cancelSale(saleId)
      notify.success('Vente annulée')
      setDetail(null)
      refresh()
    } catch (e) {
      notify.error(getErrorMessage(e))
    }
  }

  const onPaid = async (validatedSale) => {
    setSelected(null)
    try {
      const t = await posApi.ticket(validatedSale.id)
      setTicket(t)
    } catch { /* optional */ }
    notify.success(`Vente ${validatedSale.saleNumber} encaissée`)
    refresh()
  }

  if (!hasPermission('pos.payment.collect') && !hasPermission('pos.sale.read')) {
    return (
      <div className="flex-1 flex items-center justify-center text-slate-400">
        Permission requise (pos.payment.collect)
      </div>
    )
  }

  if (!session || session.sessionType !== 'CASHIER') {
    return (
      <div className="flex-1 flex flex-col items-center justify-center p-8 text-center">
        <h1 className="text-xl font-semibold mb-2">Ventes à encaisser</h1>
        <p className="text-slate-400 text-sm mb-6 max-w-md">
          Ouvrez votre session caisse pour voir les ventes en attente et encaisser les paiements.
        </p>
        <button type="button" disabled={openingSession} onClick={() => setShowOpenModal(true)}
          className="px-6 py-3 bg-emerald-600 hover:bg-emerald-500 rounded-xl font-medium disabled:opacity-50">
          Ouvrir session caisse
        </button>
        {!cashierOnly && (
          <Link to="/pos" className="mt-4 text-sm text-emerald-400 underline">← Préparation ventes</Link>
        )}
      </div>
    )
  }

  return (
    <div className="flex-1 flex flex-col min-h-0">
      <header className="px-4 py-3 bg-slate-900 border-b border-slate-800 flex items-center gap-4 text-sm">
        <div>
          <h1 className="font-semibold text-lg">Ventes à encaisser</h1>
          <p className="text-xs text-slate-500">
            Session {session.sessionNumber} · {session.warehouseCode} — ventes transférées par les vendeurs
          </p>
        </div>
        <div className="ml-auto flex items-center gap-3">
          {(hasPermission('pos.session.close') || hasPermission('pos.payment.collect')) && (
            <button type="button" onClick={() => setShowCloseModal(true)}
              className="text-xs px-3 py-1.5 bg-slate-800 rounded-lg hover:bg-slate-700">
              Fermer session
            </button>
          )}
          {!cashierOnly && (
            <Link to="/pos" className="text-xs text-emerald-400 hover:text-emerald-300 underline">
              ← Préparation ventes
            </Link>
          )}
        </div>
      </header>
      <main className="flex-1 overflow-auto p-4">
        {loading && <p className="text-slate-400 text-sm">Chargement…</p>}
        {!loading && pending.length === 0 && (
          <div className="text-center py-12 text-slate-400 text-sm max-w-md mx-auto space-y-2">
            <p>Aucune vente à encaisser pour le moment.</p>
            <p className="text-xs text-slate-500">
              Seules les ventes envoyées avec « Envoyer à la caisse » apparaissent ici.
              La « Pause client » (F8) côté vendeur reste sur son poste.
            </p>
          </div>
        )}
        {!loading && pending.length > 0 && (
          <div className="overflow-x-auto">
            <table className="w-full text-sm border-collapse">
              <thead>
                <tr className="text-left text-slate-400 border-b border-slate-700">
                  <th className="py-2 pr-4">N° vente</th>
                  <th className="py-2 pr-4">Vendeur</th>
                  <th className="py-2 pr-4">Client</th>
                  <th className="py-2 pr-4">Statut</th>
                  <th className="py-2 pr-4">Heure envoi</th>
                  <th className="py-2 pr-4">Montant</th>
                  <th className="py-2">Actions</th>
                </tr>
              </thead>
              <tbody>
                {pending.map((s) => (
                  <tr key={s.id} className="border-b border-slate-800 hover:bg-slate-900/50">
                    <td className="py-3 pr-4 font-medium">{s.saleNumber}</td>
                    <td className="py-3 pr-4">{s.sellerName || s.cashierName}</td>
                    <td className="py-3 pr-4">{s.customerName || '—'}</td>
                    <td className="py-3 pr-4">
                      <span className="inline-block px-2 py-0.5 rounded bg-amber-900/40 text-amber-300 text-xs">
                        {saleStatusLabel(s.status)}
                      </span>
                    </td>
                    <td className="py-3 pr-4 text-slate-400">
                      {s.sentToPaymentAt || s.submittedAt
                        ? new Date(s.sentToPaymentAt || s.submittedAt).toLocaleTimeString('fr-FR')
                        : '—'}
                    </td>
                    <td className="py-3 pr-4 text-emerald-400">{formatPosMoney(s.total, currency)}</td>
                    <td className="py-3">
                      <div className="flex flex-wrap gap-2">
                        {hasPermission('pos.payment.collect') && (
                          <button type="button" onClick={() => setSelected(s)}
                            className="px-3 py-1.5 bg-emerald-600 hover:bg-emerald-500 rounded-lg text-xs">
                            Encaisser
                          </button>
                        )}
                        {hasPermission('pos.sale.cancel') && (
                          <button type="button" onClick={() => cancelPending(s.id)}
                            className="px-3 py-1.5 bg-red-900/50 border border-red-800 rounded-lg text-xs">
                            Annuler
                          </button>
                        )}
                        <button type="button" onClick={() => openDetail(s)}
                          className="px-3 py-1.5 bg-slate-800 rounded-lg text-xs">
                          Détail
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </main>

      {detail && (
        <div className="fixed inset-0 bg-black/70 flex items-center justify-center z-40 p-4">
          <div className="bg-slate-900 border border-slate-700 rounded-xl w-full max-w-md p-6 max-h-[80vh] overflow-auto">
            <h3 className="font-semibold mb-2">{detail.saleNumber}</h3>
            <p className="text-xs text-slate-400 mb-4">Vendeur : {detail.sellerName || detail.cashierName}</p>
            <ul className="text-sm space-y-1 mb-4">
              {detail.lignes?.map((l) => (
                <li key={l.id} className="flex justify-between">
                  <span>{l.productNom} × {l.quantityInput}</span>
                  <span>{formatPosMoney(l.lineTotal, currency)}</span>
                </li>
              ))}
            </ul>
            <p className="font-bold text-emerald-400">Total {formatPosMoney(detail.total, currency)}</p>
            <button type="button" onClick={() => setDetail(null)} className="mt-4 px-4 py-2 bg-slate-700 rounded-lg text-sm">Fermer</button>
          </div>
        </div>
      )}

      {selected && (
        <PaymentModal sale={selected} currency={currency} onClose={() => setSelected(null)} onPaid={onPaid} />
      )}
      {ticket && <TicketModal ticket={ticket} onClose={() => setTicket(null)} />}
      {showOpenModal && (
        <CashSessionOpenModal
          currency={currency}
          loading={openingSession}
          onClose={() => setShowOpenModal(false)}
          onConfirm={openCashierSession}
        />
      )}
      {showCloseModal && session && (
        <CashSessionCloseModal
          session={session}
          currency={currency}
          onClose={() => setShowCloseModal(false)}
          onClosed={onSessionClosed}
        />
      )}
    </div>
  )
}
