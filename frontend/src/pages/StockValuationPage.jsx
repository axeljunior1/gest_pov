import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  Bar, BarChart, CartesianGrid, Cell, Legend, Line, LineChart,
  Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis,
} from 'recharts'
import { stockValuationApi, settingsApi } from '../api'
import { PageHeader, Card, Loading, Badge } from '../components/ui'
import { useNotification } from '../context/NotificationContext'
import { getErrorMessage } from '../utils/errors'

const CATEGORY_COLORS = ['#2563eb', '#059669', '#d97706', '#7c3aed', '#dc2626', '#0891b2', '#64748b']

function formatMoney(value, currency = 'EUR') {
  if (value == null) return '—'
  try {
    return new Intl.NumberFormat('fr-FR', { style: 'currency', currency }).format(Number(value))
  } catch {
    return `${Number(value).toFixed(2)} ${currency}`
  }
}

function formatNumber(value) {
  if (value == null) return '—'
  return Number(value).toLocaleString('fr-FR', { maximumFractionDigits: 4 })
}

function defaultFromDate() {
  const d = new Date()
  d.setMonth(d.getMonth() - 5)
  d.setDate(1)
  return d.toISOString().slice(0, 10)
}

function todayIso() {
  return new Date().toISOString().slice(0, 10)
}

