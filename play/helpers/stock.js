import { expect } from '@playwright/test'
import { pickEntityInContainer } from './entity-picker.js'

export async function goToStockEntries(page) {
  await page.goto('/stock/entries')
  await expect(page.getByRole('heading', { name: 'Entrées de stock' })).toBeVisible()
}

export async function goToStockExits(page) {
  await page.goto('/stock/exits')
  await expect(page.getByRole('heading', { name: 'Sorties de stock' })).toBeVisible()
}

export async function goToStockConsultation(page) {
  await page.goto('/stock')
  await expect(page.getByRole('heading', { name: 'Gestion de stock' })).toBeVisible()
}

async function openNewEntryForm(page) {
  await goToStockEntries(page)
  const productsResponse = page.waitForResponse(
    (res) => res.url().includes('/api/products') && res.request().method() === 'GET' && res.ok(),
  )
  await page.reload()
  await productsResponse
  await page.getByRole('button', { name: 'Nouvelle entrée' }).last().click()
  await expect(page.getByRole('button', { name: 'Enregistrer brouillon' })).toBeVisible()
}

async function openNewExitForm(page) {
  await goToStockExits(page)
  const productsResponse = page.waitForResponse(
    (res) => res.url().includes('/api/products') && res.request().method() === 'GET' && res.ok(),
  )
  await page.reload()
  await productsResponse
  await page.getByRole('button', { name: 'Nouvelle sortie' }).last().click()
  await expect(page.getByRole('button', { name: 'Enregistrer brouillon' })).toBeVisible()
}

async function selectWarehouseAndLocation(page) {
  const warehouseSelect = page.locator('label').filter({ hasText: 'Entrepôt *' }).locator('..').locator('select')
  const locationsResponse = page.waitForResponse(
    (res) => /\/api\/warehouses\/\d+\/locations/.test(res.url()) && res.ok(),
  )
  await warehouseSelect.selectOption({ index: 1 })
  await locationsResponse
  const locationSelect = page.locator('label').filter({ hasText: 'Emplacement *' }).locator('..').locator('select')
  await locationSelect.selectOption({ index: 1 })
}

async function fillStockLine(page, productName, quantity, linesHeading = 'Lignes produits') {
  const linesBlock = linesHeading === 'Lignes produits'
    ? page.locator('div.border-t').filter({ has: page.getByRole('heading', { name: linesHeading }) })
    : page.locator('div.space-y-3').filter({ has: page.getByRole('heading', { name: linesHeading }) })
  const line = linesBlock.locator('.grid').first()
  await pickEntityInContainer(line, productName)
  await line.locator('input[type="number"]').fill(String(quantity))
}

export async function createAndValidateStockEntry(page, productName, quantity) {
  await openNewEntryForm(page)
  await selectWarehouseAndLocation(page)
  await fillStockLine(page, productName, quantity)
  const createResponse = page.waitForResponse(
    (res) => res.url().includes('/api/stock/entries') && res.request().method() === 'POST' && res.ok(),
  )
  await page.getByRole('button', { name: 'Enregistrer brouillon' }).click()
  await createResponse
  page.once('dialog', (dialog) => dialog.accept())
  const validateResponse = page.waitForResponse(
    (res) => res.url().includes('/api/stock/entries/') && res.url().includes('/validate') && res.ok(),
  )
  await page.getByRole('button', { name: 'Valider' }).click()
  await validateResponse
  await expect(page.getByText('VALIDATED')).toBeVisible()
}

export async function createAndValidateStockExit(page, productName, quantity) {
  await openNewExitForm(page)
  await selectWarehouseAndLocation(page)
  await fillStockLine(page, productName, quantity, 'Lignes')
  const createResponse = page.waitForResponse(
    (res) => res.url().includes('/api/stock/exits') && res.request().method() === 'POST' && res.ok(),
  )
  await page.getByRole('button', { name: 'Enregistrer brouillon' }).click()
  await createResponse
  page.once('dialog', (dialog) => dialog.accept())
  const validateResponse = page.waitForResponse(
    (res) => res.url().includes('/api/stock/exits/') && res.url().includes('/validate') && res.ok(),
  )
  await page.getByRole('button', { name: 'Valider' }).click()
  await validateResponse
  await expect(page.getByText('VALIDATED')).toBeVisible()
}

export async function expectStockQuantityForProduct(page, productName, quantity) {
  await goToStockConsultation(page)
  const row = page.locator('table tbody tr').filter({ hasText: productName })
  await expect(row).toBeVisible()
  await expect(row).toContainText(String(quantity))
}

export async function openStockMovementsTab(page) {
  await goToStockConsultation(page)
  await page.getByRole('button', { name: 'Historique' }).click()
}

export async function expectStockMovementForProduct(page, productName) {
  await openStockMovementsTab(page)
  await expect(page.locator('table tbody tr').filter({ hasText: productName }).first()).toBeVisible()
}
