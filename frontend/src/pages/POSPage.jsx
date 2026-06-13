import { useCallback, useEffect, useRef, useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import POSCustomerPanel from '../components/POSCustomerPanel'
import { posApi } from '../api'
import { useAuth } from '../context/AuthContext'
import { useNotification } from '../context/NotificationContext'
import { getErrorMessage } from '../utils/errors'
import { isCashierOnlyUser } from '../utils/auth'
import { parseQty, parseSearchWithQty } from '../utils/posQty'
import CashSessionCloseModal from '../components/pos/CashSessionCloseModal'
import CashSessionOpenModal from '../components/pos/CashSessionOpenModal'
import { notifyPosSessionChanged } from '../utils/posSession'
import { PosSessionChip, PosSessionTypeBadge } from '../components/pos/PosWorkspaceNav'
import { PosTicketModal } from '../components/pos/PosPrintModals'

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

function PosSearchResults({ results, matchType, highlightIndex, currency, onPick }) {
  if (results === null) return null

  if (results.length === 0) {
    return <p className="text-red-400 text-sm mt-2 px-1">Aucun produit trouvé</p>
  }

  const hint = matchType === 'TEXT'
    ? `${results.length} résultat(s) — cliquez ou Entrée pour ajouter`
    : null

  return (
    <div className="mt-2 bg-slate-800 rounded-xl border border-slate-700 shadow-lg overflow-hidden">
      {hint && (
        <p className="text-xs text-slate-400 px-4 py-2 border-b border-slate-700">{hint}</p>
      )}
      <ul className="max-h-56 overflow-auto">
        {results.map((p, index) => (
          <li key={p.id}>
            <button
              type="button"
              onClick={() => onPick(p)}
              className={`w-full text-left px-4 py-3 text-sm flex items-center justify-between gap-3 ${
                index === highlightIndex ? 'bg-emerald-600/30 ring-1 ring-inset ring-emerald-500' : 'hover:bg-slate-700'
              }`}
            >
              <span className="min-w-0">
                <span className="font-medium block truncate">{p.nom}</span>
                <span className="text-xs text-slate-400">{p.sku}{p.categoryNom ? ` · ${p.categoryNom}` : ''}</span>
              </span>
              <span className="shrink-0 text-right">
                <span className="text-emerald-400 font-medium">{formatMoney(p.unitPrice, currency)}</span>
                {p.outOfStock
                  ? <span className="block text-[10px] text-red-400">Rupture</span>
                  : <span className="block text-[10px] text-slate-500">Stock {Number(p.stockAvailable).toFixed(0)}</span>}
              </span>
            </button>
          </li>
        ))}
      </ul>
    </div>
  )
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
              className="rounded-lg px-3 py-2 text-sm flex-1 border border-slate-600"
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
              className="rounded-lg px-3 py-2 text-sm w-32 border border-slate-600"
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
            className="w-full rounded-lg px-3 py-2 border border-slate-600"
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

function PackagingPickerModal({ product, qty, currency, onClose, onPick }) {
  const packagings = (product?.packagings || []).filter((p) => p.active !== false)
  const defaultId = packagings.find((p) => p.defaultSale)?.id ?? packagings[0]?.id

  return (
    <div className="fixed inset-0 bg-black/70 flex items-center justify-center z-50 p-4">
      <div className="bg-slate-900 border border-slate-700 rounded-xl w-full max-w-md p-6">
        <h3 className="text-lg font-semibold mb-1">{product?.nom}</h3>
        <p className="text-sm text-slate-400 mb-4">Choisissez le conditionnement · quantité ×{qty}</p>
        <div className="space-y-2">
          {packagings.map((p) => (
            <button
              key={p.id}
              type="button"
              onClick={() => onPick(p.id)}
              className={`w-full text-left px-4 py-3 rounded-xl border transition-colors ${
                p.id === defaultId
                  ? 'border-emerald-500 bg-emerald-600/20 hover:bg-emerald-600/30'
                  : 'border-slate-700 hover:border-slate-500 hover:bg-slate-800'
              }`}
            >
              <span className="font-medium">{p.nom}</span>
              {p.quantiteBase > 1 && (
                <span className="text-xs text-slate-400 ml-2">({p.quantiteBase} u.)</span>
              )}
              <span className="float-right text-emerald-400 font-semibold">{formatMoney(p.salePrice, currency)}</span>
            </button>
          ))}
        </div>
        <button type="button" onClick={onClose} className="mt-4 w-full py-2 rounded-lg bg-slate-700 text-sm hover:bg-slate-600">
          Annuler (Esc)
        </button>
      </div>
    </div>
  )
}

function ProductPackagingHints({ packagings, currency }) {
  const active = (packagings || []).filter((p) => p.active !== false)
  if (active.length <= 1) return null
  return (
    <div className="text-[10px] text-slate-400 mt-1 space-y-0.5">
      {active.slice(0, 3).map((p) => (
        <div key={p.id}>{p.nom} : {formatMoney(p.salePrice, currency)}</div>
      ))}
    </div>
  )
}

export default function POSPage() {
  const { user, hasPermission } = useAuth()
  const navigate = useNavigate()
  const notify = useNotification()
  const searchRef = useRef(null)
  const qtyRef = useRef(null)
  const searchTimerRef = useRef(null)
  const searchRequestRef = useRef(0)

  const [context, setContext] = useState(null)
  const [session, setSession] = useState(null)
  const [catalog, setCatalog] = useState(null)
  const [products, setProducts] = useState([])
  const [categoryId, setCategoryId] = useState(null)
  const [search, setSearch] = useState('')
  const [searchResults, setSearchResults] = useState(null)
  const [searchMatchType, setSearchMatchType] = useState(null)
  const [searchOpen, setSearchOpen] = useState(false)
  const [searchHighlight, setSearchHighlight] = useState(0)
  const [sale, setSale] = useState(null)
  const [holdSales, setHoldSales] = useState([])
  const [showPayment, setShowPayment] = useState(false)
  const [ticket, setTicket] = useState(null)
  const [clock, setClock] = useState(new Date())
  const [online] = useState(true)
  const [openingSession, setOpeningSession] = useState(false)
  const [showOpenModal, setShowOpenModal] = useState(false)
  const [showCloseModal, setShowCloseModal] = useState(false)
  const [nextQty, setNextQty] = useState('')
  const [packagingPicker, setPackagingPicker] = useState(null)

  const currency = context?.publicSettings?.currency || 'EUR'
  const warehouseId = session?.warehouseId
  const posConfig = context?.posConfig
  const salesFlowMode = posConfig?.salesFlowMode
    || (posConfig?.cashHandlingMode === 'CENTRAL_CASHIER' ? 'CENTRAL_CASHIER' : 'SELLER_COLLECTS_PAYMENT')
  const isCentralCashier = salesFlowMode === 'CENTRAL_CASHIER'
  const isSellerCollects = salesFlowMode === 'SELLER_COLLECTS_PAYMENT'
  const canCollect = hasPermission('pos.payment.collect')
  const canSendToPayment = hasPermission('pos.sale.send_to_payment')
    || hasPermission('pos.sale.prepare')
    || hasPermission('pos.sale.create')
  const canOpenCashSession = hasPermission('pos.session.open')
  const canPrepareSales = hasPermission('pos.sale.create') || canSendToPayment
  const showPaymentButton = isSellerCollects && canCollect
  const showSendToCash = isCentralCashier && canSendToPayment
  const requiredSessionType = isCentralCashier ? 'SALES' : 'CASHIER'
  const canReprint = hasPermission('pos.ticket.print') || hasPermission('pos.ticket.reprint')
  const canStartSession = isCentralCashier
    ? canPrepareSales || canOpenCashSession
    : canOpenCashSession || canPrepareSales
  const wrongSessionType = session?.sessionType && session.sessionType !== requiredSessionType

  const submitToCash = useCallback(async () => {
    if (!sale?.id || !sale.lignes?.length) return
    try {
      await posApi.sendToPayment(sale.id)
      setSale(null)
      setNextQty('')
      notify.success('Vente transférée à la caisse — le caissier peut l’encaisser')
    } catch (e) {
      notify.error(getErrorMessage(e))
    }
  }, [sale, notify])

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

  const addProductWithPackaging = useCallback(async (product, packagingId, qtyOverride) => {
    try {
      const qty = qtyOverride ?? parseQty(nextQty, 1)
      const s = await ensureSale()
      const payload = { productId: product.id, quantityInput: qty }
      if (packagingId) payload.packagingId = packagingId
      const updated = await posApi.addLine(s.id, payload)
      setSale(updated)
      setNextQty('')
      const pkg = product.packagings?.find((p) => p.id === packagingId)
      const label = pkg ? `${product.nom} — ${pkg.nom}` : product.nom
      notify.success(`${label} × ${qty}`)
    } catch (e) {
      notify.error(getErrorMessage(e))
    }
  }, [ensureSale, notify, nextQty])

  const addProduct = useCallback(async (product, qtyOverride) => {
    const active = (product.packagings || []).filter((p) => p.active !== false)

    if (product.matchedPackagingId) {
      await addProductWithPackaging(product, product.matchedPackagingId, qtyOverride)
      return
    }

    if (active.length === 1) {
      await addProductWithPackaging(product, active[0].id, qtyOverride)
      return
    }

    if (active.length > 1) {
      setPackagingPicker({ product, qty: qtyOverride ?? parseQty(nextQty, 1) })
      return
    }

    await addProductWithPackaging(product, null, qtyOverride)
  }, [addProductWithPackaging, nextQty])

  const updateLineQty = useCallback(async (lineId, rawQty) => {
    if (!sale?.id) return
    const qty = parseQty(rawQty, 0)
    try {
      const updated = await posApi.updateLine(sale.id, lineId, qty)
      setSale(updated)
    } catch (e) {
      notify.error(getErrorMessage(e))
    }
  }, [sale?.id, notify])

  const clearSearch = useCallback(() => {
    clearTimeout(searchTimerRef.current)
    setSearch('')
    setSearchResults(null)
    setSearchMatchType(null)
    setSearchOpen(false)
    setSearchHighlight(0)
  }, [])

  const pickSearchProduct = useCallback(async (product, fromQuery) => {
    const { qty: prefixQty } = parseSearchWithQty(fromQuery ?? search)
    await addProduct(product, prefixQty ?? undefined)
    clearSearch()
    searchRef.current?.focus()
  }, [addProduct, clearSearch, search])

  const runSearch = useCallback(async (q, { autoAddExact = false } = {}) => {
    const { qty: prefixQty, term } = parseSearchWithQty(q)
    if (prefixQty != null) setNextQty(String(prefixQty))

    if (!term) {
      setSearchResults(null)
      setSearchMatchType(null)
      setSearchOpen(false)
      setSearchHighlight(0)
      return
    }

    const requestId = ++searchRequestRef.current
    try {
      const data = await posApi.search(term, { warehouseId })
      if (requestId !== searchRequestRef.current) return

      const items = data.products ?? (Array.isArray(data) ? data : [])
      const matchType = data.matchType ?? (items.length ? 'TEXT' : 'NONE')

      setSearchResults(items)
      setSearchMatchType(matchType)
      setSearchOpen(true)
      setSearchHighlight(0)

      const isExactScan = matchType === 'EXACT_BARCODE'
        || matchType === 'EXACT_SKU'
        || matchType === 'EXACT_PACKAGING_BARCODE'
      if (autoAddExact && isExactScan && items.length === 1) {
        await pickSearchProduct(items[0], q)
      }
    } catch (e) {
      if (requestId === searchRequestRef.current) {
        notify.error(getErrorMessage(e))
      }
    }
  }, [warehouseId, pickSearchProduct, notify])

  const onSearchChange = (e) => {
    const value = e.target.value
    setSearch(value)
    clearTimeout(searchTimerRef.current)

    const { qty: prefixQty, term } = parseSearchWithQty(value)
    if (prefixQty != null) setNextQty(String(prefixQty))

    if (!term && !value.trim()) {
      setSearchResults(null)
      setSearchMatchType(null)
      setSearchOpen(false)
      setSearchHighlight(0)
      return
    }
    if (!term) {
      setSearchResults(null)
      setSearchMatchType(null)
      setSearchOpen(false)
      setSearchHighlight(0)
      return
    }

    searchTimerRef.current = setTimeout(() => {
      runSearch(value, { autoAddExact: true })
    }, 350)
  }

  const onSearchKeyDown = (e) => {
    if (e.key === 'ArrowDown') {
      e.preventDefault()
      if (searchResults?.length) {
        setSearchHighlight((i) => Math.min(i + 1, searchResults.length - 1))
      }
      return
    }
    if (e.key === 'ArrowUp') {
      e.preventDefault()
      if (searchResults?.length) {
        setSearchHighlight((i) => Math.max(i - 1, 0))
      }
      return
    }
    if (e.key === 'Escape') {
      e.preventDefault()
      clearSearch()
      return
    }
    if (e.key === 'Enter') {
      e.preventDefault()
      clearTimeout(searchTimerRef.current)
      if (searchResults?.length) {
        pickSearchProduct(searchResults[searchHighlight] ?? searchResults[0])
      } else if (search.trim()) {
        runSearch(search, { autoAddExact: true })
      }
    }
  }

  useEffect(() => {
    if (isCashierOnlyUser(user)) {
      navigate('/pos/pending', { replace: true })
    }
  }, [user, navigate])

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
      if (e.key === 'F3') { e.preventDefault(); qtyRef.current?.focus(); qtyRef.current?.select() }
      if (e.key === 'F4') {
        e.preventDefault()
        if (sale?.lignes?.length) {
          if (showSendToCash) submitToCash()
          else if (showPaymentButton) setShowPayment(true)
        }
      }
      if (e.key === 'F8' && sale?.id) {
        e.preventDefault()
        posApi.holdSale(sale.id).then(() => {
          notify.success('Panier mis en pause — reprise vendeur (F9), pas envoyé à la caisse')
          setSale(null)
          setNextQty('')
          posApi.listHold().then(setHoldSales)
        }).catch((err) => notify.error(getErrorMessage(err)))
      }
      if (e.key === 'F9') {
        e.preventDefault()
        posApi.listHold().then(setHoldSales)
      }
      if (e.key === 'Escape') {
        if (searchOpen) {
          clearSearch()
        } else {
          setShowPayment(false)
          setTicket(null)
        }
      }
      if (e.ctrlKey && e.key === 'n') { e.preventDefault(); setSale(null); posApi.createSale().then(setSale) }
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [sale, notify, searchOpen, clearSearch, showSendToCash, showPaymentButton, submitToCash])

  const openSession = async (openingCashAmount = 0) => {
    setOpeningSession(true)
    try {
      const sessionType = requiredSessionType
      const payload = { sessionType, openingCashAmount: sessionType === 'CASHIER' ? openingCashAmount : 0 }
      const s = await posApi.openSession(payload)
      setSession(s)
      setShowOpenModal(false)
      notify.success(sessionType === 'SALES' ? 'Session vente ouverte' : 'Session caisse ouverte')
      notifyPosSessionChanged()
      await loadCatalog(s.warehouseId, null)
      await refreshContext()
    } catch (e) {
      notify.error(getErrorMessage(e))
    } finally {
      setOpeningSession(false)
    }
  }

  const startOpenSession = () => {
    if (requiredSessionType === 'CASHIER') {
      setShowOpenModal(true)
    } else {
      openSession(0)
    }
  }

  const closeSalesSession = async () => {
    if (!window.confirm('Confirmer la clôture de session ?')) return
    try {
      if (sale?.id) {
        const hasLines = (sale.lignes?.length ?? 0) > 0
        if (hasLines) {
          const abandon = window.confirm(
            'Un panier est en cours. Annuler cette vente et fermer la session ?',
          )
          if (!abandon) return
        }
        await posApi.cancelSale(sale.id)
        setSale(null)
      }
      const report = await posApi.closeSession({ closingCashAmount: 0 })
      notify.success(`Session fermée — ${report.saleCount} vente(s)`)
      setSession(null)
      notifyPosSessionChanged()
    } catch (e) {
      const msg = getErrorMessage(e)
      if (msg.includes('brouillon')) {
        const force = window.confirm(`${msg}\n\nAnnuler toutes les ventes brouillon et fermer ?`)
        if (force) {
          try {
            await posApi.closeSession({ closingCashAmount: 0, cancelPendingDrafts: true })
            setSession(null)
            setSale(null)
            notify.success('Session fermée')
            notifyPosSessionChanged()
          } catch (e2) {
            notify.error(getErrorMessage(e2))
          }
        }
      } else {
        notify.error(msg)
      }
    }
  }

  const handleCloseClick = () => {
    if (session?.sessionType === 'CASHIER') {
      setShowCloseModal(true)
    } else {
      closeSalesSession()
    }
  }

  const onCashSessionClosed = () => {
    setShowCloseModal(false)
    setSession(null)
    setSale(null)
    notifyPosSessionChanged()
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
    const openingLabel = isCentralCashier ? 'Session vente' : 'Session caisse'
    const openingType = requiredSessionType
    return (
      <>
        <div className="flex-1 flex flex-col items-center justify-center gap-6 p-8 max-w-lg mx-auto text-center">
          <PosSessionTypeBadge type={openingType} size="lg" />
          <h2 className="text-2xl font-semibold">
            {isCentralCashier ? 'Ouvrir le poste vendeur' : 'Ouvrir la caisse'}
          </h2>
          <p className="text-slate-400">
            {isCentralCashier
              ? 'Composez le panier ici, puis envoyez la vente à l’encaissement. La pause client (F8) reste sur ce poste.'
              : 'Vente et paiement sur le même écran. Ouvrez une session caisse pour commencer.'}
          </p>
          {!canStartSession && (
            <p className="text-amber-300 text-sm">
              Vous n’avez pas la permission d’ouvrir une session ({openingLabel.toLowerCase()}).
            </p>
          )}
          {canStartSession && (
            <button type="button" disabled={openingSession} onClick={startOpenSession}
              className="px-8 py-4 bg-emerald-600 hover:bg-emerald-500 rounded-xl text-lg font-medium disabled:opacity-50">
              Ouvrir {openingLabel.toLowerCase()}
            </button>
          )}
        </div>
        {showOpenModal && (
          <CashSessionOpenModal
            currency={currency}
            loading={openingSession}
            onClose={() => setShowOpenModal(false)}
            onConfirm={(amount) => openSession(amount)}
          />
        )}
      </>
    )
  }

  if (wrongSessionType) {
    const needed = requiredSessionType === 'SALES' ? 'vente' : 'caisse'
    const current = session.sessionType === 'SALES' ? 'vente' : 'caisse'
    return (
      <div className="flex-1 flex flex-col items-center justify-center gap-6 p-8 max-w-xl mx-auto text-center">
        <PosSessionChip session={session} expectedType={requiredSessionType} centralMode={isCentralCashier} />
        <h2 className="text-xl font-semibold text-amber-300">Mauvais poste pour cette session</h2>
        <p className="text-slate-400">
          Vous avez une session <strong className="text-white">{current}</strong> ouverte.
          Cet écran nécessite une session <strong className="text-white">{needed}</strong>.
        </p>
        {session.sessionType === 'CASHIER' && isCentralCashier && canCollect && (
          <p className="text-sm text-slate-500">
            Passez à l’onglet <strong className="text-emerald-400">Encaissement</strong> en haut de l’écran.
          </p>
        )}
        {(hasPermission('pos.session.close') || canPrepareSales) && (
          <button type="button" onClick={handleCloseClick}
            className="px-6 py-3 bg-slate-700 hover:bg-slate-600 rounded-xl text-sm font-medium">
            Fermer la session {current}
          </button>
        )}
      </div>
    )
  }

  const categories = flatCategories(catalog?.categories)
  const lines = sale?.lignes || []
  const pendingQty = parseQty(nextQty, 1)

  return (
    <div className="flex-1 flex flex-col min-h-0">
      {/* Header */}
      <header className="px-4 py-3 bg-slate-900 border-b border-slate-800 flex flex-wrap items-center gap-4 text-sm">
        <PosSessionChip session={session} expectedType={requiredSessionType} centralMode={isCentralCashier} />
        <div className="text-slate-300">
          <p>{user?.firstName} {user?.lastName}</p>
          <p className="text-xs text-slate-500">{clock.toLocaleString('fr-FR')}</p>
        </div>
        <div className="ml-auto flex flex-wrap items-center gap-3">
        <div className={`flex items-center gap-1 text-xs ${online ? 'text-emerald-400' : 'text-red-400'}`}>
          <span className={`w-2 h-2 rounded-full ${online ? 'bg-emerald-400' : 'bg-red-400'}`} />
          {online ? 'En ligne' : 'Hors ligne'}
        </div>
        {canReprint && (
          <Link
            to="/pos/history"
            className="px-4 py-2 bg-slate-800 border border-slate-600 rounded-lg text-sm hover:bg-slate-700"
          >
            Ventes passées
          </Link>
        )}
        {(hasPermission('pos.session.close') || (session.sessionType === 'SALES' && canPrepareSales)) && (
          <button type="button" onClick={handleCloseClick} className="px-4 py-2 bg-red-900/50 border border-red-800 rounded-lg text-sm hover:bg-red-900">
            Fermer session
          </button>
        )}
        </div>
      </header>

      {!isCentralCashier && (
        <div className="px-4 py-2 bg-amber-950/60 border-b border-amber-800 text-amber-200 text-xs text-center">
          Mode « vendeur encaisseur » actif — le bouton « Envoyer à la caisse » n’existe qu’en mode caisse centrale.
          {hasPermission('settings.read') && (
            <> Paramètres → Point de vente → <strong>pos_sales_flow_mode = CENTRAL_CASHIER</strong>.</>
          )}
        </div>
      )}

      {isCentralCashier && showSendToCash && (
        <div className="px-4 py-2 bg-amber-950/40 border-b border-amber-900/50 text-amber-100 text-xs text-center">
          Panier prêt ? Utilisez <strong>Envoyer à la caisse</strong> (F4) — la « Pause client » (F8) ne part pas en caisse.
        </div>
      )}

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
            <div className="flex gap-2">
              <div className="shrink-0 w-[4.5rem]">
                <label htmlFor="pos-next-qty" className="text-[10px] uppercase tracking-wide text-slate-500 block mb-1 px-0.5">
                  Qté (F3)
                </label>
                <input
                  id="pos-next-qty"
                  ref={qtyRef}
                  type="text"
                  inputMode="decimal"
                  value={nextQty}
                  onChange={(e) => setNextQty(e.target.value.replace(/[^0-9.,]/g, ''))}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter') {
                      e.preventDefault()
                      searchRef.current?.focus()
                    }
                  }}
                  placeholder="1"
                  className="w-full rounded-xl px-2 py-3 text-center text-base font-semibold tabular-nums border border-slate-600 focus:outline-none focus:ring-2 focus:ring-emerald-500"
                  autoComplete="off"
                />
              </div>
              <div className="flex-1 min-w-0 relative">
                <label htmlFor="pos-search" className="text-[10px] uppercase tracking-wide text-slate-500 block mb-1 px-0.5">
                  Recherche (F2)
                </label>
                <input
                  id="pos-search"
                  ref={searchRef}
                  type="text"
                  value={search}
                  onChange={onSearchChange}
                  onKeyDown={onSearchKeyDown}
                  placeholder="Code-barres, nom… ou 5* puis scan"
                  className="w-full rounded-xl px-4 py-3 text-base border border-slate-600 focus:outline-none focus:ring-2 focus:ring-emerald-500"
                  autoComplete="off"
                  spellCheck={false}
                />
                {searchOpen && (
                  <PosSearchResults
                    results={searchResults}
                    matchType={searchMatchType}
                    highlightIndex={searchHighlight}
                    currency={currency}
                    onPick={(p) => pickSearchProduct(p)}
                  />
                )}
              </div>
            </div>
            {pendingQty !== 1 && (
              <p className="text-xs text-emerald-400 mt-1.5 px-0.5">
                Prochain produit : ×{pendingQty}
              </p>
            )}
          </div>
          <div className="flex-1 overflow-y-auto grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-3 content-start">
            {products.map((p) => (
              <button
                key={p.id}
                type="button"
                onClick={() => addProduct(p)}
                className="bg-slate-800 border border-slate-700 rounded-xl p-3 text-left hover:border-emerald-500 hover:bg-slate-750 transition-colors min-h-[120px] flex flex-col"
              >
                <p className="font-medium text-sm line-clamp-2 flex-1">{p.nom}</p>
                <p className="text-emerald-400 font-semibold mt-2">{formatMoney(p.unitPrice, currency)}</p>
                <ProductPackagingHints packagings={p.packagings} currency={currency} />
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
          <POSCustomerPanel
            sale={sale}
            currency={currency}
            loyaltyConfig={context?.loyaltyConfig}
            onSaleUpdate={setSale}
          />
          <div className="px-4 py-3 border-b border-slate-800">
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
                  <span className="font-medium line-clamp-1">
                    {l.productNom}
                    {l.packagingNameSnapshot && (
                      <span className="block text-xs text-slate-400 font-normal">{l.packagingNameSnapshot}</span>
                    )}
                  </span>
                  <span>{formatMoney(l.lineTotal, currency)}</span>
                </div>
                <div className="flex items-center gap-2 mt-2 text-slate-400">
                  <button
                    type="button"
                    className="w-7 h-7 bg-slate-800 rounded hover:bg-slate-700"
                    onClick={() => updateLineQty(l.id, Number(l.quantityInput) - 1)}
                  >
                    −
                  </button>
                  <input
                    type="text"
                    inputMode="decimal"
                    aria-label={`Quantité ${l.productNom}`}
                    key={`${l.id}-${l.quantityInput}`}
                    defaultValue={l.quantityInput}
                    className="w-14 h-7 text-center bg-slate-800 border border-slate-600 rounded text-sm text-white tabular-nums focus:border-emerald-500 focus:outline-none"
                    onBlur={(e) => updateLineQty(l.id, e.target.value)}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter') {
                        e.preventDefault()
                        e.currentTarget.blur()
                      }
                    }}
                  />
                  <button
                    type="button"
                    className="w-7 h-7 bg-slate-800 rounded hover:bg-slate-700"
                    onClick={() => updateLineQty(l.id, Number(l.quantityInput) + 1)}
                  >
                    +
                  </button>
                  <span className="text-xs ml-auto">{formatMoney(l.unitPrice, currency)}/cond.</span>
                  <button
                    type="button"
                    className="text-red-400 text-xs hover:text-red-300 px-1"
                    onClick={() => updateLineQty(l.id, 0)}
                  >
                    ✕
                  </button>
                </div>
              </li>
            ))}
          </ul>
          <div className="p-4 border-t border-slate-800 space-y-1 text-sm">
            <div className="flex justify-between text-slate-400"><span>Sous-total</span><span>{formatMoney(sale?.subtotal, currency)}</span></div>
            <div className="flex justify-between text-slate-400"><span>Remise</span><span>{formatMoney(sale?.discountTotal, currency)}</span></div>
            {(sale?.loyaltyDiscountAmount > 0) && (
              <div className="flex justify-between text-amber-400"><span>Fidélité</span><span>-{formatMoney(sale.loyaltyDiscountAmount, currency)}</span></div>
            )}
            <div className="flex justify-between text-slate-400"><span>Taxes</span><span>{formatMoney(sale?.taxTotal, currency)}</span></div>
            <div className="flex justify-between text-lg font-bold pt-2"><span>Total</span><span className="text-emerald-400">{formatMoney(sale?.total, currency)}</span></div>
            {showSendToCash && (
              <button
                type="button"
                disabled={!lines.length}
                onClick={submitToCash}
                className="w-full mt-3 py-3 bg-amber-600 hover:bg-amber-500 rounded-xl font-semibold disabled:opacity-40 disabled:cursor-not-allowed"
              >
                {lines.length
                  ? 'Envoyer à la caisse — à encaisser (F4)'
                  : 'Envoyer à la caisse (F4) — ajoutez des produits'}
              </button>
            )}
            {showPaymentButton && (
              <button
                type="button"
                disabled={!lines.length}
                onClick={() => setShowPayment(true)}
                className="w-full mt-3 py-3 bg-emerald-600 hover:bg-emerald-500 rounded-xl font-semibold disabled:opacity-40"
              >
                Paiement (F4)
              </button>
            )}
            <div className="flex gap-2 mt-2">
              <button type="button" disabled={!sale?.id} onClick={() => posApi.holdSale(sale.id).then(() => { notify.success('Panier en pause vendeur'); setSale(null) })}
                className="flex-1 py-2 bg-slate-800 rounded-lg text-xs disabled:opacity-40" title="Pause locale : servir un autre client, reprise avec F9">Pause client (F8)</button>
              <button type="button" onClick={() => posApi.listHold().then(setHoldSales)}
                className="flex-1 py-2 bg-slate-800 rounded-lg text-xs">Reprendre (F9)</button>
            </div>
          </div>
        </aside>
      </div>

      {holdSales.length > 0 && (
        <div className="fixed bottom-4 right-4 bg-slate-800 border border-slate-600 rounded-xl p-3 max-w-xs z-40">
          <p className="text-xs text-slate-400 mb-2">Paniers en pause (vendeur uniquement)</p>
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
      {packagingPicker && (
        <PackagingPickerModal
          product={packagingPicker.product}
          qty={packagingPicker.qty}
          currency={currency}
          onClose={() => setPackagingPicker(null)}
          onPick={async (packagingId) => {
            const { product, qty } = packagingPicker
            setPackagingPicker(null)
            await addProductWithPackaging(product, packagingId, qty)
            searchRef.current?.focus()
          }}
        />
      )}
      {ticket && <PosTicketModal ticket={ticket} onClose={() => setTicket(null)} />}
      {showOpenModal && (
        <CashSessionOpenModal
          currency={currency}
          loading={openingSession}
          onClose={() => setShowOpenModal(false)}
          onConfirm={(amount) => openSession(amount)}
        />
      )}
      {showCloseModal && session?.sessionType === 'CASHIER' && (
        <CashSessionCloseModal
          session={session}
          currency={currency}
          onClose={() => setShowCloseModal(false)}
          onClosed={onCashSessionClosed}
        />
      )}
    </div>
  )
}
