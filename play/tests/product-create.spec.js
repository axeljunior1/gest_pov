import { test, expect } from '@playwright/test'
import { loginAsAdmin } from '../helpers/auth.js'
import { inputByLabel } from '../helpers/forms.js'

test.describe('Produits', () => {
  test('crée un produit dont le nom commence par TEST', async ({ page }) => {
    const productName = `TEST-PW-${Date.now()}`

    await loginAsAdmin(page)
    await page.goto('/products/new')

    await expect(page.getByRole('heading', { name: 'Nouveau produit' })).toBeVisible()
    await inputByLabel(page, 'Nom').fill(productName)
    await inputByLabel(page, 'Prix vente').fill('12.50')

    const createResponse = page.waitForResponse(
      (res) => res.url().includes('/api/products') && res.request().method() === 'POST' && res.ok(),
    )
    await page.getByRole('button', { name: 'Créer' }).click()
    await createResponse

    await expect(page).toHaveURL(/\/products\/\d+/)
    await expect(page.getByRole('heading', { name: productName })).toBeVisible()

    await page.goto('/')
    await page.getByPlaceholder(/Rechercher un produit/).fill(productName)
    await page.getByRole('button', { name: 'Filtrer' }).click()
    await expect(page.locator('table tbody tr').filter({ hasText: productName })).toBeVisible()
  })
})
