import { expect } from '@playwright/test'
import { uniqueName } from './test-data.js'

export async function goToCustomers(page) {
  await page.goto('/customers')
  await expect(page.getByRole('heading', { name: 'Clients' })).toBeVisible()
}

function customerForm(page) {
  return page.locator('h3').filter({ hasText: /Nouveau client|Modifier client/ }).locator('..')
}

export async function createCustomer(page, {
  firstName = 'TEST',
  lastName = uniqueName('Client'),
  phone = '0612345678',
  email = `test-client-${Date.now()}@erp.local`,
} = {}) {
  await goToCustomers(page)
  const form = customerForm(page)
  await form.getByRole('textbox', { name: 'Prénom *' }).fill(firstName)
  await form.getByRole('textbox', { name: 'Nom *', exact: true }).fill(lastName)
  await form.getByPlaceholder('Téléphone').fill(phone)
  await form.getByPlaceholder('Email').fill(email)
  const responsePromise = page.waitForResponse(
    (res) => res.url().includes('/api/customers') && res.request().method() === 'POST',
  )
  await form.getByRole('button', { name: 'Créer' }).click()
  const response = await responsePromise
  expect(response.ok()).toBeTruthy()
  const fullName = `${firstName} ${lastName}`
  await expect(page.locator('table tbody tr').filter({ hasText: fullName })).toBeVisible()
  return { firstName, lastName, fullName, phone, email }
}

export async function searchCustomer(page, query) {
  await page.getByPlaceholder(/Rechercher un client/).fill(query)
  const response = page.waitForResponse(
    (res) => res.url().includes('/api/customers') && res.ok(),
  )
  await page.getByRole('button', { name: 'Rechercher' }).click()
  await response
}

export async function openCustomerSheet(page, fullName) {
  const row = page.locator('table tbody tr').filter({ hasText: fullName })
  const href = await row.getByRole('link', { name: 'Fiche' }).getAttribute('href')
  await page.goto(href)
  await expect(page.getByRole('heading', { level: 2, name: fullName })).toBeVisible({ timeout: 30_000 })
}

export async function editCustomerFromList(page, fullName, newPhone) {
  const row = page.locator('table tbody tr').filter({ hasText: fullName })
  await row.getByRole('button', { name: 'Modifier' }).click()
  const form = customerForm(page)
  await form.getByPlaceholder('Téléphone').fill(newPhone)
  const response = page.waitForResponse(
    (res) => res.url().includes('/api/customers/') && res.request().method() === 'PUT' && res.ok(),
  )
  await page.getByRole('button', { name: 'Enregistrer' }).click()
  await response
}
