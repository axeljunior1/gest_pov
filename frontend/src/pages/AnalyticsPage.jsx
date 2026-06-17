import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  Bar, BarChart, CartesianGrid, Cell, Legend, Line, LineChart,
  Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis,
} from 'recharts'
import { analyticsApi, settingsApi } from '../api'
import { PageHeader, Card, Loading, Badge } from '../components/ui'
import { useNotification } from '../context/NotificationContext'
import { useAuth } from '../context/AuthContext'
import { getErrorMessage } from '../utils/errors'

const PERIODS = [
  { value: 'TODAY', label: "Aujourd'hui" },
  { value: 'YESTERDAY', label: 'Hier' },
  { value: 'THIS_WEEK', label: 'Cette semaine' },
  { value: 'THIS_MONTH', label: 'Ce mois' },
  { value: 'LAST_MONTH', label: 'Mois précédent' },
  { value: 'THIS_YEAR', label: 'Année en cours' },
]

const PAYMENT_COLORS = ['#059669', '#2563eb', '#d97706', '#7c3aed', '#64748b']

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
  return Number(value).toLocaleString('fr-FR', { maximumFractionDigits: 2 })
}

function TrendBadge({ metric }) {
  if (!metric) return null
  const pct = Number(metric.changePercent ?? 0)
  const tone = metric.trend === 'up' ? 'success' : metric.trend === 'down' ? 'danger' : 'default'
  const sign = pct > 0 ? '+' : ''
  return (
    <Badge tone={tone} className="mt-1 text-[10px]">
      {sign}{pct.toFixed(1)}% vs période préc.
    </Badge>
  )
}

function KpiCard({ label, metric, format = formatNumber, currency }) {
  return (
    <Card className="p-4">
      <p className="text-xs font-medium text-gray-500 uppercase tracking-wide">{label}</p>
      <p className="text-2xl font-semibold mt-1 text-gray-900">
        {format === formatMoney ? formatMoney(metric?.current, currency) : format(metric?.current)}
      </p>
      <TrendBadge metric={metric} />
    </Card>
  )
}

