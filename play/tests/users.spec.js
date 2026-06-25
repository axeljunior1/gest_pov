import { test, expect } from '@playwright/test'
import { loginAsAdmin } from '../helpers/auth.js'
import { uniqueName } from '../helpers/test-data.js'
import {
  cleanupTestUsers,
  createUser,
  deleteUser,
  goToUsers,
  updateUser,
} from '../helpers/users.js'

test.describe.configure({ mode: 'serial' })

test.describe('Utilisateurs', () => {
  test.beforeAll(async ({ request }) => {
    await cleanupTestUsers(request)
  })

  test.afterAll(async ({ request }) => {
    await cleanupTestUsers(request)
  })
  test('crée un utilisateur TEST avec rôle Caissier', async ({ page }) => {
    await loginAsAdmin(page)
    await createUser(page, { lastName: uniqueName('User-Create') })
  })

  test('modifie un utilisateur', async ({ page }) => {
    await loginAsAdmin(page)
    const user = await createUser(page, { lastName: uniqueName('User-Edit') })
    await updateUser(page, user.email, { firstName: 'Modifié' })
    await expect(page.locator('table tbody tr').filter({ hasText: user.email })).toContainText('Modifié')
  })

  test('supprime un utilisateur TEST', async ({ page }) => {
    await loginAsAdmin(page)
    const user = await createUser(page, { lastName: uniqueName('User-Del') })
    await deleteUser(page, user.email)
    await goToUsers(page)
    await expect(page.locator('table tbody tr').filter({ hasText: user.email })).toHaveCount(0)
  })
})
