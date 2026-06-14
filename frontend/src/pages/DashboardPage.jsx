import { useEffect, useState } from 'react'
import { dashboardApi, settingsApi } from '../api'
import { PageHeader, Card, Loading, Badge, EmptyState } from '../components/ui'
import { useNotification } from '../context/NotificationContext'
import { getErrorMessage } from '../utils/errors'

function KpiCard({ label, value, tone, hint }) {
  return (
    <Card className="p-5">
      <p className="text-xs font-medium text-gray-500 uppercase tracking-wide">{label}</p>
      <p className={`text-2xl font-semibold mt-2 ${tone === 'danger' ? 'text-red-600' : tone === 'warning' ? 'text-amber-600' : 'text-gray-900'}`}>
        {value}
      </p>
      {hint && <p className="text-xs text-gray-400 mt-1">{hint}</p>}
    </Card>
  )
}

function formatNumber(value) {
  if (value == null) return '—'
  const n = Number(value)
  if (Number.isNaN(n)) return value
  return n.toLocaleString('fr-FR', { maximumFractionDigits: 2 })
}

function formatCurrency(value, currency = 'EUR') {
  if (value == null) return '—'
  try {
    return new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency: currency || 'EUR',
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(Number(value))
  } catch {
    return `${Number(value).toLocaleString('fr-FR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} ${currency || 'EUR'}`
  }
}

function formatDate(value, dateFormat) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  if (dateFormat === 'dd/MM/yyyy') {
    return date.toLocaleDateString('fr-FR')
  }
  if (dateFormat === 'yyyy-MM-dd') {
    return date.toISOString().slice(0, 10)
  }
  return date.toLocaleDateString('fr-FR')
}

