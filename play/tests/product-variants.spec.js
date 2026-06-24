import { test, expect } from '@playwright/test'
import { loginAsAdmin } from '../helpers/auth.js'
import { createProductViaUi } from '../helpers/products.js'

const COLORS = ['Rouge', 'Bleu']
const SIZES = ['S', 'M']

test.describe('Produits à variantes', () => {
  test('crée un produit avec 2 couleurs et 2 tailles au statut Actif', async ({ page }) => {
    const productName = `TEST-PW-VAR-${Date.now()}`
    const variants = COLORS.flatMap((couleur) =>
      SIZES.map((taille) => ({ couleur, taille })),
    )

    await loginAsAdmin(page)
    await createProductViaUi(page, {
      name: productName,
      prixVente: '25',
      variants,
    })

    await expect(page.locator('.flex.gap-2.mb-6').getByText('Actif', { exact: true })).toBeVisible()

    await page.getByRole('button', { name: 'Variantes' }).click()
    const variantRows = page.locator('table tbody tr').filter({ has: page.locator('td') })
    await expect(variantRows).toHaveCount(4)

    for (const couleur of COLORS) {
      await expect(page.locator('table tbody td').filter({ hasText: new RegExp(`^${couleur}$`) })).toHaveCount(2)
    }
    for (const taille of SIZES) {
      await expect(page.locator('table tbody td').filter({ hasText: new RegExp(`^${taille}$`) })).toHaveCount(2)
    }
  })
})
