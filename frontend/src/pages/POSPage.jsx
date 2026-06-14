import { useCallback, useEffect, useRef, useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import POSCustomerPanel from '../components/POSCustomerPanel'
import { posApi } from '../api'
import { useAuth } from '../context/AuthContext'
import { useNotification } from '../context/NotificationContext'
import { getErrorMessage } from '../utils/errors'
import { isCashierOnlyUser } from '../utils/auth'
import { parseQty, parseSearchWithQty } from '../utils/posQty'
import {
  barcodeMinLengthFromContext, isBarcodeAutoAddEnabled, isBarcodeScanEnabled, looksLikeBarcode,
} from '../utils/barcodeScan'
import CashSessionCloseModal from '../components/pos/CashSessionCloseModal'
import CashSessionOpenModal from '../components/pos/CashSessionOpenModal'
import { notifyPosSessionChanged } from '../utils/posSession'
import { notifyPosSaleStateChanged } from '../utils/posSaleEvents'
import { formatStockIssueLine, getSaleStockIssueLines, saleHasStockIssues } from '../utils/posStock'
import { PosSessionChip, PosSessionTypeBadge, PosWrongSessionPanel } from '../components/pos/PosWorkspaceNav'
import ResumeSalesModal from '../components/pos/ResumeSalesModal'
import ModalOverlay from '../components/ui/ModalOverlay'
import { PosTicketModal } from '../components/pos/PosPrintModals'
import PosSearchResults, { resolvePosVariantId } from '../components/pos/PosSearchResults'
import { formatPosMoney } from '../utils/posMoney'

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
    <ModalOverlay open onClose={onClose}>
      <div className="bg-slate-900 border border-slate-700 rounded-xl w-full max-w-lg p-6">
        <h3 className="text-lg font-semibold mb-4">Paiement — {formatPosMoney(total, currency)}</h3>
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
            <p className="text-emerald-400 text-sm mt-2">Monnaie à rendre : {formatPosMoney(change, currency)}</p>
          )}
        </div>
        <div className="flex gap-2 justify-end">
          <button type="button" onClick={onClose} className="px-4 py-2 rounded-lg bg-slate-700 text-sm">Retour à la saisie (Esc)</button>
          <button type="button" disabled={loading || paid < total} onClick={submit}
            className="px-4 py-2 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-sm font-medium disabled:opacity-50">
            Encaisser
          </button>
        </div>
      </div>
    </ModalOverlay>
  )
}

function VariantPickerModal({ product, qty, currency, onClose, onPick }) {
  const variants = product?.variants || []
  return (
    <ModalOverlay open onClose={onClose}>
      <div className="bg-slate-900 border border-slate-700 rounded-xl w-full max-w-md p-6 max-h-[80vh] overflow-y-auto">
        <h3 className="text-lg font-semibold mb-1">{product?.nom}</h3>
        <p className="text-sm text-slate-400 mb-4">Choisissez une variante · quantité ×{qty}</p>
        <div className="space-y-2">
          {variants.length === 0 && (
            <p className="text-sm text-slate-500">Aucune variante vendable</p>
          )}
          {variants.map((v) => (
            <button
              key={v.id}
              type="button"
              disabled={v.outOfStock}
              onClick={() => onPick(v.id)}
              className={`w-full text-left px-4 py-3 rounded-xl border transition-colors ${
                v.outOfStock
                  ? 'border-slate-800 opacity-60 cursor-not-allowed'
                  : 'border-slate-700 hover:border-emerald-500 hover:bg-slate-800'
              }`}
            >
              <span className="font-medium">{v.label || v.sku}</span>
              <span className="text-xs text-slate-400 ml-2">({v.stockAvailable ?? 0} dispo.)</span>
              {v.outOfStock && <span className="text-[10px] bg-red-900 text-red-300 px-1.5 py-0.5 rounded ml-2">Rupture</span>}
              {v.lowStock && !v.outOfStock && <span className="text-[10px] bg-orange-900 text-orange-300 px-1.5 py-0.5 rounded ml-2">Stock faible</span>}
              <span className="float-right text-emerald-400 font-semibold">{formatPosMoney(v.unitPrice, currency)}</span>
              {v.packagings?.length > 0 && (
                <span className="block text-[10px] text-slate-500 mt-1">
                  {v.packagings.slice(0, 2).map((p) => p.nom).join(' · ')}
                </span>
              )}
            </button>
          ))}
        </div>
        <button type="button" onClick={onClose} className="mt-4 w-full py-2 rounded-lg bg-slate-700 text-sm hover:bg-slate-600">
          Annuler (Esc)
        </button>
      </div>
    </ModalOverlay>
  )
}

