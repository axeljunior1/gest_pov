import { test, expect } from '@playwright/test'
import { loginAsAdmin } from '../helpers/auth.js'
import { uniqueName } from '../helpers/test-data.js'
import { createProductViaUi } from '../helpers/products.js'
import {
  createAndValidateStockEntry,
  createAndValidateStockExit,
  expectStockMovementForProduct,
  expectStockQuantityForProduct,
} from '../helpers/stock.js'

test.describe.configure({ mode: 'serial' })

test.describe('Stock', () => {
  test('entrée validée augmente le stock consultable', async ({ page }) => {
    const productName = uniqueName('TEST-STOCK-IN')
    await loginAsAdmin(page)
    await createProductViaUi(page, { name: productName, prixVente: '5', statutLabel: 'Actif' })
    await createAndValidateStockEntry(page, productName, 50)
    await expectStockQuantityForProduct(page, productName, 50)
  })

  test('sortie validée diminue le stock', async ({ page }) => {
    const productName = uniqueName('TEST-STOCK-OUT')
    await loginAsAdmin(page)
    await createProductViaUi(page, { name: productName, prixVente: '5', statutLabel: 'Actif' })
    await createAndValidateStockEntry(page, productName, 40)
    await createAndValidateStockExit(page, productName, 10)
    await expectStockQuantityForProduct(page, productName, 30)
  })

  test('mouvement de stock visible dans l’historique', async ({ page }) => {
    const productName = uniqueName('TEST-STOCK-MVT')
    await loginAsAdmin(page)
    await createProductViaUi(page, { name: productName, prixVente: '5', statutLabel: 'Actif' })
    await createAndValidateStockEntry(page, productName, 15)
    await expectStockMovementForProduct(page, productName)
  })
})
