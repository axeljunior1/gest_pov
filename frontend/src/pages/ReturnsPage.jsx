import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { salesBrowseApi, settingsApi } from '../api'
import { PageHeader, Card, Loading, Badge, EmptyState, Button } from '../components/ui'
import BrowsePagination from '../components/BrowsePagination'
import BrowseFiltersPanel from '../components/BrowseFiltersPanel'
import { useSalesBrowseFilters, RETURN_BROWSE_EXTRA_KEYS } from '../hooks/useSalesBrowseFilters'
import { useAuth } from '../context/AuthContext'
import { refundStatusLabel } from '../utils/saleStatus'
import { formatPosDateTime, formatPosMoney } from '../utils/posMoney'
import { REFUND_STATUS_FILTER_OPTIONS, refundStatusTone } from '../utils/saleDisplay'

export default function ReturnsPage() {
  const { hasAnyPermission } = useAuth()
  const [currency, setCurrency] = useState('EUR')
  const canExport = hasAnyPermission('export.read', 'analytics.export', 'pos.return.read', 'pos.report.read')

  const {
    items: returns,
    loading,
    exporting,
    draft,
    updateDraft,
    applyFilters,
    resetFilters,
    goToPage,
    pagination,
    runExport,
  } = useSalesBrowseFilters({
    fetchFn: salesBrowseApi.listReturns,
    extraParamKeys: RETURN_BROWSE_EXTRA_KEYS,
  })

  useEffect(() => {
    settingsApi.getPublic().then((s) => setCurrency(s.currency || 'EUR')).catch(() => {})
  }, [])

  if (loading && !returns.length) return <Loading />

  return (
    <div className="space-y-6 pb-8">
      <PageHeader
        title="Retours"
        subtitle="Consultation des retours et remboursements POS"
        action={
          <Link to="/sales" className="text-sm text-emerald-600 hover:underline">
            ← Voir les ventes
          </Link>
        }
      />

      <Card className="p-4">
        <BrowseFiltersPanel
          draft={draft}
          updateDraft={updateDraft}
          onApply={applyFilters}
          onReset={resetFilters}
          statusOptions={REFUND_STATUS_FILTER_OPTIONS}
          searchPlaceholder="N° retour, vente, client…"
          extraFields={(
            <div>
              <label className="text-xs text-gray-500 block mb-1">N° vente (ID)</label>
              <input
                type="number"
                min="1"
                value={draft.saleId || ''}
                onChange={(e) => updateDraft({ saleId: e.target.value })}
                placeholder="ID vente"
                className="rounded-lg border px-3 py-2 text-sm w-28"
              />
            </div>
          )}
          exportButton={canExport && (
            <Button
              type="button"
              variant="secondary"
              disabled={exporting}
              onClick={() => runExport(salesBrowseApi.exportReturns)}
            >
              {exporting ? 'Export…' : 'Export CSV'}
            </Button>
          )}
        />
      </Card>

      {pagination.totalElements > 0 && (
        <p className="text-sm text-gray-500">
          {pagination.totalElements} retour{pagination.totalElements > 1 ? 's' : ''}
          {loading ? ' — chargement…' : ''}
        </p>
      )}

      <Card className="p-0 overflow-hidden">
        {returns.length === 0 ? (
          <div className="p-8"><EmptyState message="Aucun retour trouvé." /></div>
        ) : (
          <>
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead className="bg-gray-50 text-left text-gray-500">
                  <tr>
                    <th className="px-4 py-3 font-medium">N° retour</th>
                    <th className="px-4 py-3 font-medium">Vente</th>
                    <th className="px-4 py-3 font-medium">Date</th>
                    <th className="px-4 py-3 font-medium">Statut</th>
                    <th className="px-4 py-3 font-medium">Client</th>
                    <th className="px-4 py-3 font-medium">Caissier</th>
                    <th className="px-4 py-3 font-medium">Motif</th>
                    <th className="px-4 py-3 font-medium text-right">Lignes</th>
                    <th className="px-4 py-3 font-medium text-right">Montant</th>
                    <th className="px-4 py-3 font-medium" />
                  </tr>
                </thead>
                <tbody className="divide-y">
                  {returns.map((r) => (
                    <tr key={r.id} className="hover:bg-gray-50/80">
                      <td className="px-4 py-3 font-medium">{r.refundNumber}</td>
                      <td className="px-4 py-3">
                        <Link to={`/sales/${r.saleId}`} className="text-emerald-600 hover:underline">
                          {r.saleNumber}
                        </Link>
                      </td>
                      <td className="px-4 py-3 text-gray-600">
                        {formatPosDateTime(r.validatedAt || r.createdAt)}
                      </td>
                      <td className="px-4 py-3">
                        <Badge tone={refundStatusTone(r.status)}>{refundStatusLabel(r.status)}</Badge>
                      </td>
                      <td className="px-4 py-3">{r.customerName || '—'}</td>
                      <td className="px-4 py-3">{r.createdBy || '—'}</td>
                      <td className="px-4 py-3 text-gray-600 max-w-[12rem] truncate" title={r.reason || ''}>
                        {r.reason || '—'}
                      </td>
                      <td className="px-4 py-3 text-right text-gray-600">{r.lineCount ?? '—'}</td>
                      <td className="px-4 py-3 text-right font-medium">{formatPosMoney(r.totalAmount, currency)}</td>
                      <td className="px-4 py-3 text-right">
                        <Link to={`/returns/${r.id}`} className="text-emerald-600 hover:underline">
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
