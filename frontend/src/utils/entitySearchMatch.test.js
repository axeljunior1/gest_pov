import { describe, expect, it } from 'vitest'
import { findEntityMatch, filterEntities, resolvePosProductMatch } from './entitySearchMatch'

describe('entitySearchMatch', () => {
  const product = {
    nom: 'Café Arabica 1kg',
    sku: 'CAF001',
    codeBarre: '3761234567890',
    barcodes: ['3761234567890'],
    categorieNom: 'Boissons',
    marque: 'Torrefacteur',
  }

  it('detects SKU match', () => {
    expect(findEntityMatch('CAF001', product, 'product')).toEqual({
      label: 'SKU',
      value: 'CAF001',
    })
  })

  it('detects EAN13 match', () => {
    expect(findEntityMatch('3761234567890', product, 'product')).toEqual({
      label: 'Code-barres',
      value: '3761234567890',
    })
  })

  it('detects name match', () => {
    expect(findEntityMatch('Arabica', product, 'product')).toEqual({
      label: 'Nom',
      value: 'Café Arabica 1kg',
    })
  })

  it('filters customers locally', () => {
    const customers = [
      { id: 1, fullName: 'Jean Dupont', phone: '0612345678', email: 'j@ex.com', customerNumber: 'C001' },
      { id: 2, fullName: 'Marie Martin', phone: '0698765432', email: 'm@ex.com', customerNumber: 'C002' },
    ]
    expect(filterEntities(customers, '0612', 'customer')).toHaveLength(1)
  })

  it('maps POS barcode match type', () => {
    expect(resolvePosProductMatch('3761234567890', product, 'EXACT_BARCODE')).toEqual({
      label: 'Code-barres / EAN13',
      value: '3761234567890',
    })
  })
})
