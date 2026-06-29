import { expect } from '@playwright/test'

export function cartAside(page) {
  return page.locator('aside').filter({ has: page.getByRole('heading', { name: 'Panier' }) })
}

export async function currentSaleNumber(page) {
  const number = await page.getByRole('heading', { name: 'Panier' }).locator('xpath=following-sibling::p[1]').textContent()
  return number.trim()
}

export async function goToPosSeller(page) {
  await page.goto('/pos')
  await expect(
    page.getByRole('heading', { name: 'Ouvrir le poste vendeur' }).or(page.locator('#pos-search')),
  ).toBeVisible({ timeout: 30_000 })
}

export async function goToPosCashier(page) {
  await page.goto('/pos/pending')
}

async function waitForPosContext(page) {
  const response = await page.waitForResponse(
    (res) => res.url().includes('/api/pos/context') && res.ok(),
    { timeout: 30_000 },
  )
  return response.json()
}

export async function openSellerSession(page) {
  const contextPromise = waitForPosContext(page)
  await goToPosSeller(page)
  const ctx = await contextPromise
  if (ctx.session?.sessionType === 'SALES' || await page.locator('#pos-search').isVisible()) {
    return
  }
  const openResponse = page.waitForResponse(
    (res) => res.url().includes('/api/pos/sessions/open') && res.request().method() === 'POST' && res.ok(),
    { timeout: 30_000 },
  )
  await page.getByRole('button', { name: /Ouvrir session vente/i }).click()
  await openResponse
  await expect(page.locator('#pos-search')).toBeVisible({ timeout: 30_000 })
}

export async function openCashierSession(page) {
  const contextPromise = waitForPosContext(page)
  await goToPosCashier(page)
  const ctx = await contextPromise
  if (ctx.session?.sessionType === 'CASHIER') {
    await expect(page.getByRole('heading', { name: 'Encaissement' })).toBeVisible({ timeout: 15_000 })
    return
  }
  await page.getByRole('button', { name: 'Ouvrir session caisse' }).click()
  await expect(page.getByRole('button', { name: 'Ouvrir la caisse' })).toBeVisible()
  const openResponse = page.waitForResponse(
    (res) => res.url().includes('/api/pos/sessions/open') && res.request().method() === 'POST' && res.ok(),
    { timeout: 30_000 },
  )
  await page.getByRole('button', { name: 'Ouvrir la caisse' }).click()
  await openResponse
  await expect(page.getByRole('heading', { name: 'Encaissement' })).toBeVisible({ timeout: 30_000 })
}

export async function collectPendingPayment(page, saleNumber) {
  await openCashierSession(page)
  const row = page.locator('table tbody tr').filter({ hasText: saleNumber })
  await expect(row).toBeVisible({ timeout: 30_000 })
  await row.getByRole('button', { name: 'Encaisser' }).click()
  await expect(page.getByRole('heading', { name: /Encaisser —/ })).toBeVisible()
  const validateResponse = page.waitForResponse(
    (res) => res.url().includes('/validate') && res.request().method() === 'POST' && res.ok(),
  )
  await page.getByRole('button', { name: 'Valider paiement' }).click()
  await validateResponse
}

async function pickPosModalOption(page) {
  const modal = page.locator('[role="presentation"]').filter({ hasText: 'Choisissez le conditionnement' })
  if (await modal.isVisible({ timeout: 2_000 }).catch(() => false)) {
    await modal.getByRole('button').filter({ hasNotText: 'Annuler' }).first().click()
    return
  }
  const variantModal = page.locator('[role="presentation"]').filter({ hasText: 'Choisissez la variante' })
  if (await variantModal.isVisible({ timeout: 1_000 }).catch(() => false)) {
    await variantModal.getByRole('button').filter({ hasNotText: 'Annuler' }).first().click()
  }
}

export async function addProductToCart(page, productName) {
  const catalogButton = page.locator('main').getByRole('button').filter({ hasText: productName }).first()
  await expect(catalogButton).toBeVisible({ timeout: 20_000 })

  const lineResponse = page.waitForResponse(
    (res) => /\/api\/pos\/sales\/\d+\/lines/.test(res.url()) && res.request().method() === 'POST' && res.ok(),
    { timeout: 20_000 },
  )
  await catalogButton.click()
  await pickPosModalOption(page)
  await lineResponse
  await expect(cartAside(page)).not.toContainText('Panier vide')
}

export async function sendCartToCashier(page) {
  const response = page.waitForResponse(
    (res) => res.url().includes('/send-to-payment') && res.request().method() === 'POST' && res.ok(),
  )
  await page.getByRole('button', { name: /Envoyer à la caisse/ }).click()
  await response
  await expect(cartAside(page)).toContainText('Panier vide')
}

export async function pauseClientCart(page) {
  const response = page.waitForResponse(
    (res) => res.url().includes('/hold') && res.request().method() === 'POST' && res.ok(),
  )
  await page.getByRole('button', { name: /Pause client/ }).click()
  await response
  await expect(cartAside(page)).toContainText('Panier vide')
}

export async function resumeLatestPausedSale(page) {
  await page.getByRole('button', { name: /Reprendre \(F9\)/ }).click()
  const modal = page.locator('[role="presentation"]').filter({ has: page.getByRole('heading', { name: /Ventes en attente/ }) })
  await expect(modal).toBeVisible()
  const holdSection = modal.getByText(/En pause \/ retour caisse/).locator('..')
  await holdSection.getByRole('button').first().click()
  await expect(cartAside(page)).not.toContainText('Panier vide')
}

export async function collectFirstPendingPayment(page) {
  await openCashierSession(page)
  const row = page.locator('table tbody tr').first()
  await expect(row).toBeVisible({ timeout: 30_000 })
  const saleNumber = await row.locator('td').first().textContent()
  await collectPendingPayment(page, saleNumber.trim())
}

export async function expectSaleNotInPendingQueue(page, saleNumber) {
  await openCashierSession(page)
  await expect(page.locator('table tbody tr').filter({ hasText: saleNumber })).toHaveCount(0)
}
