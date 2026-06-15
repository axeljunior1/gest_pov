import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { posApi } from '../api'
import { useAuth } from '../context/AuthContext'
import { useNotification } from '../context/NotificationContext'
import { getErrorMessage } from '../utils/errors'
import { formatPosMoney } from '../utils/posMoney'
import { PosSessionChip } from '../components/pos/PosWorkspaceNav'
import { ReturnReceiptModal } from '../components/pos/PosPrintModals'

export default function PosReturnsPage() {
  const { hasPermission } = useAuth()
  const notify = useNotification()
  const [session, setSession] = useState(null)
  const [currency, setCurrency] = useState('EUR')
  const [search, setSearch] = useState('')
  const [results, setResults] = useState([])
  const [selected, setSelected] = useState(null)
  const [lines, setLines] = useState([])
  const [reason, setReason] = useState('')
  const [paymentMethod, setPaymentMethod] = useState('CASH')
  const [loading, setLoading] = useState(false)
  const [receipt, setReceipt] = useState(null)

  const canReturn = hasPermission('pos.return.create') || hasPermission('pos.sale.refund')

  useEffect(() => {
    posApi.context().then((ctx) => {
      setSession(ctx.session ?? null)
      setCurrency(ctx.publicSettings?.currency || 'EUR')
    }).catch(() => {})
  }, [])

  const searchSales = async () => {
    if (!search.trim()) return
    setLoading(true)
    try {
      setResults(await posApi.searchRefundableSales(search.trim()))
    } catch (e) {
      notify.error(getErrorMessage(e))
    } finally {
      setLoading(false)
    }
  }

  const loadSale = async (saleId) => {
    setLoading(true)
    try {
      const data = await posApi.returnableSale(saleId)
      setSelected(data)
      setLines(data.lines.map((l) => ({
        saleLineId: l.saleLineId,
        productNom: l.productNom,
        variantNameSnapshot: l.variantNameSnapshot,
        packagingName: l.packagingNameSnapshot,
        maxQty: Number(l.quantityReturnable),
        quantity: Number(l.quantityReturnable),
        unitPrice: l.unitPriceSnapshot,
        maxRefund: l.maxRefundAmount,
        restock: true,
      })))
    } catch (e) {
      notify.error(getErrorMessage(e))
    } finally {
      setLoading(false)
    }
  }

  const refundTotal = useMemo(() => lines.reduce((sum, l) => {
    if (!l.quantity || l.quantity <= 0) return sum
    const ratio = l.maxQty > 0 ? l.quantity / l.maxQty : 0
    return sum + Number(l.maxRefund) * ratio
  }, 0), [lines])

  const submitReturn = async (fullReturn) => {
    if (!selected) return
    setLoading(true)
    try {
      const requestLines = fullReturn
        ? undefined
        : lines.filter((l) => l.quantity > 0).map((l) => ({
            saleLineId: l.saleLineId,
            quantity: l.quantity,
            restock: l.restock,
          }))
      const draft = await posApi.createReturn(selected.id, {
        reason,
        returnToStock: true,
        lines: requestLines,
      })
      const validated = await posApi.validateReturn(draft.id, {
        payments: [{ method: paymentMethod, amount: draft.totalAmount ?? refundTotal }],
      })
      setReceipt(await posApi.returnReceipt(validated.id))
      notify.success(`Retour ${validated.refundNumber} validé`)
      setSelected(null)
      setResults([])
    } catch (e) {
      notify.error(getErrorMessage(e))
    } finally {
      setLoading(false)
    }
  }

  if (!canReturn) {
    return (
      <div className="flex-1 flex items-center justify-center text-slate-400">
        Permission requise (pos.return.create)
      </div>
    )
  }

  return (
    <div className="flex-1 flex flex-col min-h-0">
      <header className="px-4 py-3 bg-slate-900 border-b border-slate-800 flex flex-wrap items-center gap-4">
        <div>
          <h1 className="text-lg font-semibold">Retours / remboursements</h1>
          <p className="text-sm text-slate-400">Rechercher une vente payée et enregistrer un retour</p>
        </div>
        {session && <PosSessionChip session={session} centralMode />}
        <Link to="/pos/pending" className="ml-auto px-3 py-2 rounded-lg border border-slate-600 text-sm text-slate-300 hover:bg-slate-800">
          ← Retour caisse
        </Link>
      </header>

      <main className="flex-1 overflow-auto p-4 space-y-4 max-w-4xl">
        {!selected && (
          <>
            <div className="flex gap-2">
              <input
                type="search"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder="N° vente ou client…"
                className="flex-1 rounded-lg px-3 py-2 bg-slate-800 border border-slate-600 text-sm"
                onKeyDown={(e) => e.key === 'Enter' && searchSales()}
              />
              <button type="button" onClick={searchSales} disabled={loading}
                className="px-4 py-2 bg-indigo-600 hover:bg-indigo-500 rounded-lg text-sm disabled:opacity-50">
                Rechercher
              </button>
            </div>
            {results.length > 0 && (
              <ul className="divide-y divide-slate-800 border border-slate-700 rounded-xl overflow-hidden">
                {results.map((s) => (
                  <li key={s.id}>
                    <button type="button" onClick={() => loadSale(s.id)}
                      className="w-full text-left px-4 py-3 hover:bg-slate-800/60 flex justify-between gap-4">
                      <span className="font-mono text-xs">{s.saleNumber}</span>
                      <span className="text-slate-300">{s.customerName || 'Comptoir'}</span>
                      <span className="text-emerald-400">{formatPosMoney(s.total, currency)}</span>
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </>
        )}

        {selected && (
          <div className="space-y-4">
            <div className="flex justify-between items-start">
              <div>
                <p className="font-mono text-sm">{selected.saleNumber}</p>
                <p className="text-xs text-slate-400">Remboursable : {formatPosMoney(selected.amountRefundable, currency)}</p>
              </div>
              <button type="button" onClick={() => setSelected(null)} className="text-sm text-slate-400 hover:text-white">Annuler</button>
            </div>

            <label className="block text-sm">
              <span className="text-slate-400">Motif</span>
              <input value={reason} onChange={(e) => setReason(e.target.value)}
                className="mt-1 w-full rounded-lg px-3 py-2 bg-slate-800 border border-slate-600" />
            </label>

            <div className="space-y-2">
              {lines.map((l, idx) => (
                <div key={l.saleLineId} className="p-3 rounded-xl bg-slate-800/50 border border-slate-700 text-sm">
                  <p className="font-medium">
                    {l.productNom}
                    {l.variantNameSnapshot ? ` — ${l.variantNameSnapshot}` : ''}
                    {l.packagingName && ` (${l.packagingName})`}
                  </p>
                  <div className="flex flex-wrap gap-3 mt-2 items-center">
                    <label className="text-xs text-slate-400">
                      Qté max {l.maxQty}
                      <input type="number" min="0" max={l.maxQty} step="0.001" value={l.quantity}
                        onChange={(e) => {
                          const next = [...lines]
                          next[idx] = { ...l, quantity: Number(e.target.value) }
                          setLines(next)
                        }}
                        className="ml-2 w-20 rounded px-2 py-1 bg-slate-900 border border-slate-600" />
                    </label>
                    <label className="flex items-center gap-2 text-xs cursor-pointer">
                      <input type="checkbox" checked={l.restock}
                        onChange={(e) => {
                          const next = [...lines]
                          next[idx] = { ...l, restock: e.target.checked }
                          setLines(next)
                        }} />
                      Remettre en stock
                    </label>
                  </div>
                </div>
              ))}
            </div>

            <label className="block text-sm">
              <span className="text-slate-400">Méthode remboursement</span>
              <select value={paymentMethod} onChange={(e) => setPaymentMethod(e.target.value)}
                className="mt-1 rounded-lg px-3 py-2 bg-slate-800 border border-slate-600">
                <option value="CASH">Espèces</option>
                <option value="CARD">Carte</option>
                <option value="MOBILE_MONEY">Mobile money</option>
                <option value="BANK_TRANSFER">Virement</option>
              </select>
            </label>

            <p className="text-sm">Total remboursement : <span className="text-emerald-400 font-semibold">{formatPosMoney(refundTotal, currency)}</span></p>

            <div className="flex flex-wrap gap-2">
              <button type="button" disabled={loading} onClick={() => submitReturn(true)}
                className="px-4 py-2 bg-red-700 hover:bg-red-600 rounded-lg text-sm disabled:opacity-50">
                Retour total
              </button>
              <button type="button" disabled={loading} onClick={() => submitReturn(false)}
                className="px-4 py-2 bg-indigo-700 hover:bg-indigo-600 rounded-lg text-sm disabled:opacity-50">
                Valider retour partiel
              </button>
            </div>
          </div>
        )}
      </main>

      {receipt && <ReturnReceiptModal receipt={receipt} onClose={() => setReceipt(null)} />}
    </div>
  )
}
