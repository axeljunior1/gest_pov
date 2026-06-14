import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { salesBrowseApi, settingsApi } from '../api'
import { PageHeader, Card, Loading, Badge, EmptyState } from '../components/ui'
import { useNotification } from '../context/NotificationContext'
import { getErrorMessage } from '../utils/errors'
import { refundStatusLabel } from '../utils/saleStatus'
import { formatPosDateTime, formatPosMoney } from '../utils/posMoney'
import { refundStatusTone } from '../utils/saleDisplay'

function InfoRow({ label, value }) {
  return (
    <div>
      <p className="text-xs text-gray-500 uppercase">{label}</p>
      <p className="text-sm mt-0.5">{value ?? '—'}</p>
    </div>
  )
}

export default function ReturnDetailPage() {
  const { id } = useParams()
  const notify = useNotification()
  const [loading, setLoading] = useState(true)
  const [currency, setCurrency] = useState('EUR')
  const [retour, setRetour] = useState(null)

  useEffect(() => {
    settingsApi.getPublic().then((s) => setCurrency(s.currency || 'EUR')).catch(() => {})
  }, [])

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    salesBrowseApi.returnDetail(id)
      .then((data) => { if (!cancelled) setRetour(data) })
      .catch((e) => notify.error(getErrorMessage(e)))
      .finally(() => { if (!cancelled) setLoading(false) })
    return () => { cancelled = true }
  }, [id, notify])

  if (loading) return <Loading />
  if (!retour) {
    return (
      <div className="p-8">
        <EmptyState message="Retour introuvable." />
        <Link to="/returns" className="text-sm text-emerald-600 hover:underline mt-4 inline-block">← Retour aux retours</Link>
      </div>
    )
  }

  return (
    <div className="space-y-6 pb-8">
      <PageHeader
        title={retour.refundNumber}
        subtitle="Détail du retour"
        action={
          <div className="flex flex-col items-end gap-1 text-sm">
            <Link to="/returns" className="text-emerald-600 hover:underline">← Retour aux retours</Link>
            {retour.saleId && (
              <Link to={`/sales/${retour.saleId}`} className="text-emerald-600 hover:underline">
                Vente {retour.saleNumber} →
              </Link>
            )}
          </div>
        }
      />

      <div className="flex flex-wrap items-center gap-2">
        <Badge tone={refundStatusTone(retour.status)}>{refundStatusLabel(retour.status)}</Badge>
        {retour.returnToStock != null && (
          <Badge tone={retour.returnToStock ? 'success' : 'default'}>
            {retour.returnToStock ? 'Réintégration stock' : 'Sans réintégration'}
          </Badge>
        )}
      </div>

      <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-4">
        <Card className="p-4">
          <InfoRow
            label="Vente d'origine"
            value={
              retour.saleId ? (
                <Link to={`/sales/${retour.saleId}`} className="text-emerald-600 hover:underline">
                  {retour.saleNumber}
                </Link>
              ) : retour.saleNumber
            }
          />
        </Card>
        <Card className="p-4"><InfoRow label="Créé le" value={formatPosDateTime(retour.createdAt)} /></Card>
        <Card className="p-4"><InfoRow label="Validé le" value={formatPosDateTime(retour.validatedAt || retour.completedAt)} /></Card>
        <Card className="p-4"><InfoRow label="Par" value={retour.createdBy} /></Card>
      </div>

      <Card className="p-4 grid md:grid-cols-2 gap-4">
        <InfoRow label="Motif" value={retour.reason} />
        <InfoRow label="Notes" value={retour.notes} />
        <InfoRow label="Montant total" value={formatPosMoney(retour.totalAmount, currency)} />
      </Card>

      <Card className="p-0 overflow-hidden">
        <div className="px-4 py-3 border-b bg-gray-50 font-medium text-sm">Lignes retournées</div>
        {retour.lignes?.length ? (
          <table className="w-full text-sm">
            <thead className="text-left text-gray-500">
              <tr>
                <th className="px-4 py-2 font-medium">Produit</th>
                <th className="px-4 py-2 font-medium">Qté</th>
                <th className="px-4 py-2 font-medium">Stock</th>
                <th className="px-4 py-2 font-medium text-right">Remboursé</th>
                <th className="px-4 py-2 font-medium">Motif</th>
              </tr>
            </thead>
            <tbody className="divide-y">
              {retour.lignes.map((l) => (
                <tr key={l.id}>
                  <td className="px-4 py-2">
                    <div>{l.productNom}</div>
                    {l.packagingNameSnapshot && <div className="text-xs text-gray-500">{l.packagingNameSnapshot}</div>}
                  </td>
                  <td className="px-4 py-2">{l.quantity}</td>
                  <td className="px-4 py-2">{l.restock ? 'Oui' : 'Non'}</td>
                  <td className="px-4 py-2 text-right font-medium">{formatPosMoney(l.refundAmount, currency)}</td>
                  <td className="px-4 py-2 text-gray-600">{l.reason || '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : (
          <div className="p-6"><EmptyState message="Aucune ligne." /></div>
        )}
      </Card>

      {retour.payments?.length > 0 && (
        <Card className="p-0 overflow-hidden">
          <div className="px-4 py-3 border-b bg-gray-50 font-medium text-sm">Remboursements</div>
          <ul className="divide-y text-sm">
            {retour.payments.map((p) => (
              <li key={p.id} className="px-4 py-2 flex justify-between">
                <span>{p.method}</span>
                <span className="font-medium">{formatPosMoney(p.amount, currency)}</span>
              </li>
            ))}
          </ul>
        </Card>
      )}
    </div>
  )
}
