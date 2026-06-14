import { useCallback, useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { saleCancellationsApi, settingsApi } from '../api'
import { PageHeader, Card, Loading, Badge, EmptyState } from '../components/ui'
import { useNotification } from '../context/NotificationContext'
import { useAuth } from '../context/AuthContext'
import { getErrorMessage } from '../utils/errors'
import { saleStatusLabel } from '../utils/saleStatus'

const PERIODS = [
  { value: 'TODAY', label: "Aujourd'hui" },
  { value: 'THIS_WEEK', label: 'Cette semaine' },
  { value: 'THIS_MONTH', label: 'Ce mois' },
  { value: 'LAST_MONTH', label: 'Mois précédent' },
]

function formatMoney(value, currency = 'EUR') {
  if (value == null) return '—'
  try {
    return new Intl.NumberFormat('fr-FR', { style: 'currency', currency }).format(Number(value))
  } catch {
    return `${Number(value).toFixed(2)} ${currency}`
  }
}

function formatDateTime(value) {
  if (!value) return '—'
  return new Date(value).toLocaleString('fr-FR')
}

export default function CancelledSalesPage() {
  const notify = useNotification()
  const { hasPermission } = useAuth()
  const [searchParams, setSearchParams] = useSearchParams()
  const [loading, setLoading] = useState(true)
  const [currency, setCurrency] = useState('EUR')
  const [sales, setSales] = useState([])
  const [analytics, setAnalytics] = useState(null)
  const [reasons, setReasons] = useState([])
  const [detail, setDetail] = useState(null)
  const [detailLoading, setDetailLoading] = useState(false)

  const [period, setPeriod] = useState(searchParams.get('period') || 'THIS_MONTH')
  const [reason, setReason] = useState(searchParams.get('reason') || '')
  const [amountMin, setAmountMin] = useState(searchParams.get('amountMin') || '')
  const [amountMax, setAmountMax] = useState(searchParams.get('amountMax') || '')

  const canAudit = hasPermission('sales.cancellations.audit')

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const params = { period, ...(reason ? { reason } : {}), ...(amountMin ? { amountMin } : {}), ...(amountMax ? { amountMax } : {}) }
      const [list, stats, reasonList] = await Promise.all([
        saleCancellationsApi.list(params),
        saleCancellationsApi.analytics({ period }),
        saleCancellationsApi.reasons(),
      ])
      setSales(list)
      setAnalytics(stats)
      setReasons(reasonList)
    } catch (e) {
      notify.error(getErrorMessage(e))
    } finally {
      setLoading(false)
    }
  }, [period, reason, amountMin, amountMax, notify])

  useEffect(() => {
    settingsApi.getPublic().then((s) => setCurrency(s.currency || 'EUR')).catch(() => {})
    saleCancellationsApi.reasons().then(setReasons).catch(() => {})
  }, [])

  useEffect(() => { load() }, [load])

  useEffect(() => {
    const params = { period }
    if (reason) params.reason = reason
    if (amountMin) params.amountMin = amountMin
    if (amountMax) params.amountMax = amountMax
    setSearchParams(params, { replace: true })
  }, [period, reason, amountMin, amountMax, setSearchParams])

  const openDetail = async (id) => {
    setDetailLoading(true)
    try {
      setDetail(await saleCancellationsApi.detail(id))
    } catch (e) {
      notify.error(getErrorMessage(e))
    } finally {
      setDetailLoading(false)
    }
  }

  if (loading && !sales.length) return <Loading />

  return (
    <div className="space-y-6 pb-8">
      <PageHeader
        title="Ventes annulées"
        subtitle="Audit et traçabilité des annulations POS"
        action={
          <Link to="/analytics" className="text-sm text-emerald-600 hover:underline">
            ← Retour Analytics
          </Link>
        }
      />

      {analytics && (
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
          <Card className="p-4">
            <p className="text-xs text-gray-500 uppercase">Nombre annulées</p>
            <p className="text-2xl font-semibold mt-1">{analytics.cancelledCount ?? 0}</p>
          </Card>
          <Card className="p-4">
            <p className="text-xs text-gray-500 uppercase">Montant total annulé</p>
            <p className="text-2xl font-semibold mt-1 text-red-600">
              {formatMoney(analytics.cancelledAmountTotal, currency)}
            </p>
          </Card>
          <Card className="p-4 lg:col-span-2">
            <p className="text-xs text-gray-500 uppercase mb-2">Top motifs</p>
            <div className="flex flex-wrap gap-2">
              {(analytics.topReasons ?? []).slice(0, 4).map((r) => (
                <Badge key={r.reason} tone="warning">{r.reasonLabel} ({r.count})</Badge>
              ))}
            </div>
          </Card>
        </div>
      )}

      {analytics && (analytics.topSellers?.length > 0 || analytics.topCashiers?.length > 0) && (
        <div className="grid md:grid-cols-2 gap-4">
          {analytics.topSellers?.length > 0 && (
            <Card className="p-4">
              <p className="text-xs text-gray-500 uppercase mb-2">Top vendeurs (annulations)</p>
              <ul className="text-sm space-y-1">
                {analytics.topSellers.map((a) => (
                  <li key={a.userId} className="flex justify-between">
                    <span>{a.userName}</span>
                    <span className="text-gray-500">{a.count} · {formatMoney(a.amount, currency)}</span>
                  </li>
                ))}
              </ul>
            </Card>
          )}
          {analytics.topCashiers?.length > 0 && (
            <Card className="p-4">
              <p className="text-xs text-gray-500 uppercase mb-2">Top caissiers (annulations)</p>
              <ul className="text-sm space-y-1">
                {analytics.topCashiers.map((a) => (
                  <li key={a.userId} className="flex justify-between">
                    <span>{a.userName}</span>
                    <span className="text-gray-500">{a.count} · {formatMoney(a.amount, currency)}</span>
                  </li>
                ))}
              </ul>
            </Card>
          )}
        </div>
      )}

      <Card className="p-4 flex flex-wrap gap-3 items-end">
        <div>
          <label className="text-xs text-gray-500 block mb-1">Période</label>
          <select value={period} onChange={(e) => setPeriod(e.target.value)} className="rounded-lg border px-3 py-2 text-sm">
            {PERIODS.map((p) => <option key={p.value} value={p.value}>{p.label}</option>)}
          </select>
        </div>
        <div>
          <label className="text-xs text-gray-500 block mb-1">Motif</label>
          <select value={reason} onChange={(e) => setReason(e.target.value)} className="rounded-lg border px-3 py-2 text-sm">
            <option value="">Tous</option>
            {reasons.map((r) => <option key={r.code} value={r.code}>{r.label}</option>)}
          </select>
        </div>
        <div>
          <label className="text-xs text-gray-500 block mb-1">Montant min</label>
          <input type="number" value={amountMin} onChange={(e) => setAmountMin(e.target.value)} className="rounded-lg border px-3 py-2 text-sm w-28" />
        </div>
        <div>
          <label className="text-xs text-gray-500 block mb-1">Montant max</label>
          <input type="number" value={amountMax} onChange={(e) => setAmountMax(e.target.value)} className="rounded-lg border px-3 py-2 text-sm w-28" />
        </div>
        <button type="button" onClick={load} className="px-4 py-2 bg-emerald-600 text-white rounded-lg text-sm">Filtrer</button>
      </Card>

      <Card className="p-0 overflow-hidden">
        {sales.length === 0 ? (
          <div className="p-8"><EmptyState message="Aucune vente annulée sur cette période." /></div>
        ) : (
          <table className="w-full text-sm">
            <thead className="bg-gray-50 text-left text-gray-500">
              <tr>
                <th className="px-4 py-3 font-medium">N° vente</th>
                <th className="px-4 py-3 font-medium">Création</th>
                <th className="px-4 py-3 font-medium">Annulation</th>
                <th className="px-4 py-3 font-medium">Vendeur</th>
                <th className="px-4 py-3 font-medium">Caissier</th>
                <th className="px-4 py-3 font-medium">Client</th>
                <th className="px-4 py-3 font-medium text-right">Montant</th>
                <th className="px-4 py-3 font-medium">Motif</th>
                <th className="px-4 py-3 font-medium">Statut</th>
              </tr>
            </thead>
            <tbody>
              {sales.map((s) => (
                <tr
                  key={s.id}
                  className="border-t border-gray-100 hover:bg-gray-50 cursor-pointer"
                  onClick={() => openDetail(s.id)}
                >
                  <td className="px-4 py-3 font-mono text-xs">{s.saleNumber}</td>
                  <td className="px-4 py-3">{formatDateTime(s.createdAt)}</td>
                  <td className="px-4 py-3">{formatDateTime(s.cancelledAt)}</td>
                  <td className="px-4 py-3">{s.sellerName}</td>
                  <td className="px-4 py-3">{s.cashierName}</td>
                  <td className="px-4 py-3">{s.customerName || '—'}</td>
                  <td className="px-4 py-3 text-right font-medium">{formatMoney(s.total, currency)}</td>
                  <td className="px-4 py-3">{s.cancellationReasonLabel || '—'}</td>
                  <td className="px-4 py-3">{saleStatusLabel(s.status)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </Card>

      {(detail || detailLoading) && (
        <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4" onMouseDown={(e) => e.target === e.currentTarget && setDetail(null)}>
          <div className="bg-white rounded-xl w-full max-w-3xl max-h-[90vh] overflow-auto shadow-xl" onMouseDown={(e) => e.stopPropagation()}>
            {detailLoading && !detail ? (
              <div className="p-8"><Loading /></div>
            ) : detail && (
              <div className="p-6 space-y-6">
                <div className="flex justify-between items-start">
                  <div>
                    <h2 className="text-xl font-semibold">{detail.saleNumber}</h2>
                    <p className="text-sm text-gray-500 mt-1">
                      Créée {formatDateTime(detail.createdAt)} · Annulée {formatDateTime(detail.cancelledAt)}
                    </p>
                  </div>
                  <button type="button" onClick={() => setDetail(null)} className="text-gray-400 hover:text-gray-700 text-2xl leading-none">×</button>
                </div>

                <div className="grid sm:grid-cols-2 gap-4 text-sm">
                  <div><span className="text-gray-500">Vendeur</span><p className="font-medium">{detail.sellerName}</p></div>
                  <div><span className="text-gray-500">Caissier</span><p className="font-medium">{detail.cashierName}</p></div>
                  <div><span className="text-gray-500">Client</span><p className="font-medium">{detail.customerName || '—'}</p></div>
                  <div><span className="text-gray-500">Motif</span><p className="font-medium">{detail.cancellationReasonLabel || '—'}</p></div>
                  {detail.cancellationComment && (
                    <div className="sm:col-span-2"><span className="text-gray-500">Commentaire</span><p>{detail.cancellationComment}</p></div>
                  )}
                </div>

                <div>
                  <h3 className="font-semibold mb-2">Produits</h3>
                  <table className="w-full text-sm">
                    <thead><tr className="text-gray-500 border-b"><th className="py-2 text-left">Produit</th><th className="py-2">Qté</th><th className="py-2 text-right">Prix</th><th className="py-2 text-right">Total</th></tr></thead>
                    <tbody>
                      {detail.lignes?.map((l) => (
                        <tr key={l.id} className="border-b border-gray-50">
                          <td className="py-2">
                            {l.productNom}
                            {l.variantNameSnapshot && <span className="text-gray-500 text-xs block">{l.variantNameSnapshot}</span>}
                            {l.packagingNameSnapshot && <span className="text-gray-400 text-xs block">{l.packagingNameSnapshot}</span>}
                          </td>
                          <td className="py-2 text-center">{l.quantityInput}</td>
                          <td className="py-2 text-right">{formatMoney(l.unitPrice, currency)}</td>
                          <td className="py-2 text-right">{formatMoney(l.lineTotal, currency)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                  <p className="text-right font-bold mt-2">Total {formatMoney(detail.total, currency)}</p>
                </div>

                {detail.paymentInfo && (
                  <div className="text-sm grid sm:grid-cols-2 gap-2 bg-gray-50 rounded-lg p-4">
                    <p>Paiement commencé : <strong>{detail.paymentInfo.paymentStarted ? 'Oui' : 'Non'}</strong></p>
                    <p>Paiement validé : <strong>{detail.paymentInfo.paymentValidated ? 'Oui' : 'Non'}</strong></p>
                    {detail.paymentInfo.paymentMethodLabel && <p>Mode : {detail.paymentInfo.paymentMethodLabel}</p>}
                    {detail.paymentInfo.paidAmount > 0 && <p>Montant encaissé : {formatMoney(detail.paymentInfo.paidAmount, currency)}</p>}
                  </div>
                )}

                <div>
                  <h3 className="font-semibold mb-2">Historique</h3>
                  <ul className="space-y-2 border-l-2 border-emerald-200 pl-4">
                    {detail.timeline?.map((ev) => (
                      <li key={ev.id || `${ev.eventType}-${ev.occurredAt}`} className="text-sm">
                        <span className="text-gray-500">{new Date(ev.occurredAt).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' })}</span>
                        {' '}{ev.description || ev.eventTypeLabel}
                        {ev.details && <span className="text-gray-400"> — {ev.details}</span>}
                        {ev.actorName && <span className="text-gray-400 text-xs block">{ev.actorName}</span>}
                      </li>
                    ))}
                  </ul>
                </div>

                {canAudit && detail.audit && (
                  <div className="text-sm bg-slate-50 rounded-lg p-4 space-y-1">
                    <h3 className="font-semibold mb-2">Audit</h3>
                    <p>Créé par {detail.audit.createdByName} — {formatDateTime(detail.audit.createdAt)}</p>
                    <p>Modifié par {detail.audit.updatedByName}</p>
                    <p>Annulé par {detail.audit.cancelledByName} — {formatDateTime(detail.audit.cancelledAt)}</p>
                  </div>
                )}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  )
}
