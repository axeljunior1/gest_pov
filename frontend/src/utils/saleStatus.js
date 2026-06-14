/** Libellés métier POS — distinct de la pause vendeur (HOLD). */
export const SALE_STATUS_LABELS = {
  DRAFT: 'Brouillon',
  HOLD: 'Pause vendeur',
  PENDING_PAYMENT: 'À encaisser',
  PAID: 'Payée',
  VALIDATED: 'Validée',
  CANCELLED: 'Annulée',
  REFUNDED: 'Remboursée',
  PARTIALLY_REFUNDED: 'Part. remboursée',
}

export function saleStatusLabel(status) {
  return SALE_STATUS_LABELS[status] || status || '—'
}

export const REFUND_STATUS_LABELS = {
  PENDING: 'En attente',
  APPROVED: 'Approuvé',
  COMPLETED: 'Validé',
  REJECTED: 'Rejeté',
}

export function refundStatusLabel(status) {
  return REFUND_STATUS_LABELS[status] || status || '—'
}

/** Vente renvoyée par la caisse (HOLD avec libellé dédié). */
export function isCashierRecallHold(sale) {
  return sale?.status === 'HOLD' && String(sale?.holdLabel || '').startsWith('Retour caisse')
}
