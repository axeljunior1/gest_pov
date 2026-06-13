import { useCallback, useEffect, useRef, useState } from 'react'
import { posApi } from '../api'
import { useAuth } from '../context/AuthContext'
import { useNotification } from '../context/NotificationContext'
import { getErrorMessage } from '../utils/errors'

function formatMoney(value, currency = 'EUR') {
  if (value == null) return '—'
  try {
    return new Intl.NumberFormat('fr-FR', { style: 'currency', currency }).format(Number(value))
  } catch {
    return `${Number(value).toFixed(2)} ${currency}`
  }
}

function flatCategories(nodes, depth = 0) {
  if (!nodes) return []
  return nodes.flatMap((n) => [
    { id: n.id, nom: n.nom, depth },
    ...flatCategories(n.enfants || n.children, depth + 1),
  ])
}

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
        <h3 className="text-lg font-semibold mb-4">Paiement — {formatMoney(total, currency)}</h3>
        {payments.map((p, i) => (
          <div key={i} className="flex gap-2 mb-2">
            <select
              className="bg-slate-800 border border-slate-600 rounded-lg px-3 py-2 text-sm flex-1"
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
              className="bg-slate-800 border border-slate-600 rounded-lg px-3 py-2 text-sm w-32"
              value={p.amount}
              onChange={(e) => {
                const next = [...payments]
                next[i].amount = e.target.value
                setPayments(next)
              }}
            />
          </div>
        ))}
        <button
          type="button"
          className="text-xs text-slate-400 mb-4 hover:text-white"
          onClick={() => setPayments([...payments, { method: 'CARD', amount: 0 }])}
        >
          + Mode de paiement
        </button>
        <div className="mb-4">
          <label className="text-xs text-slate-400 block mb-1">Montant reçu (espèces)</label>
          <input
            type="number"
            className="w-full bg-slate-800 border border-slate-600 rounded-lg px-3 py-2"
            value={cashReceived}
            onChange={(e) => setCashReceived(e.target.value)}
            placeholder="Optionnel"
          />
          {change > 0 && (
            <p className="text-emerald-400 text-sm mt-2">Monnaie à rendre : {formatMoney(change, currency)}</p>
          )}
        </div>
        <div className="flex gap-2 justify-end">
          <button type="button" onClick={onClose} className="px-4 py-2 rounded-lg bg-slate-700 text-sm">Annuler (Esc)</button>
          <button type="button" disabled={loading || paid < total} onClick={submit}
            className="px-4 py-2 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-sm font-medium disabled:opacity-50">
            Encaisser
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
      <div className="bg-white text-gray-900 rounded-xl w-full max-w-sm p-6 font-mono text-sm">
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
        {ticket.changeAmount > 0 && (
          <p className="text-xs mt-1">Monnaie : {Number(ticket.changeAmount).toFixed(2)}</p>
        )}
        <div className="flex gap-2 mt-4">
          <button type="button" onClick={() => window.print()} className="flex-1 py-2 bg-gray-900 text-white rounded-lg text-xs">Imprimer</button>
          <button type="button" onClick={onClose} className="flex-1 py-2 bg-gray-200 rounded-lg text-xs">Fermer</button>
        </div>
      </div>
    </div>
  )
}

