import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { posApi, salesBrowseApi, settingsApi } from '../api'
import { PageHeader, Card, Loading, Badge, EmptyState, Button } from '../components/ui'
import { PosTicketModal } from '../components/pos/PosPrintModals'
import { useNotification } from '../context/NotificationContext'
import { useAuth } from '../context/AuthContext'
import { getErrorMessage } from '../utils/errors'
import { refundStatusLabel, saleStatusLabel } from '../utils/saleStatus'
import { formatPosDateTime, formatPosMoney } from '../utils/posMoney'
import { isReprintableSaleStatus } from '../types/sales'
import { refundStatusTone, saleStatusTone, paymentMethodLabel } from '../utils/saleDisplay'

function InfoRow({ label, value }) {
  return (
    <div>
      <p className="text-xs text-gray-500 uppercase">{label}</p>
      <p className="text-sm mt-0.5">{value ?? '—'}</p>
    </div>
  )
}

export default function SaleDetailPage() {
  const { id } = useParams()
  const notify = useNotification()
  const { hasPermission } = useAuth()
  const [loading, setLoading] = useState(true)
  const [currency, setCurrency] = useState('EUR')
  const [detail, setDetail] = useState(null)
  const [ticket, setTicket] = useState(null)
  const [printing, setPrinting] = useState(false)

  const canReprint = hasPermission('pos.ticket.print') || hasPermission('pos.ticket.reprint')

  useEffect(() => {
    settingsApi.getPublic().then((s) => setCurrency(s.currency || 'EUR')).catch(() => {})
  }, [])

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    salesBrowseApi.detail(id)
      .then((data) => { if (!cancelled) setDetail(data) })
      .catch((e) => notify.error(getErrorMessage(e)))
      .finally(() => { if (!cancelled) setLoading(false) })
    return () => { cancelled = true }
  }, [id, notify])

  const printTicket = async (saleId) => {
    setPrinting(true)
    try {
      setTicket(await posApi.ticket(saleId))
    } catch (e) {
      notify.error(getErrorMessage(e))
    } finally {
      setPrinting(false)
    }
  }

  if (loading) return <Loading />
  if (!detail?.sale) {
    return (
      <div className="p-8">
        <EmptyState message="Vente introuvable." />
        <Link to="/sales" className="text-sm text-emerald-600 hover:underline mt-4 inline-block">← Retour aux ventes</Link>
      </div>
    )
  }

  const sale = detail.sale

  return (
    <div className="space-y-6 pb-8">
      <PageHeader
        title={sale.saleNumber}
        subtitle="Détail de la vente"
        action={
          <div className="flex flex-col items-end gap-2">
            <Link to="/sales" className="text-sm text-emerald-600 hover:underline">
              ← Retour aux ventes
            </Link>
            {canReprint && isReprintableSaleStatus(sale.status) && (
              <Button
                type="button"
                variant="secondary"
                disabled={printing}
                onClick={() => printTicket(sale.id)}
              >
                {printing ? 'Chargement…' : 'Réimprimer ticket'}
              </Button>
            )}
          </div>
        }
      />

      <div className="flex flex-wrap items-center gap-2">
        <Badge tone={saleStatusTone(sale.status)}>{saleStatusLabel(sale.status)}</Badge>
        {detail.refunds?.length > 0 && (
          <Badge tone="warning">{detail.refunds.length} retour(s)</Badge>
        )}
      </div>

      <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-4">
        <Card className="p-4"><InfoRow label="Création" value={formatPosDateTime(sale.createdAt)} /></Card>
        <Card className="p-4"><InfoRow label="Encaissement" value={formatPosDateTime(sale.paidAt || sale.validatedAt)} /></Card>
        <Card className="p-4"><InfoRow label="Vendeur" value={sale.sellerName} /></Card>
        <Card className="p-4"><InfoRow label="Caissier" value={sale.cashierName} /></Card>
      </div>

      <Card className="p-4 grid md:grid-cols-2 gap-4">
        <InfoRow label="Client" value={sale.customerName ? `${sale.customerName}${sale.customerNumber ? ` (${sale.customerNumber})` : ''}` : '—'} />
        <InfoRow label="Téléphone" value={sale.customerPhone} />
        {sale.customerLoyaltyTier && (
          <InfoRow label="Fidélité" value={`${sale.customerLoyaltyTier} · ${sale.customerLoyaltyPoints ?? 0} pts`} />
        )}
      </Card>

      <Card className="p-0 overflow-hidden">
        <div className="px-4 py-3 border-b bg-gray-50 font-medium text-sm">Lignes</div>
        {sale.lignes?.length ? (
          <table className="w-full text-sm">
            <thead className="text-left text-gray-500">
              <tr>
                <th className="px-4 py-2 font-medium">Produit</th>
                <th className="px-4 py-2 font-medium">Qté</th>
                <th className="px-4 py-2 font-medium text-right">P.U.</th>
                <th className="px-4 py-2 font-medium text-right">Total</th>
              </tr>
            </thead>
            <tbody className="divide-y">
              {sale.lignes.map((l) => (
                <tr key={l.id}>
                  <td className="px-4 py-2">
                    <div>{l.productNom}</div>
                    {l.variantNameSnapshot && <div className="text-xs text-gray-500">{l.variantNameSnapshot}</div>}
                    {l.packagingNameSnapshot && <div className="text-xs text-gray-500">{l.packagingNameSnapshot}</div>}
                  </td>
                  <td className="px-4 py-2">{l.quantityInput ?? l.quantity}</td>
                  <td className="px-4 py-2 text-right">{formatPosMoney(l.unitPrice, currency)}</td>
                  <td className="px-4 py-2 text-right font-medium">{formatPosMoney(l.lineTotal, currency)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : (
          <div className="p-6"><EmptyState message="Aucune ligne." /></div>
        )}
      </Card>

      <div className="grid md:grid-cols-2 gap-4">
        <Card className="p-4 space-y-2 text-sm">
          <div className="flex justify-between"><span className="text-gray-500">Sous-total</span><span>{formatPosMoney(sale.subtotal, currency)}</span></div>
          {(sale.discountTotal > 0) && (
            <div className="flex justify-between"><span className="text-gray-500">Remises</span><span>-{formatPosMoney(sale.discountTotal, currency)}</span></div>
          )}
          {(sale.loyaltyDiscountAmount > 0) && (
            <div className="flex justify-between"><span className="text-gray-500">Fidélité</span><span>-{formatPosMoney(sale.loyaltyDiscountAmount, currency)}</span></div>
          )}
          {(sale.taxTotal > 0) && (
            <div className="flex justify-between"><span className="text-gray-500">TVA</span><span>{formatPosMoney(sale.taxTotal, currency)}</span></div>
          )}
          <div className="flex justify-between font-semibold text-base pt-2 border-t">
            <span>Total</span><span>{formatPosMoney(sale.total, currency)}</span>
          </div>
          {detail.totalRefunded > 0 && (
            <div className="flex justify-between text-amber-700">
              <span>Remboursé</span><span>-{formatPosMoney(detail.totalRefunded, currency)}</span>
            </div>
          )}
        </Card>

        <Card className="p-0 overflow-hidden">
          <div className="px-4 py-3 border-b bg-gray-50 font-medium text-sm">Paiements</div>
          {sale.payments?.length ? (
            <ul className="divide-y text-sm">
              {sale.payments.map((p) => (
                <li key={p.id} className="px-4 py-2 flex justify-between">
                  <span>{paymentMethodLabel(p.method)}</span>
                  <span className="font-medium">{formatPosMoney(p.amount, currency)}</span>
                </li>
              ))}
            </ul>
          ) : (
            <div className="p-4 text-sm text-gray-500">Aucun paiement enregistré.</div>
          )}
          {sale.paidAmount != null && (
            <div className="px-4 py-2 border-t text-sm flex justify-between bg-gray-50">
              <span>Encaissé</span>
              <span className="font-medium">{formatPosMoney(sale.paidAmount, currency)}</span>
            </div>
          )}
        </Card>
      </div>

      <Card className="p-0 overflow-hidden">
        <div className="px-4 py-3 border-b bg-gray-50 font-medium text-sm">Retours liés</div>
        {detail.refunds?.length ? (
          <table className="w-full text-sm">
            <thead className="text-left text-gray-500">
              <tr>
                <th className="px-4 py-2 font-medium">N° retour</th>
                <th className="px-4 py-2 font-medium">Date</th>
                <th className="px-4 py-2 font-medium">Statut</th>
                <th className="px-4 py-2 font-medium">Motif</th>
                <th className="px-4 py-2 font-medium text-right">Montant</th>
                <th className="px-4 py-2 font-medium" />
              </tr>
            </thead>
            <tbody className="divide-y">
              {detail.refunds.map((r) => (
                <tr key={r.id}>
                  <td className="px-4 py-2 font-medium">{r.refundNumber}</td>
                  <td className="px-4 py-2">{formatPosDateTime(r.validatedAt || r.createdAt)}</td>
                  <td className="px-4 py-2"><Badge tone={refundStatusTone(r.status)}>{refundStatusLabel(r.status)}</Badge></td>
                  <td className="px-4 py-2 text-gray-600">{r.reason || '—'}</td>
                  <td className="px-4 py-2 text-right">{formatPosMoney(r.totalAmount, currency)}</td>
                  <td className="px-4 py-2 text-right">
                    <Link to={`/returns/${r.id}`} className="text-emerald-600 hover:underline">Voir</Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : (
          <div className="p-6"><EmptyState message="Aucun retour pour cette vente." /></div>
        )}
      </Card>

      {detail.timeline?.length > 0 && (
        <Card className="p-4">
          <p className="text-sm font-medium mb-3">Historique</p>
          <ol className="space-y-3 border-l-2 border-emerald-200 pl-4">
            {detail.timeline.map((ev) => (
              <li key={ev.id || `${ev.eventType}-${ev.occurredAt}`} className="relative">
                <span className="absolute -left-[1.35rem] top-1.5 w-2 h-2 rounded-full bg-emerald-500" />
                <p className="text-sm font-medium">{ev.eventTypeLabel || ev.eventType}</p>
                {ev.description && ev.description !== ev.eventTypeLabel && (
                  <p className="text-xs text-gray-600">{ev.description}</p>
                )}
                {ev.details && <p className="text-xs text-gray-500">{ev.details}</p>}
                <p className="text-xs text-gray-400 mt-0.5">
                  {formatPosDateTime(ev.occurredAt)}
                  {ev.actorName ? ` · ${ev.actorName}` : ''}
                </p>
              </li>
            ))}
          </ol>
        </Card>
      )}

      {ticket && <PosTicketModal ticket={ticket} onClose={() => setTicket(null)} />}
    </div>
  )
}
