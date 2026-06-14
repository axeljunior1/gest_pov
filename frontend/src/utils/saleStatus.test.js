import { describe, expect, it } from 'vitest'
import { refundStatusLabel, saleStatusLabel } from './saleStatus'

describe('saleStatusLabel', () => {
  it('traduit les statuts connus', () => {
    expect(saleStatusLabel('PAID')).toBe('Payée')
    expect(saleStatusLabel('HOLD')).toBe('Pause vendeur')
  })

  it('retourne le code brut si inconnu', () => {
    expect(saleStatusLabel('UNKNOWN')).toBe('UNKNOWN')
  })
})

describe('refundStatusLabel', () => {
  it('traduit COMPLETED', () => {
    expect(refundStatusLabel('COMPLETED')).toBe('Validé')
  })
})