export default function POSPage() {
  const { user, hasPermission } = useAuth()
  const notify = useNotification()
  const searchRef = useRef(null)
  const scanBufferRef = useRef('')
  const scanTimerRef = useRef(null)

  const [context, setContext] = useState(null)
  const [session, setSession] = useState(null)
  const [catalog, setCatalog] = useState(null)
  const [products, setProducts] = useState([])
  const [categoryId, setCategoryId] = useState(null)
  const [search, setSearch] = useState('')
  const [searchResults, setSearchResults] = useState(null)
  const [sale, setSale] = useState(null)
  const [holdSales, setHoldSales] = useState([])
  const [showPayment, setShowPayment] = useState(false)
  const [ticket, setTicket] = useState(null)
  const [clock, setClock] = useState(new Date())
  const [online] = useState(true)
  const [openingSession, setOpeningSession] = useState(false)

  const currency = context?.publicSettings?.currency || 'EUR'
  const warehouseId = session?.warehouseId

  const refreshContext = useCallback(async () => {
    const ctx = await posApi.context()
    setContext(ctx)
    setSession(ctx.session)
    return ctx
  }, [])

  const loadCatalog = useCallback(async (whId, catId) => {
    const data = await posApi.catalog({ warehouseId: whId, categoryId: catId || undefined })
    setCatalog(data)
    setProducts(data.products || [])
  }, [])

  const ensureSale = useCallback(async () => {
    if (sale?.status === 'DRAFT') return sale
    const s = await posApi.createSale()
    setSale(s)
    return s
  }, [sale])

  const addProduct = useCallback(async (product) => {
    try {
      const s = await ensureSale()
      const updated = await posApi.addLine(s.id, {
        productId: product.id,
        quantityInput: 1,
        unitPrice: product.unitPrice,
      })
      setSale(updated)
      notify.success(`${product.nom} ajouté`)
    } catch (e) {
      notify.error(getErrorMessage(e))
    }
  }, [ensureSale, notify])

  const runSearch = useCallback(async (q) => {
    if (!q?.trim()) {
      setSearchResults(null)
      return
    }
    const results = await posApi.search(q.trim(), { warehouseId })
    setSearchResults(results)
    if (results.length === 1) {
      await addProduct(results[0])
      setSearch('')
      setSearchResults(null)
    }
  }, [warehouseId, addProduct])

  useEffect(() => {
    refreshContext().catch((e) => notify.error(getErrorMessage(e)))
    const t = setInterval(() => setClock(new Date()), 1000)
    return () => clearInterval(t)
  }, [refreshContext, notify])

  useEffect(() => {
    if (session?.warehouseId) {
      loadCatalog(session.warehouseId, categoryId).catch(() => {})
    }
  }, [session, categoryId, loadCatalog])

  useEffect(() => {
    searchRef.current?.focus()
  }, [session])

  useEffect(() => {
    const onKey = (e) => {
      if (e.key === 'F2') { e.preventDefault(); searchRef.current?.focus() }
      if (e.key === 'F4') { e.preventDefault(); if (sale?.lignes?.length) setShowPayment(true) }
      if (e.key === 'F8' && sale?.id) {
        e.preventDefault()
        posApi.holdSale(sale.id).then(() => {
          notify.success('Vente mise en attente')
          setSale(null)
          posApi.listHold().then(setHoldSales)
        }).catch((err) => notify.error(getErrorMessage(err)))
      }
      if (e.key === 'F9') {
        e.preventDefault()
        posApi.listHold().then(setHoldSales)
      }
      if (e.key === 'Escape') { setShowPayment(false); setTicket(null) }
      if (e.ctrlKey && e.key === 'n') { e.preventDefault(); setSale(null); posApi.createSale().then(setSale) }
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [sale, notify])

  const onScanInput = (e) => {
    const v = e.target.value
    setSearch(v)
    clearTimeout(scanTimerRef.current)
    scanTimerRef.current = setTimeout(() => runSearch(v), 200)
  }

  const openSession = async () => {
    setOpeningSession(true)
    try {
      const s = await posApi.openSession({ openingCashAmount: 0 })
      setSession(s)
      notify.success('Session ouverte')
      await loadCatalog(s.warehouseId, null)
    } catch (e) {
      notify.error(getErrorMessage(e))
    } finally {
      setOpeningSession(false)
    }
  }

  const closeSession = async () => {
    if (!window.confirm('Confirmer la clôture de session ?')) return
    try {
      const report = await posApi.closeSession({ closingCashAmount: 0 })
      notify.success(`Session fermée — ${report.saleCount} vente(s), CA ${formatMoney(report.totalRevenue, currency)}`)
      setSession(null)
      setSale(null)
    } catch (e) {
      notify.error(getErrorMessage(e))
    }
  }

  const onPaid = async (validatedSale) => {
    setShowPayment(false)
    setSale(null)
    try {
      const t = await posApi.ticket(validatedSale.id)
      setTicket(t)
    } catch { /* ticket optional */ }
    notify.success(`Vente ${validatedSale.saleNumber} validée`)
  }

  if (!hasPermission('pos.sale.read')) {
    return (
      <div className="flex-1 flex items-center justify-center text-slate-400">
        Permission POS requise (pos.sale.read)
      </div>
    )
  }

  if (!session) {
    return (
      <div className="flex-1 flex flex-col items-center justify-center gap-6 p-8">
        <h2 className="text-2xl font-semibold">Ouverture de caisse</h2>
        <p className="text-slate-400 text-center max-w-md">
          Aucune session active. Ouvrez une session pour commencer à vendre.
        </p>
        {hasPermission('pos.session.open') && (
          <button type="button" disabled={openingSession} onClick={openSession}
            className="px-8 py-4 bg-emerald-600 hover:bg-emerald-500 rounded-xl text-lg font-medium disabled:opacity-50">
            Ouvrir la session
          </button>
        )}
      </div>
    )
  }

  const categories = flatCategories(catalog?.categories)
  const displayProducts = searchResults ?? products
  const lines = sale?.lignes || []

  return (
    <div className="flex-1 flex flex-col min-h-0">
      {/* Header */}
      <header className="px-4 py-3 bg-slate-900 border-b border-slate-800 flex flex-wrap items-center gap-4 text-sm">
        <div>
          <p className="font-semibold">{context?.publicSettings?.companyName}</p>
          <p className="text-slate-400 text-xs">{context?.registerName || 'Caisse 1'}</p>
        </div>
        <div className="text-slate-300">
          <p>Session {session.sessionNumber}</p>
          <p className="text-xs text-slate-500">{session.warehouseCode}</p>
        </div>
        <div>
          <p>{user?.firstName} {user?.lastName}</p>
          <p className="text-xs text-slate-500">{clock.toLocaleString('fr-FR')}</p>
        </div>
        <div className={`ml-auto flex items-center gap-1 text-xs ${online ? 'text-emerald-400' : 'text-red-400'}`}>
          <span className={`w-2 h-2 rounded-full ${online ? 'bg-emerald-400' : 'bg-red-400'}`} />
          {online ? 'En ligne' : 'Hors ligne'}
        </div>
        {hasPermission('pos.session.close') && (
          <button type="button" onClick={closeSession} className="px-3 py-1.5 bg-red-900/50 border border-red-800 rounded-lg text-xs hover:bg-red-900">
            Fermer session
          </button>
        )}
      </header>

      <div className="flex-1 flex min-h-0">
        {/* Colonne gauche — catégories */}
        <aside className="w-48 shrink-0 bg-slate-900 border-r border-slate-800 overflow-y-auto p-2 text-sm">
          <button type="button" onClick={() => setCategoryId(null)}
            className={`w-full text-left px-3 py-2 rounded-lg mb-1 ${!categoryId ? 'bg-emerald-600' : 'hover:bg-slate-800'}`}>
            Tous
          </button>
          {categories.map((c) => (
            <button key={c.id} type="button" onClick={() => setCategoryId(c.id)}
              className={`w-full text-left px-3 py-2 rounded-lg mb-0.5 truncate ${categoryId === c.id ? 'bg-emerald-600' : 'hover:bg-slate-800'}`}
              style={{ paddingLeft: `${12 + c.depth * 12}px` }}>
              {c.nom}
            </button>
          ))}
          {catalog?.promotions?.length > 0 && (
            <>
              <p className="text-xs text-slate-500 mt-4 mb-2 px-2">Promotions</p>
              {catalog.promotions.slice(0, 5).map((p) => (
                <button key={p.id} type="button" onClick={() => addProduct(p)}
                  className="w-full text-left px-3 py-1.5 text-xs text-amber-400 hover:bg-slate-800 rounded truncate">
                  {p.nom}
                </button>
              ))}
            </>
          )}
        </aside>

        {/* Zone centrale — produits + recherche */}
        <main className="flex-1 flex flex-col min-w-0 p-4">
          <div className="mb-4">
            <input
              ref={searchRef}
              type="text"
              value={search}
              onChange={onScanInput}
              placeholder="Scanner ou rechercher (F2) — nom, SKU, code-barres"
              className="w-full bg-slate-800 border border-slate-600 rounded-xl px-4 py-3 text-base focus:outline-none focus:ring-2 focus:ring-emerald-500"
              autoComplete="off"
            />
            {searchResults && searchResults.length === 0 && (
              <p className="text-red-400 text-sm mt-2">Aucun produit trouvé</p>
            )}
            {searchResults && searchResults.length > 1 && (
              <ul className="mt-2 bg-slate-800 rounded-xl border border-slate-700 max-h-40 overflow-auto">
                {searchResults.map((p) => (
                  <li key={p.id}>
                    <button type="button" onClick={() => { addProduct(p); setSearch(''); setSearchResults(null) }}
                      className="w-full text-left px-4 py-2 hover:bg-slate-700 text-sm">
                      {p.nom} — {formatMoney(p.unitPrice, currency)}
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </div>
          <div className="flex-1 overflow-y-auto grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-3 content-start">
            {displayProducts.map((p) => (
              <button
                key={p.id}
                type="button"
                onClick={() => addProduct(p)}
                className="bg-slate-800 border border-slate-700 rounded-xl p-3 text-left hover:border-emerald-500 hover:bg-slate-750 transition-colors min-h-[120px] flex flex-col"
              >
                <p className="font-medium text-sm line-clamp-2 flex-1">{p.nom}</p>
                <p className="text-emerald-400 font-semibold mt-2">{formatMoney(p.unitPrice, currency)}</p>
                <div className="flex gap-1 mt-1 flex-wrap">
                  {p.promotional && <span className="text-[10px] bg-amber-900 text-amber-300 px-1.5 py-0.5 rounded">PROMO</span>}
                  {p.outOfStock && <span className="text-[10px] bg-red-900 text-red-300 px-1.5 py-0.5 rounded">RUPTURE</span>}
                  {p.lowStock && !p.outOfStock && <span className="text-[10px] bg-orange-900 text-orange-300 px-1.5 py-0.5 rounded">STOCK FAIBLE</span>}
                </div>
                <p className="text-[10px] text-slate-500 mt-1">Stock : {Number(p.stockAvailable).toFixed(0)}</p>
              </button>
            ))}
          </div>
        </main>

        {/* Colonne droite — panier */}
        <aside className="w-80 shrink-0 bg-slate-900 border-l border-slate-800 flex flex-col">
          <div className="p-4 border-b border-slate-800">
            <h2 className="font-semibold">Panier</h2>
            {sale?.saleNumber && <p className="text-xs text-slate-500">{sale.saleNumber}</p>}
          </div>
          <ul className="flex-1 overflow-y-auto divide-y divide-slate-800">
            {lines.length === 0 && (
              <li className="p-4 text-slate-500 text-sm text-center">Panier vide</li>
            )}
            {lines.map((l) => (
              <li key={l.id} className="p-3 text-sm">
                <div className="flex justify-between gap-2">
                  <span className="font-medium line-clamp-1">{l.productNom}</span>
                  <span>{formatMoney(l.lineTotal, currency)}</span>
                </div>
                <div className="flex items-center gap-2 mt-2 text-slate-400">
                  <button type="button" className="w-7 h-7 bg-slate-800 rounded"
                    onClick={() => posApi.updateLine(sale.id, l.id, Number(l.quantityInput) - 1).then(setSale)}>-</button>
                  <span>{l.quantityInput}</span>
                  <button type="button" className="w-7 h-7 bg-slate-800 rounded"
                    onClick={() => posApi.updateLine(sale.id, l.id, Number(l.quantityInput) + 1).then(setSale)}>+</button>
                  <span className="text-xs ml-auto">{formatMoney(l.unitPrice, currency)}/u</span>
                  <button type="button" className="text-red-400 text-xs"
                    onClick={() => posApi.updateLine(sale.id, l.id, 0).then(setSale)}>✕</button>
                </div>
              </li>
            ))}
          </ul>
          <div className="p-4 border-t border-slate-800 space-y-1 text-sm">
            <div className="flex justify-between text-slate-400"><span>Sous-total</span><span>{formatMoney(sale?.subtotal, currency)}</span></div>
            <div className="flex justify-between text-slate-400"><span>Remise</span><span>{formatMoney(sale?.discountTotal, currency)}</span></div>
            <div className="flex justify-between text-slate-400"><span>Taxes</span><span>{formatMoney(sale?.taxTotal, currency)}</span></div>
            <div className="flex justify-between text-lg font-bold pt-2"><span>Total</span><span className="text-emerald-400">{formatMoney(sale?.total, currency)}</span></div>
            <button
              type="button"
              disabled={!lines.length || !hasPermission('pos.sale.validate')}
              onClick={() => setShowPayment(true)}
              className="w-full mt-3 py-3 bg-emerald-600 hover:bg-emerald-500 rounded-xl font-semibold disabled:opacity-40"
            >
              Paiement (F4)
            </button>
            <div className="flex gap-2 mt-2">
              <button type="button" disabled={!sale?.id} onClick={() => posApi.holdSale(sale.id).then(() => { notify.success('En attente'); setSale(null) })}
                className="flex-1 py-2 bg-slate-800 rounded-lg text-xs disabled:opacity-40">Attente (F8)</button>
              <button type="button" onClick={() => posApi.listHold().then(setHoldSales)}
                className="flex-1 py-2 bg-slate-800 rounded-lg text-xs">Reprendre (F9)</button>
            </div>
          </div>
        </aside>
      </div>

      {holdSales.length > 0 && (
        <div className="fixed bottom-4 left-4 bg-slate-800 border border-slate-600 rounded-xl p-3 max-w-xs z-40">
          <p className="text-xs text-slate-400 mb-2">Ventes en attente</p>
          {holdSales.map((h) => (
            <button key={h.id} type="button"
              onClick={() => posApi.resumeSale(h.id).then(setSale).then(() => setHoldSales([]))}
              className="block w-full text-left text-sm py-1 hover:text-emerald-400">
              {h.holdLabel || h.saleNumber} — {formatMoney(h.total, currency)}
            </button>
          ))}
        </div>
      )}

      {showPayment && sale && (
        <PaymentModal sale={sale} currency={currency} onClose={() => setShowPayment(false)} onPaid={onPaid} />
      )}
      {ticket && <TicketModal ticket={ticket} onClose={() => setTicket(null)} />}
    </div>
  )
}
