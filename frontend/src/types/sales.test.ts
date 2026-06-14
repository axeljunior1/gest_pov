import { describe, expect, it } from 'vitest'
import { isReprintableSaleStatus, REPRINTABLE_SALE_STATUSES } from './sales'

describe('isReprintableSaleStatus', () => {
  it('accepte les ventes encaissées', () => {
    expect(isReprintableSaleStatus('PAID')).toBe(true)
    expect(isReprintableSaleStatus('VALIDATED')).toBe(true)
  })

  it('refuse brouillon et annulation', () => {
    expect(isReprintableSaleStatus('DRAFT')).toBe(false)
    expect(isReprintableSaleStatus('CANCELLED')).toBe(false)
  })

  it('couvre tous les statuts déclarés réimprimables', () => {
    for (const s of REPRINTABLE_SALE_STATUSES) {
      expect(isReprintableSaleStatus(s)).toBe(true)
    }
  })
})
