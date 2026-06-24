import { test, expect } from '@playwright/test'
import { loginAsAdmin } from '../helpers/auth.js'
import { createProductViaUi } from '../helpers/products.js'

test.describe('Conditionnements', () => {
  test('crée un conditionnement sur un produit TEST', async ({ page }) => {
    const productName = `TEST-PW-PKG-${Date.now()}`
    const packagingName = `TEST-Carton-${Date.now()}`

    await loginAsAdmin(page)
    await createProductViaUi(page, { name: productName, prixVente: '10' })

    await page.getByRole('button', { name: 'Conditionnements' }).click()
    await expect(page.getByRole('heading', { name: 'Ajouter un conditionnement' })).toBeVisible()

    await page.getByPlaceholder('Nom (ex: Carton)').fill(packagingName)
    await page.getByPlaceholder('Symbole (ex: ctn)').fill('ctn')
    await page.getByPlaceholder('Qté unité de base (ex: 12)').fill('6')
    await page.getByPlaceholder('Prix vente condi.').fill('60')

    const packagingResponse = page.waitForResponse(
      (res) => res.url().includes('/packagings') && res.request().method() === 'POST' && res.ok(),
    )
    await page
      .getByRole('heading', { name: 'Ajouter un conditionnement' })
      .locator('..')
      .getByRole('button', { name: 'Ajouter' })
      .click()
    await packagingResponse

    const packagingRow = page.locator('table tbody tr').filter({ hasText: packagingName })
    await expect(packagingRow).toBeVisible()
    await expect(packagingRow).toContainText('ctn')
    await expect(packagingRow).toContainText('1')
    await expect(packagingRow).toContainText(packagingName)
    await expect(packagingRow).toContainText('6')
    await expect(packagingRow).toContainText('pcs')
  })
})
