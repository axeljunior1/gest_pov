import { test, expect } from '@playwright/test'
import { loginAsAdmin } from '../helpers/auth.js'
import { inputByLabel } from '../helpers/forms.js'
import {
  createProductViaUi,
  deleteProductOnDetailPage,
  openProductFromList,
  searchProducts,
} from '../helpers/products.js'

test.describe('Catalogue produits', () => {
  test('recherche un produit par son nom TEST', async ({ page }) => {
    const productName = `TEST-PW-SEARCH-${Date.now()}`

    await loginAsAdmin(page)
    await createProductViaUi(page, { name: productName })

    await searchProducts(page, productName)
    await expect(page.locator('table tbody tr').filter({ hasText: productName })).toBeVisible()
  })

  test('ouvre la fiche produit depuis la liste', async ({ page }) => {
    const productName = `TEST-PW-DETAIL-${Date.now()}`

    await loginAsAdmin(page)
    await createProductViaUi(page, { name: productName })
    await openProductFromList(page, productName)
  })

  test('modifie le prix vente et enregistre', async ({ page }) => {
    const productName = `TEST-PW-EDIT-${Date.now()}`
    const newPrice = '19.99'

    await loginAsAdmin(page)
    await createProductViaUi(page, { name: productName, prixVente: '10' })

    await inputByLabel(page, 'Prix vente').fill(newPrice)
    const updateResponse = page.waitForResponse(
      (res) => res.url().match(/\/api\/products\/\d+$/) && res.request().method() === 'PUT' && res.ok(),
    )
    await page.getByRole('button', { name: 'Enregistrer' }).click()
    await updateResponse

    await expect(inputByLabel(page, 'Prix vente')).toHaveValue(newPrice)
    await page.reload()
    await expect(inputByLabel(page, 'Prix vente')).toHaveValue(newPrice)
  })

  test('supprime un produit TEST', async ({ page }) => {
    const productName = `TEST-PW-DELETE-${Date.now()}`

    await loginAsAdmin(page)
    await createProductViaUi(page, { name: productName })
    await deleteProductOnDetailPage(page, productName)
  })
})
