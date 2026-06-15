export function saleStatusTone(status) {
  switch (status) {
    case 'PAID':
    case 'VALIDATED':
      return 'success'
    case 'CANCELLED':
      return 'danger'
    case 'REFUNDED':
    case 'PARTIALLY_REFUNDED':
      return 'warning'
    case 'PENDING_PAYMENT':
      return 'info'
    default:
      return 'default'
  }
}

export function refundStatusTone(status) {
  switch (status) {
    case 'COMPLETED':
      return 'success'
    case 'REJECTED':
      return 'danger'
    case 'PENDING':
      return 'warning'
    default:
      return 'default'
  }
}

export const SALE_STATUS_FILTER_OPTIONS = [
  { value: '', label: 'Tous les statuts' },
  { value: 'DRAFT', label: 'Brouillon' },
  { value: 'HOLD', label: 'Pause vendeur' },
  { value: 'PENDING_PAYMENT', label: 'À encaisser' },
  { value: 'PAID', label: 'Payée' },
  { value: 'VALIDATED', label: 'Validée' },
  { value: 'CANCELLED', label: 'Annulée' },
  { value: 'REFUNDED', label: 'Remboursée' },
  { value: 'PARTIALLY_REFUNDED', label: 'Part. remboursée' },
]

export const REFUND_STATUS_FILTER_OPTIONS = [
  { value: '', label: 'Tous les statuts' },
  { value: 'PENDING', label: 'En attente' },
  { value: 'APPROVED', label: 'Approuvé' },
  { value: 'COMPLETED', label: 'Validé' },
  { value: 'REJECTED', label: 'Rejeté' },
]

export function buildBrowseDateParams(dateFrom, dateTo) {
  const params = {}
  if (dateFrom) params.dateFrom = `${dateFrom}T00:00:00Z`
  if (dateTo) params.dateTo = `${dateTo}T23:59:59Z`
  return params
}

export function canBrowseSalesBackOffice(hasPermission, hasAnyPermission) {
  return hasAnyPermission(
    'pos.sale.read',
    'pos.sale.read_own',
    'analytics.sales.read',
    'pos.report.read',
  )
}

export function canBrowseReturnsBackOffice(hasPermission, hasAnyPermission) {
  return hasAnyPermission(
    'pos.return.read',
    'analytics.sales.read',
    'pos.report.read',
  )
}

export const PAYMENT_METHOD_LABELS = {
  CASH: 'Espèces',
  CARD: 'Carte',
  MOBILE_MONEY: 'Mobile money',
  BANK_TRANSFER: 'Virement',
  CHECK: 'Chèque',
  OTHER: 'Autre',
}

export function paymentMethodLabel(method) {
  if (!method) return '—'
  return PAYMENT_METHOD_LABELS[method] || method
}