export default function StockValuationPage() {
  const notify = useNotification()
  const [loading, setLoading] = useState(true)
  const [currency, setCurrency] = useState('EUR')
  const [from, setFrom] = useState(defaultFromDate)
  const [to, setTo] = useState(todayIso)
  const [granularity, setGranularity] = useState('day')
  const [inactiveDays, setInactiveDays] = useState(90)

  const [overview, setOverview] = useState(null)
  const [history, setHistory] = useState([])
  const [products, setProducts] = useState([])
  const [topProducts, setTopProducts] = useState([])
  const [staleProducts, setStaleProducts] = useState([])

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const [pub, ov, hist, byProduct, top, stale] = await Promise.all([
        settingsApi.getPublic(),
        stockValuationApi.overview(),
        stockValuationApi.history({ from, to, granularity }),
        stockValuationApi.byProduct(),
        stockValuationApi.topProducts(10),
        stockValuationApi.stale({ inactiveDays, limit: 15 }),
      ])
      setCurrency(pub?.currency || 'EUR')
      setOverview(ov)
      setHistory(hist)
      setProducts(byProduct)
      setTopProducts(top)
      setStaleProducts(stale)
    } catch (e) {
      notify.error(getErrorMessage(e))
    } finally {
      setLoading(false)
    }
  }, [from, to, granularity, inactiveDays, notify])

  useEffect(() => { load() }, [load])

  const categoryChart = useMemo(
    () => (overview?.byCategory || []).map((row) => ({
      name: row.categoryName,
      value: Number(row.stockValue || 0),
    })),
    [overview],
  )

  const historyChart = useMemo(
    () => history.map((point) => ({
      period: point.period,
      value: Number(point.value || 0),
    })),
    [history],
  )

  const topChart = useMemo(
    () => topProducts.map((row) => ({
      name: row.variantLabel ? `${row.productName} (${row.variantLabel})` : row.productName,
      value: Number(row.stockValue || 0),
    })),
    [topProducts],
  )

  if (loading && !overview) return <Loading />

  return (
    <div>
      <PageHeader
        title="Valorisation du stock (CMP)"
        subtitle="Coût moyen pondéré — valeur immobilisée et historique"
      />

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4 mb-6">
        <Card className="p-5 lg:col-span-1">
          <p className="text-xs font-medium text-gray-500 uppercase tracking-wide">Valeur totale du stock</p>
          <p className="text-3xl font-semibold mt-2 text-gray-900">
            {formatMoney(overview?.totalStockValue, currency)}
          </p>
          <Badge tone="default" className="mt-2">Coût moyen pondéré</Badge>
        </Card>

        <Card className="p-5 lg:col-span-2">
          <div className="flex flex-wrap gap-3 items-end">
            <label className="text-sm">
              <span className="block text-gray-500 mb-1">Du</span>
              <input type="date" value={from} onChange={(e) => setFrom(e.target.value)} className="input" />
            </label>
            <label className="text-sm">
              <span className="block text-gray-500 mb-1">Au</span>
              <input type="date" value={to} onChange={(e) => setTo(e.target.value)} className="input" />
            </label>
            <label className="text-sm">
              <span className="block text-gray-500 mb-1">Granularité</span>
              <select value={granularity} onChange={(e) => setGranularity(e.target.value)} className="input">
                <option value="day">Jour</option>
                <option value="month">Mois</option>
              </select>
            </label>
          </div>
        </Card>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
        <Card className="p-6">
          <h3 className="text-sm font-semibold text-gray-900 mb-4">Évolution de la valeur du stock</h3>
          {historyChart.length === 0 ? (
            <p className="text-sm text-gray-500">Aucun mouvement valorisé sur la période.</p>
          ) : (
            <ResponsiveContainer width="100%" height={280}>
              <LineChart data={historyChart}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="period" tick={{ fontSize: 11 }} />
                <YAxis tickFormatter={(v) => formatMoney(v, currency)} width={90} tick={{ fontSize: 11 }} />
                <Tooltip formatter={(v) => formatMoney(v, currency)} />
                <Line type="monotone" dataKey="value" stroke="#2563eb" strokeWidth={2} dot={false} name="Valeur" />
              </LineChart>
            </ResponsiveContainer>
          )}
        </Card>

        <Card className="p-6">
          <h3 className="text-sm font-semibold text-gray-900 mb-4">Répartition par catégorie</h3>
          {categoryChart.length === 0 ? (
            <p className="text-sm text-gray-500">Aucune valorisation par catégorie.</p>
          ) : (
            <ResponsiveContainer width="100%" height={280}>
              <PieChart>
                <Pie data={categoryChart} dataKey="value" nameKey="name" cx="50%" cy="50%" outerRadius={95} label>
                  {categoryChart.map((_, i) => (
                    <Cell key={i} fill={CATEGORY_COLORS[i % CATEGORY_COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip formatter={(v) => formatMoney(v, currency)} />
                <Legend />
              </PieChart>
            </ResponsiveContainer>
          )}
        </Card>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
        <Card className="p-6">
          <h3 className="text-sm font-semibold text-gray-900 mb-4">Top produits par valeur immobilisée</h3>
          {topChart.length === 0 ? (
            <p className="text-sm text-gray-500">Aucun produit valorisé.</p>
          ) : (
            <ResponsiveContainer width="100%" height={320}>
              <BarChart data={topChart} layout="vertical" margin={{ left: 20 }}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis type="number" tickFormatter={(v) => formatMoney(v, currency)} />
                <YAxis type="category" dataKey="name" width={140} tick={{ fontSize: 11 }} />
                <Tooltip formatter={(v) => formatMoney(v, currency)} />
                <Bar dataKey="value" fill="#059669" name="Valeur" />
              </BarChart>
            </ResponsiveContainer>
          )}
        </Card>

        <Card className="p-6">
          <div className="flex items-center justify-between mb-4 gap-3">
            <h3 className="text-sm font-semibold text-gray-900">Produits sans mouvement récent</h3>
            <select
              value={inactiveDays}
              onChange={(e) => setInactiveDays(Number(e.target.value))}
              className="input text-sm"
            >
              <option value={30}>30 jours</option>
              <option value={60}>60 jours</option>
              <option value={90}>90 jours</option>
              <option value={180}>180 jours</option>
            </select>
          </div>
          {staleProducts.length === 0 ? (
            <p className="text-sm text-gray-500">Aucun produit immobilisé sans mouvement.</p>
          ) : (
            <div className="overflow-auto max-h-80">
              <table className="w-full text-sm">
                <thead>
                  <tr className="text-left text-gray-500 border-b">
                    <th className="pb-2 font-medium">Produit</th>
                    <th className="pb-2 font-medium text-right">Qté</th>
                    <th className="pb-2 font-medium text-right">Valeur</th>
                  </tr>
                </thead>
                <tbody>
                  {staleProducts.map((row) => (
                    <tr key={`${row.productId}-${row.variantId ?? 0}`} className="border-b border-gray-50">
                      <td className="py-2">{row.productName}</td>
                      <td className="py-2 text-right">{formatNumber(row.quantityOnHand)}</td>
                      <td className="py-2 text-right">{formatMoney(row.stockValue, currency)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </Card>
      </div>

      <Card className="p-6">
        <h3 className="text-sm font-semibold text-gray-900 mb-4">CMP par produit / variante</h3>
        {products.length === 0 ? (
          <p className="text-sm text-gray-500">Aucune valorisation enregistrée.</p>
        ) : (
          <div className="overflow-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-gray-500 border-b">
                  <th className="pb-2 font-medium">Produit</th>
                  <th className="pb-2 font-medium">Variante</th>
                  <th className="pb-2 font-medium text-right">Quantité</th>
                  <th className="pb-2 font-medium text-right">CMP</th>
                  <th className="pb-2 font-medium text-right">Valeur stock</th>
                </tr>
              </thead>
              <tbody>
                {products.map((row) => (
                  <tr key={`${row.productId}-${row.variantId ?? 0}`} className="border-b border-gray-50">
                    <td className="py-2">{row.productName}</td>
                    <td className="py-2 text-gray-600">{row.variantLabel || '—'}</td>
                    <td className="py-2 text-right">{formatNumber(row.quantityOnHand)}</td>
                    <td className="py-2 text-right">{formatMoney(row.averageUnitCost, currency)}</td>
                    <td className="py-2 text-right font-medium">{formatMoney(row.stockValue, currency)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>
    </div>
  )
}
