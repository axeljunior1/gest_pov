import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { posApi } from '../api'
import { useAuth } from '../context/AuthContext'
import { useNotification } from '../context/NotificationContext'
import { getErrorMessage } from '../utils/errors'
import { isCashierOnlyUser } from '../utils/auth'
import { notifyPosSessionChanged } from '../utils/posSession'
import { notifyPosSaleStateChanged } from '../utils/posSaleEvents'
import { saleStatusLabel } from '../utils/saleStatus'
import { formatPosMoney } from '../utils/posMoney'
import CashSessionCloseModal from '../components/pos/CashSessionCloseModal'
import CashSessionOpenModal from '../components/pos/CashSessionOpenModal'
import CancelSaleModal from '../components/pos/CancelSaleModal'
import ModalOverlay from '../components/ui/ModalOverlay'
import { PosSessionChip, PosSessionTypeBadge, PosWrongSessionPanel } from '../components/pos/PosWorkspaceNav'
import { PosTicketModal } from '../components/pos/PosPrintModals'
import PosSaleLinesSummary from '../components/pos/PosSaleLinesSummary'
import PosSaleLineLabel from '../components/pos/PosSaleLineLabel'

function PaymentModal({ sale, currency, paymentMethods, changeGivingEnabled, onClose, onPaid, onRecallToDraft }) {
  const enabledMethods = useMemo(
    () => (paymentMethods || []).filter((m) => m.enabled),
    [paymentMethods],
  )
  const defaultMethod = enabledMethods[0]?.code || 'CASH'
  const [payments, setPayments] = useState([{ method: defaultMethod, amount: sale?.total || 0 }])
  const [cashReceived, setCashReceived] = useState('')
  const [loading, setLoading] = useState(false)
  const [recalling, setRecalling] = useState(false)
  const [paymentError, setPaymentError] = useState('')
  const notify = useNotification()

  useEffect(() => {
    setPayments([{ method: defaultMethod, amount: sale?.total || 0 }])
  }, [sale?.id, sale?.total, defaultMethod])

  const total = Number(sale?.total || 0)
  const paid = payments.reduce((s, p) => s + Number(p.amount || 0), 0)
  const change = changeGivingEnabled && payments.some((p) => p.method === 'CASH') && cashReceived
    ? Math.max(0, Number(cashReceived) - total)
    : changeGivingEnabled
      ? Math.max(0, paid - total)
      : 0

  const submit = async () => {
    if (loading || recalling) return
    setPaymentError('')
    setLoading(true)
    try {
      const result = await posApi.validateSale(sale.id, {
        payments: payments.map((p) => ({ method: p.method, amount: Number(p.amount) })),
        cashReceived: cashReceived ? Number(cashReceived) : undefined,
      })
      onPaid(result)
    } catch (e) {
      const message = getErrorMessage(e, { module: 'pos' })
      setPaymentError(message)
      notify.error(message)
    } finally {
      setLoading(false)
    }
  }

  const recallToDraft = async () => {
    if (!window.confirm('Renvoyer cette vente en attente sur le poste vendeur ?')) return
    setRecalling(true)
    try {
      await onRecallToDraft(sale.id)
    } catch (e) {
      notify.error(getErrorMessage(e, { module: 'pos' }))
    } finally {
      setRecalling(false)
    }
  }

  return (
    <ModalOverlay open onClose={onClose}>
      <div className="bg-slate-900 border border-slate-700 rounded-xl w-full max-w-lg p-6">
        <h3 className="text-lg font-semibold mb-1">Encaisser — {sale?.saleNumber}</h3>
        <p className="text-sm text-slate-400 mb-4">Vendeur : {sale?.sellerName || sale?.cashierName} · Total {formatPosMoney(total, currency)}</p>
        <PosSaleLinesSummary lines={sale?.lignes} currency={currency} />
        {paymentError && (
          <div className="mb-4 rounded-lg border border-red-500/50 bg-red-950/40 px-3 py-2 text-sm text-red-200" role="alert">
            {paymentError}
          </div>
        )}
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
              {enabledMethods.map((m) => (
                <option key={m.code} value={m.code}>{m.label}</option>
              ))}
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
        {changeGivingEnabled && (
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
        )}
        <div className="flex flex-wrap gap-2 justify-between">
          <div className="flex gap-2">
            <button type="button" onClick={onClose} className="px-4 py-2 rounded-lg bg-slate-700 text-sm">Fermer</button>
            {onRecallToDraft && (
              <button type="button" disabled={recalling || loading} onClick={recallToDraft}
                className="px-4 py-2 rounded-lg bg-amber-700 hover:bg-amber-600 text-sm disabled:opacity-50">
                Retour en attente vendeur
              </button>
            )}
          </div>
          <button type="button" disabled={loading || recalling || paid < total} onClick={submit}
            className="px-4 py-2 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-sm font-medium disabled:opacity-50">
            Valider paiement
          </button>
        </div>
      </div>
    </ModalOverlay>
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
  const [autoPrintTicket, setAutoPrintTicket] = useState(false)
  const [cancelTarget, setCancelTarget] = useState(null)
  const [cancelling, setCancelling] = useState(false)

  const currency = context?.publicSettings?.currency || 'EUR'
  const clientConfig = context?.clientConfig
  const paymentMethods = clientConfig?.pos?.paymentMethods
  const changeGivingEnabled = clientConfig?.pos?.changeGivingEnabled !== false
  const autoPrintAfterSale = !!clientConfig?.pos?.autoPrintAfterSale
  const isCentralCashier = context?.posConfig?.salesFlowMode === 'CENTRAL_CASHIER'
    || context?.posConfig?.cashHandlingMode === 'CENTRAL_CASHIER'
  const cashierOnly = isCashierOnlyUser(user)
  const canReprint = hasPermission('pos.ticket.print') || hasPermission('pos.ticket.reprint')

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
      notify.error(getErrorMessage(e, { module: 'pos' }))
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
      notifyPosSessionChanged()
      await refresh()
    } catch (e) {
      notify.error(getErrorMessage(e, { module: 'pos' }))
    } finally {
      setOpeningSession(false)
    }
  }

  const onSessionClosed = async () => {
    setShowCloseModal(false)
    setSession(null)
    setPending([])
    notifyPosSessionChanged()
    await refresh()
  }
  useEffect(() => {
    refresh()
    const t = setInterval(refresh, 30000)
    return () => clearInterval(t)
  }, [refresh])

  useEffect(() => {
    const onSaleChange = () => {
      refresh().catch(() => {})
    }
    window.addEventListener('pos-sale-state-changed', onSaleChange)
    return () => window.removeEventListener('pos-sale-state-changed', onSaleChange)
  }, [refresh])

  const openDetail = async (sale) => {
    try {
      const full = await posApi.getSale(sale.id)
      setDetail(full)
    } catch (e) {
      notify.error(getErrorMessage(e, { module: 'pos' }))
    }
  }

  const cancelPending = async (payload) => {
    if (!cancelTarget) return
    setCancelling(true)
    try {
      await posApi.cancelSale(cancelTarget, payload)
      notify.success('Vente annulée')
      setCancelTarget(null)
      setDetail(null)
      await refresh()
      notifyPosSaleStateChanged()
    } catch (e) {
      notify.error(getErrorMessage(e, { module: 'pos' }))
    } finally {
      setCancelling(false)
    }
  }

  const recallToDraft = async (saleId) => {
    try {
      await posApi.recallFromPayment(saleId)
      notify.success('Vente renvoyée en attente — le vendeur la retrouve sur son poste (F9)')
      setSelected(null)
      setDetail(null)
      await refresh()
      notifyPosSaleStateChanged()
    } catch (e) {
      notify.error(getErrorMessage(e, { module: 'pos' }))
      throw e
    }
  }

  const onPaid = async (validatedSale) => {
    setSelected(null)
    try {
      const t = await posApi.ticket(validatedSale.id)
      setAutoPrintTicket(autoPrintAfterSale)
      setTicket(t)
    } catch (e) {
      notify.error(getErrorMessage(e, {
        module: 'pos',
        fallback: 'Le ticket n\'a pas pu être affiché, mais la vente est bien enregistrée.',
      }))
    }
    notify.success(`Vente ${validatedSale.saleNumber} encaissée`)
    await refresh()
    notifyPosSaleStateChanged()
  }

  const closeSalesSession = async () => {
    if (!window.confirm('Fermer la session vente pour ouvrir une session caisse ici ?')) return
    try {
      const report = await posApi.closeSession({ closingCashAmount: 0, cancelPendingDrafts: true })
      notify.success(`Session vente fermée — ${report.saleCount ?? 0} vente(s)`)
      notifyPosSessionChanged()
      await refresh()
    } catch (e) {
      notify.error(getErrorMessage(e, { module: 'pos' }))
    }
  }

  if (!hasPermission('pos.payment.collect') && !hasPermission('pos.sale.read')) {
    return (
      <div className="flex-1 flex items-center justify-center text-slate-400">
        Permission requise (pos.payment.collect)
      </div>
    )
  }

  if (!session || session.sessionType !== 'CASHIER') {
    const hasWrongSession = session?.sessionType === 'SALES'
    if (hasWrongSession) {
      return (
        <PosWrongSessionPanel
          session={session}
          expectedType="CASHIER"
          centralMode={isCentralCashier}
          onCloseSession={closeSalesSession}
          canCloseSession={hasPermission('pos.session.close') || hasPermission('pos.sale.create')}
        />
      )
    }
    return (
      <>
        <div className="flex-1 flex flex-col items-center justify-center p-8 text-center max-w-lg mx-auto">
          <PosSessionTypeBadge type="CASHIER" size="lg" />
          <h1 className="text-2xl font-semibold mt-6 mb-2">Poste encaissement</h1>
          <p className="text-slate-400 mb-6">
            Ouvrez votre session caisse pour voir les ventes en attente et encaisser les paiements.
          </p>
          <button type="button" disabled={openingSession} onClick={() => setShowOpenModal(true)}
            className="px-8 py-4 bg-emerald-600 hover:bg-emerald-500 rounded-xl text-lg font-medium disabled:opacity-50">
            Ouvrir session caisse
          </button>
          {cashierOnly && (
            <p className="mt-6 text-sm text-slate-500">
              Votre compte est limité à l’encaissement — la préparation des ventes se fait sur un autre poste.
            </p>
          )}
        </div>
        {showOpenModal && (
          <CashSessionOpenModal
            currency={currency}
            loading={openingSession}
            onClose={() => setShowOpenModal(false)}
            onConfirm={openCashierSession}
          />
        )}
      </>
    )
  }

  return (
    <div className="flex-1 flex flex-col min-h-0">
      <header className="px-4 py-3 bg-slate-900 border-b border-slate-800 flex flex-wrap items-center gap-4 text-sm">
        <div>
          <h1 className="font-semibold text-lg">Encaissement</h1>
          <p className="text-sm text-slate-400 mt-0.5">
            Ventes transférées par les vendeurs
            {!loading && pending.length > 0 && (
              <span className="ml-2 px-2 py-0.5 rounded-full bg-amber-500/20 text-amber-200 text-xs font-medium">
                {pending.length} en attente
              </span>
            )}
          </p>
        </div>
        <PosSessionChip session={session} expectedType="CASHIER" centralMode />
        <div className="ml-auto flex items-center gap-3">
          {canReprint && (
            <Link
              to="/pos/history"
              className="px-4 py-2 bg-slate-800 border border-slate-600 rounded-lg text-sm hover:bg-slate-700"
            >
              Ventes passées
            </Link>
          )}
          {(hasPermission('pos.session.close') || hasPermission('pos.payment.collect')) && (
            <button type="button" onClick={() => setShowCloseModal(true)}
              className="px-4 py-2 bg-slate-800 rounded-lg hover:bg-slate-700 text-sm font-medium">
              Fermer session caisse
            </button>
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
                        {(hasPermission('pos.payment.collect')
                          || hasPermission('pos.sale.send_to_payment')
                          || hasPermission('pos.sale.create')) && (
                          <button type="button" onClick={() => recallToDraft(s.id)}
                            className="px-3 py-1.5 bg-amber-800/60 border border-amber-700 rounded-lg text-xs">
                            Retour en attente
                          </button>
                        )}
                        {hasPermission('pos.sale.cancel') && (
                          <button type="button" onClick={() => setCancelTarget(s.id)}
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
        <ModalOverlay open onClose={() => setDetail(null)}>
          <div className="bg-slate-900 border border-slate-700 rounded-xl w-full max-w-md p-6 max-h-[80vh] overflow-auto">
            <h3 className="font-semibold mb-2">{detail.saleNumber}</h3>
            <p className="text-xs text-slate-400 mb-4">Vendeur : {detail.sellerName || detail.cashierName}</p>
            <ul className="text-sm space-y-1 mb-4">
              {detail.lignes?.map((l) => (
                <li key={l.id} className="flex justify-between gap-3 items-start">
                  <div className="min-w-0">
                    <PosSaleLineLabel line={l} variant="cart" />
                    <span className="text-xs text-slate-500">× {l.quantityInput}</span>
                  </div>
                  <span className="shrink-0">{formatPosMoney(l.lineTotal, currency)}</span>
                </li>
              ))}
            </ul>
            <p className="font-bold text-emerald-400">Total {formatPosMoney(detail.total, currency)}</p>
            <button type="button" onClick={() => setDetail(null)} className="mt-4 px-4 py-2 bg-slate-700 rounded-lg text-sm">Fermer</button>
          </div>
        </ModalOverlay>
      )}

      {selected && (
        <PaymentModal
          sale={selected}
          currency={currency}
          paymentMethods={paymentMethods}
          changeGivingEnabled={changeGivingEnabled}
          onClose={() => setSelected(null)}
          onPaid={onPaid}
          onRecallToDraft={recallToDraft}
        />
      )}
      {ticket && (
        <PosTicketModal
          ticket={ticket}
          autoPrint={autoPrintTicket}
          onClose={() => { setTicket(null); setAutoPrintTicket(false) }}
        />
      )}
      {showOpenModal && (
        <CashSessionOpenModal
          currency={currency}
          loading={openingSession}
          onClose={() => setShowOpenModal(false)}
          onConfirm={openCashierSession}
        />
      )}
      {cancelTarget && (
        <CancelSaleModal
          saleNumber={pending.find((s) => s.id === cancelTarget)?.saleNumber}
          loading={cancelling}
          onClose={() => !cancelling && setCancelTarget(null)}
          onConfirm={cancelPending}
        />
      )}
      {showCloseModal && session && (
        <CashSessionCloseModal
          session={session}
          currency={currency}
          companyName={context?.publicSettings?.companyName}
          onClose={() => setShowCloseModal(false)}
          onClosed={onSessionClosed}
        />
      )}
    </div>
  )
}