function PackagingPickerModal({ product, qty, currency, onClose, onPick, packagings: packagingsProp }) {
  const packagings = (packagingsProp || product?.packagings || []).filter((p) => p.active !== false)
  const defaultId = packagings.find((p) => p.defaultSale)?.id ?? packagings[0]?.id

  return (
    <ModalOverlay open onClose={onClose}>
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
              <span className="float-right text-emerald-400 font-semibold">{formatPosMoney(p.salePrice, currency)}</span>
            </button>
          ))}
        </div>
        <button type="button" onClick={onClose} className="mt-4 w-full py-2 rounded-lg bg-slate-700 text-sm hover:bg-slate-600">
          Annuler (Esc)
        </button>
      </div>
    </ModalOverlay>
  )
}

function ProductPackagingHints({ packagings, currency }) {
  const active = (packagings || []).filter((p) => p.active !== false)
  if (active.length <= 1) return null
  return (
    <div className="text-[10px] text-slate-400 mt-1 space-y-0.5">
      {active.slice(0, 3).map((p) => (
        <div key={p.id}>{p.nom} : {formatPosMoney(p.salePrice, currency)}</div>
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
  const [searchMessage, setSearchMessage] = useState(null)
  const [searchOpen, setSearchOpen] = useState(false)
  const [searchHighlight, setSearchHighlight] = useState(0)
  const [sale, setSale] = useState(null)
  const [holdSales, setHoldSales] = useState([])
  const [holdCount, setHoldCount] = useState(0)
  const [draftSales, setDraftSales] = useState([])
  const [showResumeModal, setShowResumeModal] = useState(false)
  const [showPayment, setShowPayment] = useState(false)
  const [ticket, setTicket] = useState(null)
  const [clock, setClock] = useState(new Date())
  const [online] = useState(true)
  const [openingSession, setOpeningSession] = useState(false)
  const [showOpenModal, setShowOpenModal] = useState(false)
  const [showCloseModal, setShowCloseModal] = useState(false)
  const [nextQty, setNextQty] = useState('')
  const [packagingPicker, setPackagingPicker] = useState(null)
  const [variantPicker, setVariantPicker] = useState(null)

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

  const loadResumeSales = useCallback(async () => {
    const holds = await posApi.listHold()
    setHoldSales(holds)
    setHoldCount(holds.length)
    if (isCentralCashier) {
      const drafts = await posApi.listDraft()
      setDraftSales(drafts)
    } else {
      setDraftSales([])
    }
  }, [isCentralCashier])

  const refreshHoldCount = useCallback(async () => {
    if (!isCentralCashier || !canPrepareSales) return
    try {
      const count = await posApi.holdCount()
      setHoldCount(count ?? 0)
    } catch {
      /* ignore polling errors */
    }
  }, [isCentralCashier, canPrepareSales])

  const sendSaleToCashier = useCallback(async () => {
    if (!sale?.id || !sale.lignes?.length) return
    if (saleHasStockIssues(sale)) {
      notify.error('Stock insuffisant — corrigez les quantités avant l’envoi à la caisse.')
      return
    }
    try {
      await posApi.sendToPayment(sale.id)
      setSale(null)
      setNextQty('')
      notify.success('Vente transférée à la caisse — le caissier peut l’encaisser')
      await loadResumeSales()
      notifyPosSaleStateChanged()
    } catch (e) {
      notify.error(getErrorMessage(e))
    }
  }, [sale, notify, loadResumeSales])

  const pauseClientSale = useCallback(async () => {
    if (!sale?.id) return
    try {
      await posApi.holdSale(sale.id)
      setSale(null)
      setNextQty('')
      notify.success('Panier mis en pause — reprise vendeur (F9)')
      await loadResumeSales()
      notifyPosSaleStateChanged()
    } catch (e) {
      notify.error(getErrorMessage(e))
    }
  }, [sale, notify, loadResumeSales])

  const openResumeModal = useCallback(async () => {
    try {
      await loadResumeSales()
      setShowResumeModal(true)
    } catch (e) {
      notify.error(getErrorMessage(e))
    }
  }, [loadResumeSales, notify])

  const handleResumeHold = useCallback(async (h) => {
    try {
      const updated = await posApi.resumeSale(h.id)
      setSale(updated)
      setShowResumeModal(false)
      await loadResumeSales()
    } catch (e) {
      notify.error(getErrorMessage(e))
    }
  }, [notify, loadResumeSales])

  const handlePickDraft = useCallback(async (d) => {
    try {
      const full = await posApi.getSale(d.id)
      setSale(full)
      setShowResumeModal(false)
      await loadResumeSales()
    } catch (e) {
      notify.error(getErrorMessage(e))
    }
  }, [notify, loadResumeSales])

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

  const addProductWithPackaging = useCallback(async (product, packagingId, qtyOverride, variantId) => {
    try {
      const qty = qtyOverride ?? parseQty(nextQty, 1)
      const s = await ensureSale()
      const payload = { productId: product.id, quantityInput: qty }
      if (packagingId) payload.packagingId = packagingId
      if (variantId) payload.variantId = variantId
      const updated = await posApi.addLine(s.id, payload)
      setSale(updated)
      setNextQty('')
      const variant = product.variants?.find((v) => v.id === variantId)
      const pkg = (variant?.packagings || product.packagings)?.find((p) => p.id === packagingId)
      let label = product.nom
      if (variant?.label) label += ` — ${variant.label}`
      if (pkg) label += ` — ${pkg.nom}`
      notify.success(`${label} × ${qty}`)
    } catch (e) {
      notify.error(getErrorMessage(e))
    }
  }, [ensureSale, notify, nextQty])

  const resolvePackagings = useCallback((product, variantId) => {
    if (variantId) {
      const variant = product.variants?.find((v) => v.id === variantId)
      if (variant?.packagings?.length) return variant.packagings
    }
    return product.packagings || []
  }, [])

  const addProduct = useCallback(async (product, qtyOverride, variantId) => {
    const effectiveVariantId = resolvePosVariantId(product, variantId)

    if (product.hasVariants && !product.sellable && !effectiveVariantId) {
      setVariantPicker({ product, qty: qtyOverride ?? parseQty(nextQty, 1) })
      return
    }
    if (!product.sellable && !effectiveVariantId) {
      notify.error('Ce produit n\'est pas vendable directement.')
      return
    }

    const packagings = resolvePackagings(product, effectiveVariantId)
    const active = packagings.filter((p) => p.active !== false)
    const productWithPack = { ...product, packagings: active, matchedVariantId: effectiveVariantId }

    if (product.matchedPackagingId) {
      await addProductWithPackaging(productWithPack, product.matchedPackagingId, qtyOverride, effectiveVariantId)
      return
    }

    if (active.length === 1) {
      await addProductWithPackaging(productWithPack, active[0].id, qtyOverride, effectiveVariantId)
      return
    }

    const defaultPackaging = active.find((p) => p.defaultSale)
    if (active.length > 1 && defaultPackaging && (product.matchedVariantId || product.matchedPackagingId)) {
      await addProductWithPackaging(productWithPack, defaultPackaging.id, qtyOverride, effectiveVariantId)
      return
    }

    if (active.length > 1) {
      setPackagingPicker({ product: productWithPack, qty: qtyOverride ?? parseQty(nextQty, 1), variantId: effectiveVariantId })
      return
    }

    await addProductWithPackaging(productWithPack, null, qtyOverride, effectiveVariantId)
  }, [addProductWithPackaging, nextQty, notify, resolvePackagings])

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
    setSearchMessage(null)
    setSearchOpen(false)
    setSearchHighlight(0)
  }, [])

  const scanBarcode = useCallback(async (rawQuery) => {
    const { qty: prefixQty, term } = parseSearchWithQty(rawQuery)
    const minLen = barcodeMinLengthFromContext(context)
    if (!looksLikeBarcode(term, minLen) || !isBarcodeScanEnabled(context)) {
      return false
    }
    try {
      const qty = prefixQty ?? parseQty(nextQty, 1)
      const s = await ensureSale()
      const result = await posApi.scanItem(s.id, { code: term, quantityInput: qty })
      setSale(result.sale)
      setNextQty('')
      notify.success(result.message || 'Produit ajouté')
      clearSearch()
      searchRef.current?.focus()
      return true
    } catch (e) {
      notify.error(getErrorMessage(e))
      clearSearch()
      searchRef.current?.focus()
      return true
    }
  }, [context, ensureSale, nextQty, notify, clearSearch])

  const pickSearchProduct = useCallback(async (row, fromQuery) => {
    const product = row?.product ?? row
    const variantId = row?.variantId ?? resolvePosVariantId(product)
    const { qty: prefixQty } = parseSearchWithQty(fromQuery ?? search)
    await addProduct(product, prefixQty ?? undefined, variantId)
    clearSearch()
    searchRef.current?.focus()
  }, [addProduct, clearSearch, search])

  const runSearch = useCallback(async (q, { autoAddExact = false } = {}) => {
    const { qty: prefixQty, term } = parseSearchWithQty(q)
    if (prefixQty != null) setNextQty(String(prefixQty))

    if (!term) {
      setSearchResults(null)
      setSearchMatchType(null)
      setSearchMessage(null)
      setSearchOpen(false)
      setSearchHighlight(0)
      return
    }

    const minLen = barcodeMinLengthFromContext(context)
    if (looksLikeBarcode(term, minLen) && isBarcodeScanEnabled(context)) {
      setSearchResults(null)
      setSearchMatchType(null)
      setSearchMessage(null)
      setSearchOpen(false)
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
      setSearchMessage(data.message ?? null)
      setSearchOpen(true)
      setSearchHighlight(0)

      if (matchType === 'BARCODE_NOT_FOUND') {
        notify.error(data.message || 'Aucun produit trouvé pour ce code-barres')
        return
      }
      if (matchType === 'BARCODE_AMBIGUOUS') {
        notify.error(data.message || 'Plusieurs articles correspondent à ce code-barres')
        return
      }

      const isExactScan = matchType === 'EXACT_BARCODE'
        || matchType === 'EXACT_SKU'
        || matchType === 'EXACT_PACKAGING_BARCODE'
      if (autoAddExact && isExactScan && items.length === 1 && isBarcodeAutoAddEnabled(context)) {
        await pickSearchProduct({
          product: items[0],
          variantId: resolvePosVariantId(items[0]),
        }, q)
      }
    } catch (e) {
      if (requestId === searchRequestRef.current) {
        notify.error(getErrorMessage(e))
      }
    }
  }, [warehouseId, pickSearchProduct, notify, context])

  const onSearchChange = (e) => {
    const value = e.target.value
    setSearch(value)
    clearTimeout(searchTimerRef.current)

    const { qty: prefixQty, term } = parseSearchWithQty(value)
    if (prefixQty != null) setNextQty(String(prefixQty))

    if (!term && !value.trim()) {
      setSearchResults(null)
      setSearchMatchType(null)
      setSearchMessage(null)
      setSearchOpen(false)
      setSearchHighlight(0)
      return
    }
    if (!term) {
      setSearchResults(null)
      setSearchMatchType(null)
      setSearchMessage(null)
      setSearchOpen(false)
      setSearchHighlight(0)
      return
    }

    const minLen = barcodeMinLengthFromContext(context)
    if (looksLikeBarcode(term, minLen) && isBarcodeScanEnabled(context)) {
      setSearchResults(null)
      setSearchMatchType(null)
      setSearchMessage(null)
      setSearchOpen(false)
      return
    }

    searchTimerRef.current = setTimeout(() => {
      runSearch(value, { autoAddExact: true })
    }, 350)
  }

  const onSearchKeyDown = (e) => {
    const rows = expandPosSearchResults(searchResults, searchMatchType)
    if (e.key === 'ArrowDown') {
      e.preventDefault()
      if (rows.length) {
        setSearchHighlight((i) => Math.min(i + 1, rows.length - 1))
      }
      return
    }
    if (e.key === 'ArrowUp') {
      e.preventDefault()
      if (rows.length) {
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
      const minLen = barcodeMinLengthFromContext(context)
      const { term } = parseSearchWithQty(search)
      if (looksLikeBarcode(term, minLen) && isBarcodeScanEnabled(context)) {
        scanBarcode(search)
        return
      }
      if (rows.length) {
        pickSearchProduct(rows[searchHighlight] ?? rows[0])
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
    if (session && isCentralCashier && canPrepareSales) {
      refreshHoldCount()
    }
  }, [session, isCentralCashier, canPrepareSales, refreshHoldCount])

  useEffect(() => {
    if (!session || !isCentralCashier || !canPrepareSales) return undefined
    const tick = () => {
      if (showResumeModal) {
        loadResumeSales().catch(() => {})
      } else {
        refreshHoldCount()
      }
    }
    tick()
    const timer = setInterval(tick, 15000)
    return () => clearInterval(timer)
  }, [session, isCentralCashier, canPrepareSales, showResumeModal, loadResumeSales, refreshHoldCount])

  useEffect(() => {
    if (!isCentralCashier || !canPrepareSales) return undefined
    const onSaleChange = () => {
      loadResumeSales().catch(() => {})
    }
    window.addEventListener('pos-sale-state-changed', onSaleChange)
    return () => window.removeEventListener('pos-sale-state-changed', onSaleChange)
  }, [isCentralCashier, canPrepareSales, loadResumeSales])

  const stockAlertShownRef = useRef(false)

  useEffect(() => {
    if (!showSendToCash) {
      stockAlertShownRef.current = false
      return
    }
    if (saleHasStockIssues(sale)) {
      if (!stockAlertShownRef.current) {
        const issueLines = getSaleStockIssueLines(sale)
        const detail = issueLines.slice(0, 2).map(formatStockIssueLine).join(' · ')
        const suffix = issueLines.length > 2 ? ` (+${issueLines.length - 2})` : ''
        notify.error(`Stock insuffisant — envoi à la caisse bloqué. ${detail}${suffix}`)
        stockAlertShownRef.current = true
      }
    } else {
      stockAlertShownRef.current = false
    }
  }, [sale, showSendToCash, notify])

  useEffect(() => {
    const onKey = (e) => {
      if (e.key === 'F2') { e.preventDefault(); searchRef.current?.focus() }
      if (e.key === 'F3') { e.preventDefault(); qtyRef.current?.focus(); qtyRef.current?.select() }
      if (e.key === 'F4') {
        e.preventDefault()
        if (sale?.lignes?.length) {
          if (showSendToCash) {
            if (saleHasStockIssues(sale)) {
              notify.error('Stock insuffisant — corrigez les quantités avant l’envoi à la caisse.')
            } else {
              sendSaleToCashier()
            }
          } else if (showPaymentButton) setShowPayment(true)
        }
      }
      if (e.key === 'F8' && sale?.id) {
        e.preventDefault()
        pauseClientSale()
      }
      if (e.key === 'F9') {
        e.preventDefault()
        openResumeModal()
      }
      if (e.key === 'Escape') {
        if (searchOpen) {
          clearSearch()
        } else {
          setShowResumeModal(false)
          setShowPayment(false)
          setTicket(null)
          setVariantPicker(null)
          setPackagingPicker(null)
        }
      }
      if (e.ctrlKey && e.key === 'n') { e.preventDefault(); setSale(null); posApi.createSale().then(setSale) }
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [sale, searchOpen, clearSearch, showSendToCash, showPaymentButton, sendSaleToCashier, pauseClientSale, openResumeModal, notify])

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
    return (
      <>
        <PosWrongSessionPanel
          session={session}
          expectedType={requiredSessionType}
          centralMode={isCentralCashier}
          onCloseSession={handleCloseClick}
          canCloseSession={hasPermission('pos.session.close') || canPrepareSales || canCollect}
        />
        {showCloseModal && session?.sessionType === 'CASHIER' && (
          <CashSessionCloseModal
            session={session}
            currency={currency}
            companyName={context?.publicSettings?.companyName}
            onClose={() => setShowCloseModal(false)}
            onClosed={onCashSessionClosed}
          />
        )}
      </>
    )
  }

  const categories = flatCategories(catalog?.categories)
  const lines = sale?.lignes || []
  const stockIssues = saleHasStockIssues(sale)
  const canSendToCashier = showSendToCash && lines.length > 0 && !stockIssues
  const pendingQty = parseQty(nextQty, 1)

  return (
    <div className="flex-1 flex flex-col min-h-0">
      {/* Header */}
      <header className="px-4 py-3 bg-slate-900 border-b border-slate-800 flex flex-wrap items-center gap-4 text-sm">
        <PosSessionChip session={session} expectedType={requiredSessionType} centralMode={isCentralCashier} />
        {isCentralCashier && canPrepareSales && (
          <button
            type="button"
            onClick={openResumeModal}
            className={`flex items-center gap-2 px-3 py-1.5 rounded-lg border text-sm font-medium transition-colors ${
              holdCount > 0
                ? 'bg-amber-500/20 border-amber-400 text-amber-100 animate-pulse'
                : 'bg-slate-800 border-slate-600 text-slate-400'
            }`}
            title="Ventes en attente (retour caisse ou pause client) — F9 pour reprendre"
          >
            <span className={`inline-flex items-center justify-center min-w-[1.5rem] h-6 px-1.5 rounded-full text-xs font-bold ${
              holdCount > 0 ? 'bg-amber-500 text-slate-900' : 'bg-slate-700 text-slate-300'
            }`}>
              {holdCount}
            </span>
            en attente
          </button>
        )}
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

      {isCentralCashier && showSendToCash && stockIssues && (
        <div className="px-4 py-2 bg-red-950/70 border-b border-red-800 text-red-200 text-xs text-center">
          <strong>Stock insuffisant</strong> — corrigez les quantités du panier avant l’envoi à la caisse.
          {getSaleStockIssueLines(sale).slice(0, 3).map((l) => (
            <span key={l.id} className="block mt-1 text-red-300/90">{formatStockIssueLine(l)}</span>
          ))}
        </div>
      )}

      {isCentralCashier && showSendToCash && !stockIssues && (
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
                    message={searchMessage}
                    highlightIndex={searchHighlight}
                    currency={currency}
                    onPick={(row) => pickSearchProduct(row, search)}
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
                {p.hasVariants && p.variants?.length > 0 && (
                  <p className="text-[10px] text-slate-400 mt-0.5 line-clamp-2">
                    {p.variants.map((v) => v.label || v.sku).join(' · ')}
                  </p>
                )}
                <p className="text-emerald-400 font-semibold mt-2">{formatPosMoney(p.unitPrice, currency)}</p>
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
              <li key={l.id} className={`p-3 text-sm ${l.stockInsufficient ? 'bg-red-950/30' : ''}`}>
                <div className="flex justify-between gap-2">
                  <span className="font-medium line-clamp-1">
                    {l.variantNameSnapshot ? `${l.productNom} — ${l.variantNameSnapshot}` : l.productNom}
                    {l.packagingNameSnapshot && (
                      <span className="block text-xs text-slate-400 font-normal">{l.packagingNameSnapshot}</span>
                    )}
                  </span>
                  <span>{formatPosMoney(l.lineTotal, currency)}</span>
                </div>
                {l.stockInsufficient && (
                  <p className="text-xs text-red-400 mt-1">
                    Stock insuffisant — {Number(l.quantityInput)} en panier, {Number(l.stockAvailable ?? 0)} disponible(s)
                  </p>
                )}
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
                  <span className="text-xs ml-auto">{formatPosMoney(l.unitPrice, currency)}/cond.</span>
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
            <div className="flex justify-between text-slate-400"><span>Sous-total</span><span>{formatPosMoney(sale?.subtotal, currency)}</span></div>
            <div className="flex justify-between text-slate-400"><span>Remise</span><span>{formatPosMoney(sale?.discountTotal, currency)}</span></div>
            {(sale?.loyaltyDiscountAmount > 0) && (
              <div className="flex justify-between text-amber-400"><span>Fidélité</span><span>-{formatPosMoney(sale.loyaltyDiscountAmount, currency)}</span></div>
            )}
            <div className="flex justify-between text-slate-400"><span>Taxes</span><span>{formatPosMoney(sale?.taxTotal, currency)}</span></div>
            <div className="flex justify-between text-lg font-bold pt-2"><span>Total</span><span className="text-emerald-400">{formatPosMoney(sale?.total, currency)}</span></div>
            {canSendToCashier && (
              <button
                type="button"
                onClick={sendSaleToCashier}
                className="w-full mt-3 py-3 bg-amber-600 hover:bg-amber-500 rounded-xl font-semibold"
              >
                Envoyer à la caisse — à encaisser (F4)
              </button>
            )}
            {showSendToCash && stockIssues && lines.length > 0 && (
              <p className="w-full mt-3 py-3 rounded-xl text-center text-sm font-medium bg-red-950/50 border border-red-800 text-red-300">
                Envoi à la caisse indisponible — stock insuffisant
              </p>
            )}
            {showSendToCash && !lines.length && (
              <button
                type="button"
                disabled
                className="w-full mt-3 py-3 bg-amber-600 rounded-xl font-semibold opacity-40 cursor-not-allowed"
              >
                Envoyer à la caisse (F4) — ajoutez des produits
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
              <button type="button" disabled={!sale?.id} onClick={pauseClientSale}
                className="flex-1 py-2 bg-slate-800 rounded-lg text-xs disabled:opacity-40" title="Pause locale : servir un autre client, reprise avec F9">Pause client (F8)</button>
              <button type="button" onClick={openResumeModal}
                className="flex-1 py-2 bg-slate-800 rounded-lg text-xs">Reprendre (F9)</button>
            </div>
          </div>
        </aside>
      </div>

      <ResumeSalesModal
        open={showResumeModal}
        onClose={() => setShowResumeModal(false)}
        holdSales={holdSales}
        draftSales={draftSales}
        currency={currency}
        isCentralCashier={isCentralCashier}
        onResumeHold={handleResumeHold}
        onPickDraft={handlePickDraft}
      />

      {showPayment && sale && (
        <PaymentModal sale={sale} currency={currency} onClose={() => setShowPayment(false)} onPaid={onPaid} />
      )}
      {variantPicker && (
        <VariantPickerModal
          product={variantPicker.product}
          qty={variantPicker.qty}
          currency={currency}
          onClose={() => setVariantPicker(null)}
          onPick={async (variantId) => {
            const { product, qty } = variantPicker
            setVariantPicker(null)
            await addProduct(product, qty, variantId)
            searchRef.current?.focus()
          }}
        />
      )}
      {packagingPicker && (
        <PackagingPickerModal
          product={packagingPicker.product}
          qty={packagingPicker.qty}
          currency={currency}
          packagings={packagingPicker.product?.packagings}
          onClose={() => setPackagingPicker(null)}
          onPick={async (packagingId) => {
            const { product, qty, variantId } = packagingPicker
            setPackagingPicker(null)
            await addProductWithPackaging(product, packagingId, qty, variantId)
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
          companyName={context?.publicSettings?.companyName}
          onClose={() => setShowCloseModal(false)}
          onClosed={onCashSessionClosed}
        />
      )}
    </div>
  )
}
