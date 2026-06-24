import { expect } from '@playwright/test'
import { inputByLabel, selectByLabel } from './forms.js'

function initialVariantsSection(page) {
  return page.getByRole('heading', { name: 'Variantes initiales' }).locator('..')
}

export async function fillInitialVariants(page, combinations) {
  for (let i = 1; i < combinations.length; i++) {
    await page.getByRole('button', { name: '+ Variante' }).click()
  }
  for (let i = 0; i < combinations.length; i++) {
    const row = initialVariantsSection(page).locator('.grid-cols-6').nth(i)
    await row.getByPlaceholder('Couleur').fill(combinations[i].couleur)
    await row.getByPlaceholder('Taille').fill(combinations[i].taille)
    if (combinations[i].prix) {
      await row.getByPlaceholder('Prix').fill(combinations[i].prix)
    }
  }
}

/**
 * Crée un produit via l'UI (onglet Général) et retourne son nom.
 */
export async function createProductViaUi(page, {
  name = `TEST-PW-${Date.now()}`,
  prixVente = '10',
  unitLabel = 'Pièce (pcs)',
  statutLabel = 'Actif',
  variants = null,
} = {}) {
  await page.goto('/products/new')
  await expect(page.getByRole('heading', { name: 'Nouveau produit' })).toBeVisible()
  await inputByLabel(page, 'Nom').fill(name)
  await inputByLabel(page, 'Prix vente').fill(prixVente)
  await selectByLabel(page, 'Unité de base (stock)').selectOption({ label: unitLabel })
  await selectByLabel(page, 'Statut').selectOption({ label: statutLabel })

  if (variants?.length) {
    await fillInitialVariants(page, variants)
  }

  const createResponse = page.waitForResponse(
    (res) => res.url().includes('/api/products') && res.request().method() === 'POST' && res.ok(),
  )
  await page.getByRole('button', { name: 'Créer' }).click()
  await createResponse

  await expect(page).toHaveURL(/\/products\/\d+/)
  await expect(page.getByRole('heading', { name })).toBeVisible()
  return name
}

export async function goToProductsCatalog(page) {
  const isCatalogHome = /\/$/.test(new URL(page.url()).pathname)

  const listResponse = page.waitForResponse(
    (res) => {
      const url = res.url()
      return res.request().method() === 'GET'
        && res.ok()
        && /\/api\/products(\?|$)/.test(url)
        && !/\/api\/products\/\d+/.test(url)
    },
    { timeout: 60_000 },
  )

  if (isCatalogHome) {
    await page.reload()
  } else if (/\/products\//.test(page.url())) {
    await page.locator('aside').getByRole('link', { name: 'Produits' }).click()
  } else {
    await page.goto('/')
  }

  await listResponse
  await expect(page.getByPlaceholder(/Rechercher un produit/)).toBeVisible({ timeout: 60_000 })
}

export async function searchProducts(page, query) {
  await goToProductsCatalog(page)
  await page.getByPlaceholder(/Rechercher un produit/).fill(query)
  const searchResponse = page.waitForResponse(
    (res) => res.request().method() === 'GET'
      && res.ok()
      && res.url().includes('/api/products')
      && res.url().includes('query='),
  )
  await page.getByRole('button', { name: 'Filtrer' }).click()
  await searchResponse
}

export async function openProductFromList(page, productName) {
  await searchProducts(page, productName)
  const row = page.locator('table tbody tr').filter({ hasText: productName }).first()
  await expect(row).toBeVisible({ timeout: 15_000 })
  await row.click()
  await expect(page).toHaveURL(/\/products\/\d+/)
  await expect(page.getByRole('heading', { name: productName })).toBeVisible()
}

export async function deleteProductOnDetailPage(page, productName) {
  page.once('dialog', (dialog) => dialog.accept())
  const deleteResponse = page.waitForResponse(
    (res) => res.url().includes('/api/products/') && res.request().method() === 'DELETE',
  )
  await page.getByRole('button', { name: 'Supprimer' }).click()
  await deleteResponse
  await expect(page).toHaveURL('/')
  await searchProducts(page, productName)
  await expect(page.locator('table tbody tr').filter({ hasText: productName })).toHaveCount(0)
}
