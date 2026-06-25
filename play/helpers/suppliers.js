import { expect } from '@playwright/test'
import { uniqueName } from './test-data.js'

export async function goToSuppliers(page) {
  await page.goto('/suppliers')
  await expect(page.getByRole('heading', { name: 'Fournisseurs' })).toBeVisible()
}

function supplierForm(page) {
  return page.locator('h3').filter({ hasText: /Nouveau fournisseur|Modifier/ }).locator('..')
}

export async function createSupplier(page, {
  nom = uniqueName('TEST-Fournisseur'),
  email = `test-fournisseur-${Date.now()}@erp.local`,
  telephone = '0600000000',
} = {}) {
  await goToSuppliers(page)
  const form = supplierForm(page)
  await form.getByPlaceholder('Nom *').fill(nom)
  await form.getByPlaceholder('Email').fill(email)
  await form.getByPlaceholder('Téléphone').fill(telephone)
  const response = page.waitForResponse(
    (res) => res.url().includes('/api/suppliers') && res.request().method() === 'POST' && res.ok(),
  )
  await page.getByRole('button', { name: 'Créer' }).click()
  await response
  await expect(page.locator('table tbody tr').filter({ hasText: nom })).toBeVisible()
  return { nom, email, telephone }
}

export async function updateSupplier(page, nom, updates) {
  const row = page.locator('table tbody tr').filter({ hasText: nom })
  await row.getByRole('button', { name: 'Modifier' }).click()
  const form = supplierForm(page)
  if (updates.email) await form.getByPlaceholder('Email').fill(updates.email)
  if (updates.telephone) await form.getByPlaceholder('Téléphone').fill(updates.telephone)
  const response = page.waitForResponse(
    (res) => res.url().includes('/api/suppliers/') && res.request().method() === 'PUT' && res.ok(),
  )
  await page.getByRole('button', { name: 'Mettre à jour' }).click()
  await response
}

export async function deleteSupplier(page, nom) {
  const row = page.locator('table tbody tr').filter({ hasText: nom })
  page.once('dialog', (dialog) => dialog.accept())
  const response = page.waitForResponse(
    (res) => res.url().includes('/api/suppliers/') && res.request().method() === 'DELETE',
  )
  await row.getByRole('button', { name: 'Suppr.' }).click()
  await response
  await expect(page.locator('table tbody tr').filter({ hasText: nom })).toHaveCount(0)
}
