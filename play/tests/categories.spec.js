import { test, expect } from '@playwright/test'
import { loginAsAdmin } from '../helpers/auth.js'
import { uniqueName } from '../helpers/test-data.js'
import {
  addChildCategory,
  createRootCategory,
  deleteCategory,
  goToCategories,
  renameCategory,
} from '../helpers/categories.js'

test.describe('Catégories', () => {
  test('crée une catégorie racine TEST', async ({ page }) => {
    await loginAsAdmin(page)
    await createRootCategory(page, uniqueName('TEST-Cat'))
  })

  test('ajoute une sous-catégorie', async ({ page }) => {
    const parent = uniqueName('TEST-Cat-Parent')
    const child = uniqueName('TEST-SubCat')
    await loginAsAdmin(page)
    await createRootCategory(page, parent)
    await addChildCategory(page, parent, child)
  })

  test('renomme une catégorie', async ({ page }) => {
    const initial = uniqueName('TEST-Cat-Rename')
    const renamed = uniqueName('TEST-Cat-Renamed')
    await loginAsAdmin(page)
    await createRootCategory(page, initial)
    await renameCategory(page, initial, renamed)
  })

  test('supprime une catégorie vide', async ({ page }) => {
    const name = uniqueName('TEST-Cat-Del')
    await loginAsAdmin(page)
    await createRootCategory(page, name)
    await deleteCategory(page, name)
    await goToCategories(page)
    await expect(page.getByText(name, { exact: true })).toHaveCount(0)
  })
})
