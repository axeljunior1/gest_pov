import { test, expect } from '@playwright/test'
import { loginAsAdmin } from '../helpers/auth.js'
import { uniqueName } from '../helpers/test-data.js'
import {
  createSupplier,
  deleteSupplier,
  goToSuppliers,
  updateSupplier,
} from '../helpers/suppliers.js'

test.describe('Fournisseurs', () => {
  test('crée un fournisseur TEST', async ({ page }) => {
    await loginAsAdmin(page)
    await createSupplier(page)
  })

  test('modifie un fournisseur', async ({ page }) => {
    await loginAsAdmin(page)
    const { nom } = await createSupplier(page, { nom: uniqueName('TEST-Fournisseur') })
    await updateSupplier(page, nom, { telephone: '0700000001' })
    await expect(page.locator('table tbody tr').filter({ hasText: nom })).toContainText('0700000001')
  })

  test('supprime un fournisseur TEST', async ({ page }) => {
    await loginAsAdmin(page)
    const { nom } = await createSupplier(page, { nom: uniqueName('TEST-Fournisseur-Del') })
    await deleteSupplier(page, nom)
  })
})
