import { expect } from '@playwright/test'
import { defaultAdmin } from './auth.js'
import { uniqueName } from './test-data.js'

const API_BASE = process.env.PLAYWRIGHT_API_URL || 'http://localhost:8080'

async function adminAuthHeaders(request) {
  const loginRes = await request.post(`${API_BASE}/api/auth/login`, {
    data: { email: defaultAdmin.email, password: defaultAdmin.password },
  })
  expect(loginRes.ok()).toBeTruthy()
  const { token } = await loginRes.json()
  return { Authorization: `Bearer ${token}` }
}

/** Libère les slots licence en supprimant les comptes créés par les tests E2E. */
export async function cleanupTestUsers(request) {
  const headers = await adminAuthHeaders(request)
  const listRes = await request.get(`${API_BASE}/api/users`, { headers })
  expect(listRes.ok()).toBeTruthy()
  const users = await listRes.json()
  for (const user of users) {
    if (typeof user.email === 'string' && user.email.startsWith('test-pw-user-') && user.email.endsWith('@erp.local')) {
      const delRes = await request.delete(`${API_BASE}/api/users/${user.id}`, { headers })
      expect(delRes.ok() || delRes.status() === 204).toBeTruthy()
    }
  }
}

export async function goToUsers(page) {
  await page.goto('/users')
  await expect(page.getByRole('heading', { name: 'Utilisateurs' })).toBeVisible()
}

function userForm(page) {
  return page.locator('h3').filter({ hasText: /Nouvel utilisateur|Modifier.*utilisateur/ }).locator('..')
}

export async function createUser(page, {
  firstName = 'TEST',
  lastName = uniqueName('User'),
  email = `test-pw-user-${Date.now()}-${Math.random().toString(36).slice(2, 7)}@erp.local`,
  password = 'TestUser2026!',
  roleName = 'Caissier',
} = {}) {
  await goToUsers(page)
  const form = userForm(page)
  await form.getByRole('textbox', { name: 'Prénom *' }).fill(firstName)
  await form.getByRole('textbox', { name: 'Nom *', exact: true }).fill(lastName)
  await form.getByPlaceholder('Email *').fill(email)
  await form.getByPlaceholder('Mot de passe *').fill(password)
  await form.locator('label').filter({ hasText: roleName }).locator('input[type="checkbox"]').check()
  const responsePromise = page.waitForResponse(
    (res) => res.url().includes('/api/users') && res.request().method() === 'POST',
  )
  await form.getByRole('button', { name: 'Créer' }).click()
  const response = await responsePromise
  expect(response.ok()).toBeTruthy()
  await expect(page.locator('table tbody tr').filter({ hasText: email })).toBeVisible()
  return { firstName, lastName, email, password, fullName: `${firstName} ${lastName}` }
}

export async function updateUser(page, email, { firstName, lastName } = {}) {
  const row = page.locator('table tbody tr').filter({ hasText: email })
  await row.getByRole('button', { name: 'Modifier' }).click()
  const form = userForm(page)
  if (firstName) await form.getByRole('textbox', { name: 'Prénom *' }).fill(firstName)
  if (lastName) await form.getByRole('textbox', { name: 'Nom *', exact: true }).fill(lastName)
  const response = page.waitForResponse(
    (res) => res.url().includes('/api/users/') && res.request().method() === 'PUT' && res.ok(),
  )
  await page.getByRole('button', { name: 'Mettre à jour' }).click()
  await response
}

export async function deleteUser(page, email) {
  const row = page.locator('table tbody tr').filter({ hasText: email })
  page.once('dialog', (dialog) => dialog.accept())
  const response = page.waitForResponse(
    (res) => res.url().includes('/api/users/') && res.request().method() === 'DELETE',
  )
  await row.getByRole('button', { name: 'Suppr.' }).click()
  await response
  await expect(page.locator('table tbody tr').filter({ hasText: email })).toHaveCount(0)
}
