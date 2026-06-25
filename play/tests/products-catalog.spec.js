import { test, expect } from '@playwright/test'
import { loginAsAdmin } from '../helpers/auth.js'
import { createRootCategory } from '../helpers/categories.js'
import { uniqueName } from '../helpers/test-data.js'
import { inputByLabel, selectByLabel } from '../helpers/forms.js'
import {
  bulkDeleteSelectedProducts,
  createProductViaUi,
  deleteProductOnDetailPage,
  filterProductsByLifecycle,
  goToProductsCatalog,
  openProductFromList,
  searchProducts,
  selectAllProductsOnPage,
} from '../helpers/products.js'

test.describe('Produits — catalogue', () => {
  test('recherche un produit par son nom TEST', async ({ page }) => {
    const productName = uniqueName('TEST-PW-SEARCH')
    await loginAsAdmin(page)
    await createProductViaUi(page, { name: productName })
    await searchProducts(page, productName)
    await expect(page.locator('table tbody tr').filter({ hasText: productName })).toBeVisible()
  })

  test('ouvre la fiche produit depuis la liste', async ({ page }) => {
    const productName = uniqueName('TEST-PW-DETAIL')
    await loginAsAdmin(page)
    await createProductViaUi(page, { name: productName })
    await openProductFromList(page, productName)
  })

  test('modifie le prix vente et enregistre', async ({ page }) => {
    const productName = uniqueName('TEST-PW-EDIT')
    await loginAsAdmin(page)
    await createProductViaUi(page, { name: productName, prixVente: '10' })
    await inputByLabel(page, 'Prix vente').fill('19.99')
    const updateResponse = page.waitForResponse(
      (res) => res.url().match(/\/api\/products\/\d+$/) && res.request().method() === 'PUT' && res.ok(),
    )
    await page.getByRole('button', { name: 'Enregistrer' }).click()
    await updateResponse
    await page.reload()
    await expect(inputByLabel(page, 'Prix vente')).toHaveValue('19.99')
  })

  test('filtre par cycle de vie Brouillon', async ({ page }) => {
    const productName = uniqueName('TEST-PW-LIFE')
    await loginAsAdmin(page)
    await createProductViaUi(page, { name: productName })
    await filterProductsByLifecycle(page, 'Brouillon')
    await searchProducts(page, productName)
    await expect(page.locator('table tbody tr').filter({ hasText: productName })).toBeVisible()
  })

  test('associe une catégorie à la création', async ({ page }) => {
    const categoryName = uniqueName('TEST-Cat-Prod')
    const productName = uniqueName('TEST-PW-CAT')
    await loginAsAdmin(page)
    await createRootCategory(page, categoryName)
    await createProductViaUi(page, { name: productName, categoryLabel: categoryName })
    await openProductFromList(page, productName)
    await expect(selectByLabel(page, 'Catégorie')).toContainText(categoryName)
  })

  test('supprime un produit TEST depuis la fiche', async ({ page }) => {
    const productName = uniqueName('TEST-PW-DELETE')
    await loginAsAdmin(page)
    await createProductViaUi(page, { name: productName })
    await deleteProductOnDetailPage(page, productName)
  })

  test('supprime plusieurs produits sélectionnés', async ({ page }) => {
    const prefix = uniqueName('TEST-PW-BULK')
    const name1 = `${prefix}-A`
    const name2 = `${prefix}-B`
    await loginAsAdmin(page)
    await createProductViaUi(page, { name: name1 })
    await createProductViaUi(page, { name: name2 })
    await searchProducts(page, prefix)
    await expect(page.locator('table tbody tr').filter({ hasText: prefix })).toHaveCount(2, { timeout: 15_000 })
    await selectAllProductsOnPage(page)
    await bulkDeleteSelectedProducts(page)
    await searchProducts(page, prefix)
    await expect(page.locator('table tbody tr').filter({ hasText: prefix })).toHaveCount(0)
  })
})
