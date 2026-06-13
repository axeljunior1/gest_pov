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
