import { test, expect } from '@playwright/test'
import { loginAsAdmin } from '../helpers/auth.js'
import { uniqueName } from '../helpers/test-data.js'
import { createProductViaUi } from '../helpers/products.js'

test.describe('Conditionnements', () => {
  test('crée un conditionnement sur un produit TEST', async ({ page }) => {
    const productName = uniqueName('TEST-PW-PKG')
    const packagingName = uniqueName('TEST-Carton')

    await loginAsAdmin(page)
    await createProductViaUi(page, { name: productName, prixVente: '10' })

    await page.getByRole('button', { name: 'Conditionnements' }).click()
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

    const row = page.locator('table tbody tr').filter({ hasText: packagingName })
    await expect(row).toBeVisible()
    await expect(row).toContainText('6')
  })

  test('supprime un conditionnement', async ({ page }) => {
    const productName = uniqueName('TEST-PW-PKG-DEL')
    const packagingName = uniqueName('TEST-Carton-DEL')

    await loginAsAdmin(page)
    await createProductViaUi(page, { name: productName, prixVente: '10' })
    await page.getByRole('button', { name: 'Conditionnements' }).click()

    await page.getByPlaceholder('Nom (ex: Carton)').fill(packagingName)
    await page.getByPlaceholder('Qté unité de base (ex: 12)').fill('12')
    await page.getByPlaceholder('Prix vente condi.').fill('120')
    await page
      .getByRole('heading', { name: 'Ajouter un conditionnement' })
      .locator('..')
      .getByRole('button', { name: 'Ajouter' })
      .click()
    await expect(page.locator('table tbody tr').filter({ hasText: packagingName })).toBeVisible()

    const row = page.locator('table tbody tr').filter({ hasText: packagingName })
    page.once('dialog', (dialog) => dialog.accept())
    const deleteResponse = page.waitForResponse(
      (res) => res.url().includes('/packagings/') && res.request().method() === 'DELETE',
    )
    await row.getByRole('button', { name: 'Suppr.' }).click()
    await deleteResponse
    await expect(page.locator('table tbody tr').filter({ hasText: packagingName })).toHaveCount(0)
  })
})
