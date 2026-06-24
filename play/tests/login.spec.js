import { test, expect } from '@playwright/test'
import { defaultAdmin, loginAsAdmin, loginAsCashier, logout } from '../helpers/auth.js'

test.describe('Connexion', () => {
  test('admin peut se connecter et accéder au tableau de bord', async ({ page }) => {
    await loginAsAdmin(page)

    await expect(page).toHaveURL(/\/dashboard/)
    await expect(page.getByRole('heading', { name: 'Tableau de bord' })).toBeVisible()
  })

  test('caissier est redirigé vers les paiements en attente', async ({ page }) => {
    await loginAsCashier(page)

    await expect(page).toHaveURL(/\/pos\/pending/)
    await expect(page.getByRole('heading', { name: /Encaissement|Poste encaissement/ })).toBeVisible()
  })

  test('déconnexion ramène à la page de connexion', async ({ page }) => {
    await loginAsAdmin(page)
    await logout(page)
  })

  test('refuse un mot de passe incorrect', async ({ page }) => {
    await page.goto('/login')
    await page.locator('#email').waitFor({ state: 'visible', timeout: 60_000 })
    await page.locator('#email').fill(defaultAdmin.email)
    await page.locator('#password').fill('mauvais-mot-de-passe')
    await page.getByRole('button', { name: 'Se connecter' }).click()

    await expect(page).toHaveURL(/\/login/)
    await expect(page.locator('form').getByRole('alert')).toContainText('Email ou mot de passe incorrect')
    await expect(page.getByRole('heading', { name: 'ERP Produits' })).toBeVisible()
  })
})
