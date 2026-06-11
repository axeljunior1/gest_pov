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

export function buildProductPayload(form, { isNew = false } = {}) {
  const payload = {
    nom: form.nom?.trim(),
    sku: form.sku?.trim(),
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
    utilisateur: form.utilisateur || 'Admin',
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
      .filter((v) => v.sku?.trim())
      .map((v) => {
        const variant = {
          couleur: v.couleur?.trim() || null,
          taille: v.taille?.trim() || null,
          sku: v.sku.trim(),
          prix: toNumber(v.prix),
          stock: Number(v.stock) || 0,
        }
        if (v.generateBarcode) variant.generateBarcode = true
        if (isValidBarcodeType(v.barcodeType)) variant.barcodeType = v.barcodeType
        if (v.codeBarre?.trim()) variant.codeBarre = v.codeBarre.trim()
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
    sku: variant.sku?.trim(),
    prix: toNumber(variant.prix),
    stock: Number(variant.stock) || 0,
  }
  if (variant.generateBarcode) payload.generateBarcode = true
  if (isValidBarcodeType(variant.barcodeType)) payload.barcodeType = variant.barcodeType
  if (variant.codeBarre?.trim()) payload.codeBarre = variant.codeBarre.trim()
  return payload
}