export default function DashboardPage() {
  const notify = useNotification()
  const [loading, setLoading] = useState(true)
  const [summary, setSummary] = useState(null)
  const [alerts, setAlerts] = useState(null)
  const [movements, setMovements] = useState([])
  const [entries, setEntries] = useState([])
  const [exits, setExits] = useState([])
  const [topProducts, setTopProducts] = useState([])
  const [warehouses, setWarehouses] = useState([])
  const [publicSettings, setPublicSettings] = useState({ currency: 'EUR', dateFormat: 'dd/MM/yyyy' })

  useEffect(() => {
    settingsApi.getPublic()
      .then((s) => setPublicSettings((prev) => ({ ...prev, ...s })))
      .catch(() => {})
  }, [])

  useEffect(() => {
    const load = async () => {
      setLoading(true)
      try {
        const [
          summaryData,
          alertsData,
          movementsData,
          entriesData,
          exitsData,
          topData,
          warehousesData,
        ] = await Promise.all([
          dashboardApi.summary(),
          dashboardApi.alerts(),
          dashboardApi.recentMovements({ limit: 8 }),
          dashboardApi.recentEntries({ limit: 5 }),
          dashboardApi.recentExits({ limit: 5 }),
          dashboardApi.topMovedProducts({ limit: 5 }),
          dashboardApi.warehouses(),
        ])
        setSummary(summaryData)
        setAlerts(alertsData)
        setMovements(movementsData)
        setEntries(entriesData)
        setExits(exitsData)
        setTopProducts(topData)
        setWarehouses(warehousesData)
      } catch (e) {
        notify.error(getErrorMessage(e))
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [notify])

  if (loading) return <Loading />

  return (
    <div>
      <PageHeader
        title="Tableau de bord"
        subtitle="Vue d'ensemble de l'état du stock"
      />

      <div className="grid grid-cols-2 lg:grid-cols-5 gap-4 mb-6">
        <KpiCard label="Produits actifs" value={summary?.totalProducts ?? 0} />
        <KpiCard label="Quantité totale" value={formatNumber(summary?.totalStockQuantity)} />
        <KpiCard
          label="Valeur du stock"
          value={formatCurrency(summary?.stockValue, publicSettings.currency)}
          hint={summary?.stockValuationMethod === 'SALE_PRICE' ? 'Valorisation au PV' : 'Valorisation au PA'}
        />
        <KpiCard label="Ruptures" value={summary?.outOfStockProducts ?? 0} tone="danger" />
        <KpiCard label="Stock faible" value={summary?.lowStockProducts ?? 0} tone="warning" />
      </div>

      <div className="grid grid-cols-2 lg:grid-cols-5 gap-4 mb-8">
        <KpiCard label="Alertes ouvertes" value={alerts?.openAlerts ?? 0} tone="warning" />
        <KpiCard label="Alertes rupture" value={alerts?.openOutOfStock ?? 0} tone="danger" />
        <KpiCard label="Alertes stock faible" value={alerts?.openLowStock ?? 0} tone="warning" />
        <KpiCard label="Péremption proche" value={alerts?.openExpirySoon ?? 0} />
        <KpiCard label="Produits expirés" value={alerts?.openExpired ?? 0} tone="danger" />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
        <Card className="p-6">
          <h3 className="text-sm font-semibold text-gray-900 mb-4">Stock par entrepôt</h3>
          {warehouses.length === 0 ? (
            <EmptyState message="Aucun stock en entrepôt." />
          ) : (
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-gray-500 border-b">
                  <th className="pb-2 font-medium">Entrepôt</th>
                  <th className="pb-2 font-medium text-right">Quantité</th>
                  <th className="pb-2 font-medium text-right">Valeur</th>
                </tr>
              </thead>
              <tbody>
                {warehouses.map((w) => (
                  <tr key={w.warehouseId} className="border-b border-gray-50">
                    <td className="py-2.5">
                      <span className="font-medium">{w.warehouseCode}</span>
                      <span className="text-gray-400 ml-2">{w.warehouseNom}</span>
                    </td>
                    <td className="py-2.5 text-right">{formatNumber(w.totalQuantity)}</td>
                    <td className="py-2.5 text-right">{formatCurrency(w.stockValue, publicSettings.currency)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </Card>

        <Card className="p-6">
          <h3 className="text-sm font-semibold text-gray-900 mb-4">Produits les plus mouvementés</h3>
          {topProducts.length === 0 ? (
            <EmptyState message="Aucun mouvement enregistré." />
          ) : (
            <ul className="divide-y divide-gray-100">
              {topProducts.map((p) => (
                <li key={p.productId} className="py-2.5 flex justify-between gap-4">
                  <span className="text-sm font-medium text-gray-900">{p.productNom}</span>
                  <span className="text-xs text-gray-500 shrink-0">
                    {p.movementCount} mvts · {formatNumber(p.totalQuantityMoved)} u.
                  </span>
                </li>
              ))}
            </ul>
          )}
        </Card>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <Card className="p-6">
          <h3 className="text-sm font-semibold text-gray-900 mb-4">Mouvements récents</h3>
          {movements.length === 0 ? (
            <EmptyState message="Aucun mouvement." />
          ) : (
            <ul className="divide-y divide-gray-100">
              {movements.map((m) => (
                <li key={m.id} className="py-2.5">
                  <div className="flex items-center gap-2 mb-0.5">
                    <Badge tone={m.movementType === 'IN' ? 'success' : m.movementType === 'OUT' ? 'danger' : 'default'}>
                      {m.movementType}
                    </Badge>
                    <span className="text-sm font-medium truncate">{m.productNom}</span>
                  </div>
                  <p className="text-xs text-gray-400">
                    {formatNumber(m.quantity)} · {formatDate(m.movementDate, publicSettings.dateFormat)}
                  </p>
                </li>
              ))}
            </ul>
          )}
        </Card>

        <Card className="p-6">
          <h3 className="text-sm font-semibold text-gray-900 mb-4">Entrées récentes</h3>
          {entries.length === 0 ? (
            <EmptyState message="Aucune entrée validée." />
          ) : (
            <ul className="divide-y divide-gray-100">
              {entries.map((e) => (
                <li key={e.id} className="py-2.5">
                  <p className="text-sm font-medium">{e.entryNumber}</p>
                  <p className="text-xs text-gray-400">
                    {e.warehouseCode} · {e.lineCount} ligne(s)
                    {e.validatedAt && ` · ${formatDate(e.validatedAt, publicSettings.dateFormat)}`}
                  </p>
                </li>
              ))}
            </ul>
          )}
        </Card>

        <Card className="p-6">
          <h3 className="text-sm font-semibold text-gray-900 mb-4">Sorties récentes</h3>
          {exits.length === 0 ? (
            <EmptyState message="Aucune sortie validée." />
          ) : (
            <ul className="divide-y divide-gray-100">
              {exits.map((e) => (
                <li key={e.id} className="py-2.5">
                  <p className="text-sm font-medium">{e.exitNumber}</p>
                  <p className="text-xs text-gray-400">
                    {e.warehouseCode} · {e.reason} · {e.lineCount} ligne(s)
                  </p>
                </li>
              ))}
            </ul>
          )}
        </Card>
      </div>
    </div>
  )
}
