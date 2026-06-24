import { test, expect } from '@playwright/test'

test('page de connexion accessible', async ({ page }) => {
  await page.goto('/login')
  await expect(page.getByRole('heading', { name: 'ERP Produits' })).toBeVisible()
  await expect(page.getByRole('button', { name: 'Se connecter' })).toBeVisible()
})
