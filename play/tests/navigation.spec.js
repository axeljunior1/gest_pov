import { test, expect } from '@playwright/test'
import { loginAsAdmin } from '../helpers/auth.js'

const ADMIN_NAV_LINKS = [
  { name: 'Tableau de bord' },
  { name: 'Caisse POS' },
  { name: 'Produits' },
  { name: 'Consultation', exact: true },
]

test.describe('Navigation', () => {
  test('admin voit les entrées principales du menu', async ({ page }) => {
    await loginAsAdmin(page)

    for (const link of ADMIN_NAV_LINKS) {
      await expect(page.getByRole('link', link)).toBeVisible()
    }
  })

  test('admin accède au catalogue via le menu Produits', async ({ page }) => {
    await loginAsAdmin(page)

    await page.getByRole('link', { name: 'Produits' }).click()
    await expect(page).toHaveURL('/')
    await expect(page.getByRole('heading', { name: 'Produits' })).toBeVisible()
  })

  test('admin accède au stock via le menu Consultation', async ({ page }) => {
    await loginAsAdmin(page)

    await page.getByRole('link', { name: 'Consultation', exact: true }).click()
    await expect(page).toHaveURL('/stock')
    await expect(page.getByRole('heading', { name: 'Gestion de stock' })).toBeVisible()
  })
})
