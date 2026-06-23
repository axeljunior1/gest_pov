import { useCallback, useEffect, useRef, useState } from 'react'
import { posApi } from '../api'
import { useAuth } from '../context/AuthContext'
import { useNotification } from '../context/NotificationContext'
import { getErrorMessage } from '../utils/errors'
import SearchCriteriaHelp, { SearchMatchHint } from './search/SearchCriteriaHelp'
import { findEntityMatch } from '../utils/entitySearchMatch'

function formatMoney(value, currency = 'EUR') {
  if (value == null) return '—'
  try {
    return new Intl.NumberFormat('fr-FR', { style: 'currency', currency }).format(Number(value))
  } catch {
    return `${Number(value).toFixed(2)} ${currency}`
  }
}

export default function POSCustomerPanel({ sale, currency, loyaltyConfig, onSaleUpdate }) {
  const { hasPermission } = useAuth()
  const notify = useNotification()
  const searchRef = useRef(null)
  const timerRef = useRef(null)

  const [search, setSearch] = useState('')
  const [results, setResults] = useState(null)
  const [showCreate, setShowCreate] = useState(false)
  const [quickForm, setQuickForm] = useState({ lastName: '', firstName: '', phone: '' })
  const [redeemPoints, setRedeemPoints] = useState('')
  const [loading, setLoading] = useState(false)

  const customer = sale?.customerId ? {
    id: sale.customerId,
    name: sale.customerName,
    phone: sale.customerPhone,
    points: sale.customerLoyaltyPoints,
    tier: sale.customerLoyaltyTier,
  } : null

  const loyaltyEnabled = loyaltyConfig?.loyaltyEnabled

  const runSearch = useCallback(async (q) => {
    if (!q?.trim()) {
      setResults(null)
      setShowCreate(false)
      return
    }
    const data = await posApi.searchCustomers(q.trim())
    setResults(data)
    setShowCreate(data.length === 0)
  }, [])

  useEffect(() => {
    clearTimeout(timerRef.current)
    if (!search.trim()) {
      setResults(null)
      setShowCreate(false)
      return
    }
    timerRef.current = setTimeout(() => runSearch(search).catch(() => {}), 300)
  }, [search, runSearch])

  const assignCustomer = async (c) => {
    if (!sale?.id) return
    setLoading(true)
    try {
      const updated = await posApi.assignCustomer(sale.id, c.id)
      onSaleUpdate(updated)
      setSearch('')
      setResults(null)
      notify.success(`Client ${c.fullName || c.name} associé`)
    } catch (e) {
      notify.error(getErrorMessage(e))
    } finally {
      setLoading(false)
    }
  }

  const removeCustomer = async () => {
    if (!sale?.id) return
    setLoading(true)
    try {
      const updated = await posApi.removeCustomer(sale.id)
      onSaleUpdate(updated)
      notify.success('Client retiré')
    } catch (e) {
      notify.error(getErrorMessage(e))
    } finally {
      setLoading(false)
    }
  }

  const quickCreate = async () => {
    if (!quickForm.lastName.trim()) {
      notify.error('Nom obligatoire')
      return
    }
    setLoading(true)
    try {
      const created = await posApi.quickCreateCustomer(quickForm)
      const updated = await posApi.assignCustomer(sale.id, created.id)
      onSaleUpdate(updated)
      setShowCreate(false)
      setQuickForm({ lastName: '', firstName: '', phone: '' })
      setSearch('')
      notify.success('Client créé et associé')
    } catch (e) {
      notify.error(getErrorMessage(e))
    } finally {
      setLoading(false)
    }
  }

  const applyRedeem = async () => {
    const pts = Number(redeemPoints)
    if (!pts || pts <= 0) return
    setLoading(true)
    try {
      const updated = await posApi.redeemLoyalty(sale.id, pts)
      onSaleUpdate(updated)
      setRedeemPoints('')
      notify.success('Remise fidélité appliquée')
    } catch (e) {
      notify.error(getErrorMessage(e))
    } finally {
      setLoading(false)
    }
  }

  const clearRedeem = async () => {
    setLoading(true)
    try {
      const updated = await posApi.clearLoyaltyRedemption(sale.id)
      onSaleUpdate(updated)
      notify.success('Remise fidélité annulée')
    } catch (e) {
      notify.error(getErrorMessage(e))
    } finally {
      setLoading(false)
    }
  }

  if (!hasPermission('customer.read')) return null

  return (
    <div className="p-3 border-b border-slate-800 text-sm space-y-2">
      <div className="flex items-center justify-between">
        <h3 className="font-semibold text-xs uppercase tracking-wide text-slate-400">Client</h3>
        {customer && (
          <button type="button" onClick={removeCustomer} disabled={loading}
            className="text-xs text-red-400 hover:text-red-300">
            Retirer
          </button>
        )}
      </div>

      {!customer ? (
        <>
          <input
            ref={searchRef}
            type="search"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Rechercher un client (nom, téléphone, email…)"
            className="w-full rounded-lg px-3 py-2 text-sm border border-slate-600"
            disabled={!sale?.id || loading}
            autoComplete="off"
          />
          <SearchCriteriaHelp entityType="customer" variant="pos" mode="inline" />
          {results?.length > 0 && (
            <ul className="bg-slate-800 rounded-lg border border-slate-700 max-h-32 overflow-auto">
              {results.map((c) => {
                const match = search.trim() ? findEntityMatch(search, c, 'customer') : null
                return (
                <li key={c.id}>
                  <button type="button" disabled={loading}
                    onClick={() => assignCustomer(c)}
                    className="w-full text-left px-3 py-2 hover:bg-slate-700 text-xs">
                    <span className="font-medium">{c.fullName}</span>
                    {c.phone && <span className="text-slate-400 ml-2">{c.phone}</span>}
                    <SearchMatchHint match={match} variant="pos" />
                    {loyaltyEnabled && (
                      <span className="block text-emerald-400">{c.loyaltyPoints} pts · {c.loyaltyTier}</span>
                    )}
                  </button>
                </li>
                )
              })}
            </ul>
          )}
          {showCreate && hasPermission('customer.create') && (
            <div className="bg-slate-800/80 rounded-lg p-3 space-y-2 border border-slate-700">
              <p className="text-xs text-slate-400">Aucun client — création rapide</p>
              <input placeholder="Nom *" value={quickForm.lastName}
                onChange={(e) => setQuickForm({ ...quickForm, lastName: e.target.value })}
                className="w-full rounded px-2 py-1.5 text-sm border border-slate-600" />
              <input placeholder="Prénom" value={quickForm.firstName}
                onChange={(e) => setQuickForm({ ...quickForm, firstName: e.target.value })}
                className="w-full rounded px-2 py-1.5 text-sm border border-slate-600" />
              <input placeholder="Téléphone" value={quickForm.phone}
                onChange={(e) => setQuickForm({ ...quickForm, phone: e.target.value })}
                className="w-full rounded px-2 py-1.5 text-sm border border-slate-600" />
              <button type="button" onClick={quickCreate} disabled={loading}
                className="w-full py-1.5 bg-emerald-600 hover:bg-emerald-500 rounded text-xs font-medium">
                Créer et associer
              </button>
            </div>
          )}
        </>
      ) : (
        <div className="space-y-1">
          <p className="font-medium">{customer.name}</p>
          {customer.phone && <p className="text-slate-400 text-xs">{customer.phone}</p>}
          {loyaltyEnabled && (
            <>
              <p className="text-emerald-400 text-xs">
                {customer.points ?? 0} points · {customer.tier || 'BRONZE'}
              </p>
              {hasPermission('loyalty.redeem') && loyaltyConfig?.allowPointsRedemption && sale?.status === 'DRAFT' && (
                <div className="flex gap-1 mt-2">
                  <input type="number" placeholder="Points" value={redeemPoints}
                    onChange={(e) => setRedeemPoints(e.target.value)}
                    className="flex-1 rounded px-2 py-1 text-xs border border-slate-600" />
                  <button type="button" onClick={applyRedeem} disabled={loading}
                    className="px-2 py-1 bg-amber-700 hover:bg-amber-600 rounded text-xs">Utiliser</button>
                </div>
              )}
              {sale?.loyaltyPointsRedeemed > 0 && (
                <div className="flex items-center justify-between text-xs text-amber-400 mt-1">
                  <span>-{sale.loyaltyPointsRedeemed} pts ({formatMoney(sale.loyaltyDiscountAmount, currency)})</span>
                  <button type="button" onClick={clearRedeem} className="underline">Annuler</button>
                </div>
              )}
            </>
          )}
        </div>
      )}
    </div>
  )
}
