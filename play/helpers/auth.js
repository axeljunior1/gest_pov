import { expect } from '@playwright/test'

/**
 * Comptes seed dev — surcharge via variables d'environnement.
 */
export const defaultAdmin = {
  email: process.env.PLAYWRIGHT_ADMIN_EMAIL || 'admin@erp.local',
  password: process.env.PLAYWRIGHT_ADMIN_PASSWORD || 'ErpAdmin2026!',
}

export const defaultCashier = {
  email: process.env.PLAYWRIGHT_CASHIER_EMAIL || 'caissier@erp.local',
  password: process.env.PLAYWRIGHT_CASHIER_PASSWORD || 'Caissier2026!',
}

export const defaultSeller = {
  email: process.env.PLAYWRIGHT_SELLER_EMAIL || 'vendeur@erp.local',
  password: process.env.PLAYWRIGHT_SELLER_PASSWORD || 'Vendeur2026!',
}

export async function login(page, { email, password } = defaultAdmin) {
  await page.goto('/login')
  const emailField = page.locator('#email')
  await emailField.waitFor({ state: 'visible', timeout: 60_000 })
  await emailField.fill(email)
  await page.locator('#password').fill(password)
  await page.getByRole('button', { name: 'Se connecter' }).click()
  await page.waitForURL((url) => !url.pathname.endsWith('/login'), { timeout: 30_000 })
}

export async function loginAsAdmin(page) {
  return login(page, defaultAdmin)
}

export async function loginAsCashier(page) {
  return login(page, defaultCashier)
}

export async function loginAsSeller(page) {
  return login(page, defaultSeller)
}

export async function logout(page) {
  await page.getByRole('button', { name: 'Déconnexion' }).click()
  await page.waitForURL(/\/login/, { timeout: 15_000 })
  await expect(page.getByRole('heading', { name: 'ERP Produits' })).toBeVisible()
}
