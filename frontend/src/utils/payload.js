import { BARCODE_TYPES } from './constants'

const toNumber = (value) => {
  if (value === '' || value == null) return null
  const n = Number(value)
  return Number.isFinite(n) ? n : null
}

const toId = (value) => {
  if (value === '' || value == null) return null
  const n = Number(value)
  return Number.isFinite(n) ? n : null
}

const isValidBarcodeType = (value) => BARCODE_TYPES.includes(value)

export function buildProductPayload(form, { isNew = false, hasVariants = false } = {}) {
  const payload = {
    nom: form.nom?.trim(),
    description: form.description?.trim() || null,
    marque: form.marque?.trim() || null,
    categorieId: toId(form.categorieId),
    fournisseurPrincipalId: toId(form.fournisseurPrincipalId),
    unitId: toId(form.unitId),
    prixAchat: toNumber(form.prixAchat),
    prixVente: toNumber(form.prixVente),
    prixPromotionnel: toNumber(form.prixPromotionnel),
    statut: form.statut,
    cycleVie: form.cycleVie,
  }

  if (form.sku?.trim()) {
    payload.sku = form.sku.trim()
  }

  const hasInitialVariants = isNew && Array.isArray(form.variantes)
    && form.variantes.some((v) => v.sku?.trim() || v.couleur?.trim() || v.taille?.trim() || toNumber(v.prix) != null)

  if (!hasVariants && !hasInitialVariants) {
    if (form.codeBarre?.trim()) {
      payload.codeBarre = form.codeBarre.trim()
    } else if (isNew || form.generateBarcode) {
      payload.generateBarcode = true
    }
  }

  const attributs = form.attributs || {}
  const filledAttributs = Object.fromEntries(
    Object.entries(attributs).filter(([, value]) => value != null && String(value).trim() !== ''),
  )
  if (Object.keys(filledAttributs).length > 0) {
    payload.attributs = filledAttributs
  }

  if (isNew && Array.isArray(form.variantes)) {
    const variantes = form.variantes
      .filter((v) => v.sku?.trim() || v.couleur?.trim() || v.taille?.trim() || toNumber(v.prix) != null)
      .map((v) => {
        const variant = {
          couleur: v.couleur?.trim() || null,
          taille: v.taille?.trim() || null,
          prix: toNumber(v.prix),
          stock: Number(v.stock) || 0,
        }
        if (v.sku?.trim()) variant.sku = v.sku.trim()
        if (v.codeBarre?.trim()) variant.codeBarre = v.codeBarre.trim()
        if (v.generateBarcode !== false && !v.codeBarre?.trim()) variant.generateBarcode = true
        else if (v.generateBarcode) variant.generateBarcode = true
        if (isValidBarcodeType(v.barcodeType)) variant.barcodeType = v.barcodeType
        else if (variant.generateBarcode) variant.barcodeType = 'EAN13'
        return variant
      })

    if (variantes.length > 0) {
      payload.variantes = variantes
    }
  }

  return payload
}

export function buildVariantPayload(variant) {
  const payload = {
    couleur: variant.couleur?.trim() || null,
    taille: variant.taille?.trim() || null,
    prix: toNumber(variant.prix),
    stock: Number(variant.stock) || 0,
    sellable: variant.sellable !== false,
    stockable: variant.stockable !== false,
    active: variant.active !== false,
  }
  if (variant.costPrice != null && variant.costPrice !== '') {
    payload.costPrice = toNumber(variant.costPrice)
  }
  if (variant.sku?.trim()) payload.sku = variant.sku.trim()
  if (variant.codeBarre?.trim()) payload.codeBarre = variant.codeBarre.trim()
  if (variant.generateBarcode !== false && !variant.codeBarre?.trim()) payload.generateBarcode = true
  else if (variant.generateBarcode) payload.generateBarcode = true
  if (isValidBarcodeType(variant.barcodeType)) payload.barcodeType = variant.barcodeType
  else if (payload.generateBarcode) payload.barcodeType = 'EAN13'
  return payload
}
