/** Libellé complet d'une ligne de vente POS (produit + variante + conditionnement). */
export function formatPosSaleLineLabel(line) {
  if (!line) return ''
  const name = line.productNom || line.designation || ''
  const variant = line.variantNameSnapshot?.trim()
  if (variant && !name.includes(variant)) {
    return `${name} — ${variant}`
  }
  return name
}

export function formatPosSaleLineParts(line) {
  if (!line) {
    return { productNom: '', variantName: null, packagingName: null }
  }
  return {
    productNom: line.productNom || '',
    variantName: line.variantNameSnapshot?.trim() || null,
    packagingName: line.packagingNameSnapshot?.trim() || null,
  }
}