export default function AnalyticsPage() {
  const notify = useNotification()
  const { hasPermission } = useAuth()
  const [loading, setLoading] = useState(true)
  const [period, setPeriod] = useState('THIS_MONTH')
  const [granularity, setGranularity] = useState('DAY')
  const [currency, setCurrency] = useState('EUR')
  const [companyName, setCompanyName] = useState('')

  const [overview, setOverview] = useState(null)
  const [timeline, setTimeline] = useState(null)
  const [products, setProducts] = useState(null)
  const [categories, setCategories] = useState(null)
  const [payments, setPayments] = useState(null)
  const [cashiers, setCashiers] = useState(null)
  const [customers, setCustomers] = useState(null)
  const [stockAlerts, setStockAlerts] = useState(null)
  const [businessAlerts, setBusinessAlerts] = useState(null)

  const filters = useMemo(() => ({ period, granularity }), [period, granularity])

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const requests = [
        analyticsApi.overview(filters),
        analyticsApi.timeline(filters),
        analyticsApi.topProducts({ ...filters, size: 10 }),
        analyticsApi.categories(filters),
        analyticsApi.payments(filters),
        analyticsApi.stockAlerts(filters),
        analyticsApi.businessAlerts(filters),
      ]
      if (hasPermission('analytics.cashier.read') || hasPermission('analytics.read')) {
        requests.push(analyticsApi.cashiers(filters))
      }
      if (hasPermission('analytics.read') || hasPermission('analytics.sales.read')) {
        requests.push(analyticsApi.customers(filters))
      }

      const results = await Promise.all(requests)
      let i = 0
      setOverview(results[i++])
      setTimeline(results[i++])
      setProducts(results[i++])
      setCategories(results[i++])
      setPayments(results[i++])
      setStockAlerts(results[i++])
      setBusinessAlerts(results[i++])
      setCashiers(hasPermission('analytics.cashier.read') || hasPermission('analytics.read') ? results[i++] : null)
      setCustomers(hasPermission('analytics.read') || hasPermission('analytics.sales.read') ? results[i++] : null)
    } catch (e) {
      notify.error(getErrorMessage(e, { module: 'analytics' }))
    } finally {
      setLoading(false)
    }
  }, [filters, hasPermission, notify])

  useEffect(() => {
    settingsApi.getPublic()
      .then((s) => {
        setCurrency(s.currency || 'EUR')
        setCompanyName(s.companyName || '')
      })
      .catch(() => {})
  }, [])

  useEffect(() => { load() }, [load])

  const handleExport = async (type) => {
    try {
      const blob = await analyticsApi.exportCsv({ ...filters, type })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `analytics-${type}.csv`
      a.click()
      URL.revokeObjectURL(url)
    } catch (e) {
      notify.error(getErrorMessage(e, { module: 'analytics' }))
    }
  }

  if (loading && !overview) return <Loading />

  const paymentChart = payments?.methods?.map((m) => ({
    name: m.methodLabel,
    value: Number(m.total),
  })) ?? []

  return (
    <div className="space-y-6 pb-8">
      <PageHeader
        title="Analytics POS & Stock"
        subtitle={companyName
          ? `${companyName} · ${PERIODS.find((p) => p.value === period)?.label || period} · ${currency}`
          : 'Pilotage des ventes, paiements, produits et alertes'}
        action={hasPermission('analytics.export') && (
          <div className="flex gap-2">
            <button type="button" onClick={() => handleExport('products')} className="px-3 py-1.5 text-sm bg-white border border-gray-300 rounded-lg hover:bg-gray-50">
              Export produits
            </button>
            <button type="button" onClick={() => handleExport('payments')} className="px-3 py-1.5 text-sm bg-white border border-gray-300 rounded-lg hover:bg-gray-50">
              Export paiements
            </button>
          </div>
        )}
      />

      <Card className="p-4 flex flex-wrap gap-3 items-end">
        <div>
          <label className="text-xs text-gray-500 block mb-1">Période</label>
          <select value={period} onChange={(e) => setPeriod(e.target.value)} className="rounded-lg border border-gray-300 px-3 py-2 text-sm">
            {PERIODS.map((p) => <option key={p.value} value={p.value}>{p.label}</option>)}
          </select>
        </div>
        <div>
          <label className="text-xs text-gray-500 block mb-1">Granularité graphique</label>
          <select value={granularity} onChange={(e) => setGranularity(e.target.value)} className="rounded-lg border border-gray-300 px-3 py-2 text-sm">
            <option value="HOUR">Par heure</option>
            <option value="DAY">Par jour</option>
            <option value="MONTH">Par mois</option>
          </select>
        </div>
        <button type="button" onClick={load} className="px-4 py-2 bg-emerald-600 text-white rounded-lg text-sm hover:bg-emerald-500">
          Actualiser
        </button>
      </Card>

      {overview && (
        <div className="grid grid-cols-2 lg:grid-cols-4 xl:grid-cols-5 gap-3">
          <KpiCard label="CA aujourd'hui" metric={overview.revenueToday} format={formatMoney} currency={currency} />
          <KpiCard label="CA semaine" metric={overview.revenueWeek} format={formatMoney} currency={currency} />
          <KpiCard label="CA mois" metric={overview.revenueMonth} format={formatMoney} currency={currency} />
          <KpiCard label="Ventes du jour" metric={overview.salesCountToday} />
          <KpiCard label="Panier moyen" metric={overview.averageBasketToday} format={formatMoney} currency={currency} />
          <KpiCard label="Articles vendus" metric={overview.itemsSoldToday} />
          <Card className="p-4">
            <p className="text-xs font-medium text-gray-500 uppercase">Remboursements</p>
            <p className="text-2xl font-semibold mt-1 text-red-600">{formatMoney(overview.refundsTotal, currency)}</p>
            <TrendBadge metric={overview.refundsPeriod} />
          </Card>
          <Card className="p-4">
            <p className="text-xs font-medium text-gray-500 uppercase">Remises</p>
            <p className="text-2xl font-semibold mt-1 text-amber-600">{formatMoney(overview.discountsTotal, currency)}</p>
            <TrendBadge metric={overview.discountsPeriod} />
          </Card>
          <Card className="p-4">
            <p className="text-xs font-medium text-gray-500 uppercase">Marge brute est.</p>
            <p className="text-2xl font-semibold mt-1 text-emerald-700">{formatMoney(overview.grossProfitEstimate, currency)}</p>
          </Card>
          {(hasPermission('sales.cancellations.read') || hasPermission('analytics.read')) && (
            <Link to="/analytics/cancellations" className="block">
              <Card className="p-4 hover:border-red-300 hover:shadow-sm transition-all h-full">
                <p className="text-xs font-medium text-gray-500 uppercase">Ventes annulées</p>
                <p className="text-2xl font-semibold mt-1 text-red-600">
                  {overview.cancelledCountPeriod?.current ?? 0}
                </p>
                <p className="text-xs text-gray-500 mt-1">
                  {formatMoney(overview.cancelledAmountTotal, currency)} annulés
                </p>
                <TrendBadge metric={overview.cancelledCountPeriod} />
                <p className="text-xs text-emerald-600 mt-2">Voir le détail →</p>
              </Card>
            </Link>
          )}
        </div>
      )}

      <div className="grid lg:grid-cols-3 gap-4">
        <Card className="p-4 lg:col-span-2">
          <h3 className="font-semibold mb-4">Évolution du chiffre d'affaires</h3>
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={timeline?.points ?? []}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="label" tick={{ fontSize: 11 }} />
                <YAxis tick={{ fontSize: 11 }} />
                <Tooltip formatter={(v) => formatMoney(v, currency)} />
                <Line type="monotone" dataKey="revenue" stroke="#059669" strokeWidth={2} dot={false} name="CA" />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </Card>

        <Card className="p-4">
          <h3 className="font-semibold mb-4">Répartition paiements</h3>
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie data={paymentChart} dataKey="value" nameKey="name" cx="50%" cy="50%" outerRadius={80} label>
                  {paymentChart.map((_, idx) => (
                    <Cell key={idx} fill={PAYMENT_COLORS[idx % PAYMENT_COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip formatter={(v) => formatMoney(v, currency)} />
                <Legend />
              </PieChart>
            </ResponsiveContainer>
          </div>
        </Card>
      </div>

      <div className="grid lg:grid-cols-2 gap-4">
        <Card className="p-4">
          <h3 className="font-semibold mb-3">Top 10 produits (CA)</h3>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-gray-500 border-b">
                  <th className="py-2">Produit</th>
                  <th className="py-2">Qté</th>
                  <th className="py-2">CA</th>
                  <th className="py-2">Stock</th>
                </tr>
              </thead>
              <tbody>
                {(products?.items ?? []).map((p) => (
                  <tr key={p.productId} className="border-b border-gray-100">
                    <td className="py-2 pr-2">
                      <span className="font-medium">{p.productName}</span>
                      <span className="block text-xs text-gray-400">{p.sku}</span>
                    </td>
                    <td className="py-2">{formatNumber(p.quantitySold)}</td>
                    <td className="py-2">{formatMoney(p.revenue, currency)}</td>
                    <td className="py-2">
                      <span className={Number(p.stockRemaining) <= 0 ? 'text-red-600 font-medium' : ''}>
                        {formatNumber(p.stockRemaining)}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>

        <Card className="p-4">
          <h3 className="font-semibold mb-3">CA par catégorie</h3>
          <div className="h-56">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={categories?.items ?? []}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="categoryName" tick={{ fontSize: 10 }} interval={0} angle={-20} textAnchor="end" height={60} />
                <YAxis tick={{ fontSize: 11 }} />
                <Tooltip formatter={(v) => formatMoney(v, currency)} />
                <Bar dataKey="revenue" fill="#2563eb" name="CA" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </Card>
      </div>

      {cashiers?.items?.length > 0 && (
        <Card className="p-4">
          <h3 className="font-semibold mb-3">Performance caissiers</h3>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-gray-500 border-b">
                  <th className="py-2">Caissier</th>
                  <th className="py-2">Ventes</th>
                  <th className="py-2">CA</th>
                  <th className="py-2">Panier moy.</th>
                  <th className="py-2">Remises</th>
                  <th className="py-2">Annul.</th>
                </tr>
              </thead>
              <tbody>
                {cashiers.items.map((c) => (
                  <tr key={c.cashierId} className="border-b border-gray-100">
                    <td className="py-2 font-medium">{c.cashierName}</td>
                    <td className="py-2">{c.saleCount}</td>
                    <td className="py-2">{formatMoney(c.revenue, currency)}</td>
                    <td className="py-2">{formatMoney(c.averageBasket, currency)}</td>
                    <td className="py-2">{formatMoney(c.discountsGranted, currency)}</td>
                    <td className="py-2">{c.cancellations}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      )}

      {customers && (
        <div className="grid md:grid-cols-3 gap-4">
          <Card className="p-4">
            <p className="text-xs text-gray-500 uppercase">Nouveaux clients</p>
            <p className="text-3xl font-semibold mt-1">{customers.newCustomers}</p>
          </Card>
          <Card className="p-4">
            <p className="text-xs text-gray-500 uppercase">Clients récurrents</p>
            <p className="text-3xl font-semibold mt-1">{customers.returningCustomers}</p>
          </Card>
          <Card className="p-4">
            <p className="text-xs text-gray-500 uppercase">Points fidélité</p>
            <p className="text-sm mt-2"><span className="text-emerald-600 font-medium">+{customers.loyaltyPointsEarned}</span> distribués</p>
            <p className="text-sm"><span className="text-amber-600 font-medium">-{customers.loyaltyPointsRedeemed}</span> utilisés</p>
          </Card>
        </div>
      )}

      <div className="grid lg:grid-cols-2 gap-4">
        <Card className="p-4">
          <h3 className="font-semibold mb-3">Alertes stock</h3>
          <ul className="space-y-2 text-sm max-h-64 overflow-y-auto">
            {(stockAlerts?.items ?? []).length === 0 && <li className="text-gray-500">Aucune alerte</li>}
            {(stockAlerts?.items ?? []).map((a) => (
              <li key={`${a.productId}-${a.alertType}`} className="flex justify-between gap-2 border-b border-gray-100 pb-2">
                <span>
                  <span className="font-medium">{a.productName}</span>
                  <span className="block text-xs text-gray-400">{a.alertType}</span>
                </span>
                <Badge tone={a.severity === 'critical' ? 'danger' : a.severity === 'warning' ? 'warning' : 'default'}>
                  Stock {formatNumber(a.stockRemaining)}
                </Badge>
              </li>
            ))}
          </ul>
        </Card>

        <Card className="p-4">
          <h3 className="font-semibold mb-3">Alertes business</h3>
          <ul className="space-y-2 text-sm max-h-64 overflow-y-auto">
            {(businessAlerts?.items ?? []).length === 0 && <li className="text-gray-500">RAS</li>}
            {(businessAlerts?.items ?? []).map((a, i) => (
              <li key={`${a.code}-${i}`} className="border-b border-gray-100 pb-2">
                <div className="flex items-start justify-between gap-2">
                  <span className="font-medium">{a.title}</span>
                  <Badge tone={a.severity === 'critical' ? 'danger' : a.severity === 'warning' ? 'warning' : 'default'}>
                    {a.severity}
                  </Badge>
                </div>
                <p className="text-xs text-gray-500 mt-0.5">{a.message}</p>
              </li>
            ))}
          </ul>
        </Card>
      </div>

      {payments && (
        <Card className="p-4 grid sm:grid-cols-3 gap-4 text-sm">
          <div>
            <p className="text-gray-500">Cash attendu (sessions)</p>
            <p className="text-lg font-semibold">{formatMoney(payments.expectedCash, currency)}</p>
          </div>
          <div>
            <p className="text-gray-500">Cash déclaré</p>
            <p className="text-lg font-semibold">{formatMoney(payments.declaredCash, currency)}</p>
          </div>
          <div>
            <p className="text-gray-500">Écart caisse</p>
            <p className={`text-lg font-semibold ${Number(payments.cashDifference) !== 0 ? 'text-red-600' : 'text-emerald-600'}`}>
              {formatMoney(payments.cashDifference, currency)}
            </p>
          </div>
        </Card>
      )}
    </div>
  )
}
