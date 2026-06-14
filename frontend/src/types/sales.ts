/** DTOs métier ventes POS (miroir backend) */

export type SaleStatus =
  | 'DRAFT'
  | 'HOLD'
  | 'PENDING_PAYMENT'
  | 'PAID'
  | 'VALIDATED'
  | 'CANCELLED'
  | 'REFUNDED'
  | 'PARTIALLY_REFUNDED'

export type RefundStatus = 'PENDING' | 'APPROVED' | 'COMPLETED' | 'REJECTED'

export interface SaleSummary {
  id: number
  saleNumber: string
  status: SaleStatus
  createdAt?: string
  paidAt?: string
  validatedAt?: string
  customerName?: string
  sellerName?: string
  cashierName?: string
  total?: number
  paidAmount?: number
  refundCount: number
  totalRefunded?: number
}

export interface SaleRefundSummary {
  id: number
  refundNumber: string
  saleId: number
  saleNumber: string
  status: RefundStatus
  totalAmount?: number
  reason?: string
  createdBy?: string
  createdAt?: string
  validatedAt?: string
  lineCount: number
}

export interface SaleDetail {
  sale: Record<string, unknown>
  totalRefunded?: number
  refunds: SaleRefundSummary[]
  timeline?: Array<Record<string, unknown>>
}

/** Statuts éligibles à la réimpression ticket */
export const REPRINTABLE_SALE_STATUSES: SaleStatus[] = [
  'PAID',
  'VALIDATED',
  'PARTIALLY_REFUNDED',
  'REFUNDED',
]

export function isReprintableSaleStatus(status: string | undefined): boolean {
  return REPRINTABLE_SALE_STATUSES.includes(status as SaleStatus)
}
