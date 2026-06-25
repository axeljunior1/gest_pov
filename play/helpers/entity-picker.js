/**
 * Sélection dans un SmartEntitySelector / SearchableEntityPicker (variant compact ou default).
 */
export async function pickEntityInContainer(container, searchText) {
  const searchInput = container.locator('input[type="search"]')
  await searchInput.click()
  await searchInput.fill(searchText)
  await searchInput.press('ArrowDown')
  const option = container.locator('[role="listbox"] [role="option"]').filter({ hasText: searchText }).first()
  await option.waitFor({ state: 'visible', timeout: 15_000 })
  await option.click()
}
