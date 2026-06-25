import { expect } from '@playwright/test'
import { uniqueName } from './test-data.js'

export async function goToCategories(page) {
  await page.goto('/categories')
  await expect(page.getByRole('heading', { name: 'Catégories' })).toBeVisible()
}

export function categoryNode(page, name) {
  return page.locator('div.group').filter({ has: page.getByText(name, { exact: true }) })
}

export async function createRootCategory(page, name = uniqueName('TEST-Cat')) {
  await goToCategories(page)
  await page.getByPlaceholder('Nouvelle catégorie racine').fill(name)
  const response = page.waitForResponse(
    (res) => res.url().includes('/api/categories') && res.request().method() === 'POST' && res.ok(),
  )
  await page.getByRole('button', { name: 'Créer' }).click()
  await response
  await expect(categoryNode(page, name)).toBeVisible()
  return name
}

export async function addChildCategory(page, parentName, childName = uniqueName('TEST-SubCat')) {
  const node = categoryNode(page, parentName)
  const wrapper = node.locator('xpath=..')
  await node.hover()
  await node.getByText('+ Sous-cat.').click()
  await expect(wrapper.getByPlaceholder('Nom sous-catégorie')).toBeVisible()
  await wrapper.getByPlaceholder('Nom sous-catégorie').fill(childName)
  const response = page.waitForResponse(
    (res) => res.url().includes('/api/categories') && res.request().method() === 'POST' && res.ok(),
  )
  await wrapper.getByRole('button', { name: 'Ajouter' }).click()
  await response
  await expect(categoryNode(page, childName)).toBeVisible()
  return childName
}

export async function renameCategory(page, currentName, newName) {
  const node = categoryNode(page, currentName)
  await node.hover()
  await node.getByText('Modifier').click({ force: true })
  const input = page.locator('div.group input').first()
  await input.fill(newName)
  const response = page.waitForResponse(
    (res) => res.url().includes('/api/categories/') && res.request().method() === 'PUT' && res.ok(),
  )
  await page.getByRole('button', { name: 'OK' }).click()
  await response
  await expect(categoryNode(page, newName)).toBeVisible()
  return newName
}

export async function deleteCategory(page, name) {
  const node = categoryNode(page, name)
  page.once('dialog', (dialog) => dialog.accept())
  await node.hover()
  const response = page.waitForResponse(
    (res) => res.url().includes('/api/categories/') && res.request().method() === 'DELETE',
  )
  await node.getByText('Suppr.').click()
  await response
  await expect(categoryNode(page, name)).toHaveCount(0)
}
