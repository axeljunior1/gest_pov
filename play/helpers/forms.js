/**
 * Champ texte associé à un <label> frère (pattern ProductDetailPage).
 */
export function inputByLabel(page, label) {
  return page.locator('label').filter({ hasText: new RegExp(`^${label}$`) }).locator('xpath=following-sibling::input[1]')
}

export function selectByLabel(page, label) {
  return page.locator('label').filter({ hasText: label }).locator('xpath=following-sibling::select[1]')
}

export function textareaByLabel(page, label) {
  return page.locator('label').filter({ hasText: new RegExp(`^${label}$`) }).locator('xpath=following-sibling::textarea[1]')
}
