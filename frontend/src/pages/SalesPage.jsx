import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { salesBrowseApi, settingsApi } from '../api'
import { PageHeader, Card, Loading, Badge, EmptyState, Button } from '../components/ui'
import { useSalesBrowseFilters } from '../hooks/useSalesBrowseFilters'
import BrowseFiltersPanel from '../components/BrowseFiltersPanel'
import BrowsePagination from '../components/BrowsePagination'
import { useAuth } from '../context/AuthContext'
import { saleStatusLabel } from '../utils/saleStatus'
import { formatPosDateTime, formatPosMoney } from '../utils/posMoney'
import { SALE_STATUS_FILTER_OPTIONS, saleStatusTone } from '../utils/saleDisplay'

export default function SalesPage() {
  const { hasAnyPermission } = useAuth()
  const [currency, setCurrency] = useState('EUR')
  const canExport = hasAnyPermission('export.read', 'analytics.export', 'pos.report.read', 'analytics.sales.read')

  const {
    items: sales,
    loading,
    exporting,
    draft,
    updateDraft,
    applyFilters,
    resetFilters,
    goToPage,
    pagination,
    runExport,
  } = useSalesBrowseFilters({ fetchFn: salesBrowseApi.list })

  useEffect(() => {
    settingsApi.getPublic().then((s) => setCurrency(s.currency || 'EUR')).catch(() => {})
  }, [])

  if (loading && !sales.length) return <Loading />

  return (
    <div className="space-y-6 pb-8">
      <PageHeader
        title="Ventes"
        subtitle="Consultation des ventes POS et accès au détail"
        action={
          <Link to="/returns" className="text-sm text-emerald-600 hover:underline">
            Voir les retours →
          </Link>
        }
      />

      <Card className="p-4">
        <BrowseFiltersPanel
          draft={draft}
          updateDraft={updateDraft}
          onApply={applyFilters}
          onReset={resetFilters}
          statusOptions={SALE_STATUS_FILTER_OPTIONS}
          exportButton={canExport && (
            <Button
              type="button"
              variant="secondary"
              disabled={exporting}
              onClick={() => runExport(salesBrowseApi.exportSales)}
            >
              {exporting ? 'Export…' : 'Export CSV'}
            </Button>
          )}
        />
      </Card>

      <Card className="p-0 overflow-hidden">
        {sales.length === 0 ? (
          <div className="p-8"><EmptyState message="Aucune vente trouvée." /></div>
        ) : (
          <>
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead className="bg-gray-50 text-left text-gray-500">
                  <tr>
                    <th className="px-4 py-3 font-medium">N° vente</th>
                    <th className="px-4 py-3 font-medium">Date</th>
                    <th className="px-4 py-3 font-medium">Statut</th>
                    <th className="px-4 py-3 font-medium">Client</th>
                    <th className="px-4 py-3 font-medium">Vendeur</th>
                    <th className="px-4 py-3 font-medium">Caissier</th>
                    <th className="px-4 py-3 font-medium text-right">Total</th>
                    <th className="px-4 py-3 font-medium text-right">Retours</th>
                    <th className="px-4 py-3 font-medium" />
                  </tr>
                </thead>
                <tbody className="divide-y">
                  {sales.map((s) => (
                    <tr key={s.id} className="hover:bg-gray-50/80">
                      <td className="px-4 py-3 font-medium">{s.saleNumber}</td>
                      <td className="px-4 py-3 text-gray-600">
                        {formatPosDateTime(s.paidAt || s.validatedAt || s.createdAt)}
                      </td>
                      <td className="px-4 py-3">
                        <Badge tone={saleStatusTone(s.status)}>{saleStatusLabel(s.status)}</Badge>
                      </td>
                      <td className="px-4 py-3">{s.customerName || '—'}</td>
                      <td className="px-4 py-3">{s.sellerName || '—'}</td>
                      <td className="px-4 py-3">{s.cashierName || '—'}</td>
                      <td className="px-4 py-3 text-right font-medium">{formatPosMoney(s.total, currency)}</td>
                      <td className="px-4 py-3 text-right text-gray-600">
                        {s.refundCount > 0 ? (
                          <span>{s.refundCount} · {formatPosMoney(s.totalRefunded, currency)}</span>
                        ) : '—'}
                      </td>
                      <td className="px-4 py-3 text-right">
                        <Link to={`/sales/${s.id}`} className="text-emerald-600 hover:underline">
                          Détail
                        </Link>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <BrowsePagination pagination={pagination} onPageChange={goToPage} />
          </>
        )}
      </Card>
    </div>
  )
}
