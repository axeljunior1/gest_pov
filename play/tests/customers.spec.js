import { test, expect } from '@playwright/test'
import { loginAsAdmin } from '../helpers/auth.js'
import { uniqueName } from '../helpers/test-data.js'
import {
  createCustomer,
  editCustomerFromList,
  goToCustomers,
  openCustomerSheet,
  searchCustomer,
} from '../helpers/customers.js'

test.describe('Clients', () => {
  test('crée un client TEST', async ({ page }) => {
    await loginAsAdmin(page)
    await createCustomer(page)
  })

  test('recherche et modifie un client', async ({ page }) => {
    await loginAsAdmin(page)
    const customer = await createCustomer(page, { lastName: uniqueName('Client-Edit') })
    await searchCustomer(page, customer.lastName)
    await editCustomerFromList(page, customer.fullName, '0699887766')
    await expect(page.locator('table tbody tr').filter({ hasText: customer.fullName })).toContainText('0699887766')
  })

  test('ouvre la fiche client', async ({ page }) => {
    await loginAsAdmin(page)
    const customer = await createCustomer(page, { lastName: uniqueName('Client-Fiche') })
    await goToCustomers(page)
    await openCustomerSheet(page, customer.fullName)
    await expect(page.getByText('Historique fidélité')).toBeVisible()
  })
})
