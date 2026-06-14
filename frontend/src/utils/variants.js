export function normalizeVariantText(value) {
  if (value == null) return null
  const trimmed = String(value).trim()
  return trimmed === '' ? null : trimmed.toLowerCase()
}

export function variantIdentityKey(variant) {
  return `${normalizeVariantText(variant.couleur) ?? ''}|${normalizeVariantText(variant.taille) ?? ''}`
}

export function hasVariantIdentityConflict(candidate, existingList) {
  const key = variantIdentityKey(candidate)
  if (key === '|') return false
  return (existingList || []).some((v) => variantIdentityKey(v) === key)
}

export function duplicateVariantForm(source) {
  return {
    couleur: source.couleur || '',
    taille: source.taille || '',
    sku: '',
    prix: source.prix ?? '',
    stock: 0,
    sellable: source.sellable !== false,
    stockable: source.stockable !== false,
    active: source.active !== false,
    costPrice: source.costPrice ?? '',
    generateBarcode: true,
    barcodeType: source.barcodeType || 'EAN13',
    codeBarre: '',
  }
}

export function duplicatePackagingForm(source, defaultVariantId = '') {
  return {
    nom: source.nom ? `${source.nom} (copie)` : '',
    symbole: source.symbole || '',
    quantiteBase: source.quantiteBase != null ? String(source.quantiteBase) : '',
    prixVente: source.prixVente != null ? String(source.prixVente) : '',
    defaultVente: false,
    principal: false,
    variantId: source.variantId ? String(source.variantId) : defaultVariantId,
  }
}
