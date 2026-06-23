/** Normalisation pour recherche insensible à la casse et aux accents. */
export function normalizeForSearch(value) {
  return (value ?? '')
    .toString()
    .toLowerCase()
    .normalize('NFD')
    .replace(/\p{M}/gu, '')
    .trim()
}

export function matchesQuery(fieldValue, query) {
  if (fieldValue == null || fieldValue === '') return false
  const q = normalizeForSearch(query)
  if (!q) return false
  return normalizeForSearch(fieldValue).includes(q)
}

const PRODUCT_FIELDS = [
  { key: 'sku', label: 'SKU', get: (e) => e.sku },
  { key: 'codeBarre', label: 'Code-barres', get: (e) => e.codeBarre },
  { key: 'barcodes', label: 'EAN13', get: (e) => e.barcodes },
  { key: 'nom', label: 'Nom', get: (e) => e.nom },
  { key: 'categorie', label: 'Catégorie', get: (e) => e.categoryNom ?? e.categorieNom },
  { key: 'marque', label: 'Marque', get: (e) => e.marque ?? e.brandNom },
]

const CUSTOMER_FIELDS = [
  { key: 'customerNumber', label: 'Référence client', get: (e) => e.customerNumber },
  { key: 'lastName', label: 'Nom', get: (e) => e.lastName },
  { key: 'firstName', label: 'Prénom', get: (e) => e.firstName },
  { key: 'fullName', label: 'Nom', get: (e) => e.fullName },
  { key: 'phone', label: 'Téléphone', get: (e) => e.phone },
  { key: 'email', label: 'Email', get: (e) => e.email },
]

const SUPPLIER_FIELDS = [
  { key: 'nom', label: 'Raison sociale', get: (e) => e.nom },
  { key: 'telephone', label: 'Téléphone', get: (e) => e.telephone },
  { key: 'email', label: 'Email', get: (e) => e.email },
  { key: 'adresse', label: 'Adresse / ville', get: (e) => e.adresse },
]

const USER_FIELDS = [
  { key: 'lastName', label: 'Nom', get: (e) => e.lastName },
  { key: 'firstName', label: 'Prénom', get: (e) => e.firstName },
  { key: 'emailLogin', label: 'Email', get: (e) => e.email },
]

const CATEGORY_FIELDS = [
  { key: 'nom', label: 'Nom catégorie', get: (e) => e.nom },
  { key: 'parentNom', label: 'Catégorie parente', get: (e) => e.parentNom },
]

const BRAND_FIELDS = [
  { key: 'nom', label: 'Nom marque', get: (e) => e.nom },
]

const FIELD_MAP = {
  product: PRODUCT_FIELDS,
  customer: CUSTOMER_FIELDS,
  supplier: SUPPLIER_FIELDS,
  user: USER_FIELDS,
  category: CATEGORY_FIELDS,
  brand: BRAND_FIELDS,
}

/** Priorité des champs pour le premier match affiché. */
const FIELD_PRIORITY = {
  product: ['sku', 'codeBarre', 'barcodes', 'nom', 'categorie', 'marque'],
  customer: ['customerNumber', 'phone', 'email', 'fullName', 'lastName', 'firstName'],
  supplier: ['nom', 'telephone', 'email', 'adresse'],
  user: ['emailLogin', 'lastName', 'firstName'],
  category: ['nom', 'parentNom'],
  brand: ['nom'],
}

export function findEntityMatch(query, entity, entityType) {
  if (!query?.trim() || !entity) return null
  const fields = FIELD_MAP[entityType]
  if (!fields) return null

  const priority = FIELD_PRIORITY[entityType] ?? fields.map((f) => f.key)
  const byKey = Object.fromEntries(fields.map((f) => [f.key, f]))

  for (const key of priority) {
    const field = byKey[key]
    if (!field) continue
    const raw = field.get(entity)
    if (Array.isArray(raw)) {
      const hit = raw.find((v) => matchesQuery(v, query))
      if (hit) return { label: field.label, value: hit }
    } else if (matchesQuery(raw, query)) {
      return { label: field.label, value: raw }
    }
  }
  return null
}

export function filterEntities(entities, query, entityType) {
  const q = query?.trim()
  if (!q) return entities ?? []
  return (entities ?? []).filter((entity) => findEntityMatch(q, entity, entityType))
}

const POS_MATCH_LABELS = {
  EXACT_BARCODE: 'Code-barres / EAN13',
  EXACT_PACKAGING_BARCODE: 'Code-barres conditionnement',
  EXACT_SKU: 'SKU',
  TEXT: null,
}

export function resolvePosProductMatch(query, product, matchType) {
  if (matchType && POS_MATCH_LABELS[matchType]) {
    const label = POS_MATCH_LABELS[matchType]
    if (matchType === 'EXACT_SKU') {
      return { label, value: product.sku ?? query }
    }
    if (matchType === 'EXACT_BARCODE' || matchType === 'EXACT_PACKAGING_BARCODE') {
      const hit = (product.barcodes ?? []).find((b) => matchesQuery(b, query) || b === query)
      return { label, value: hit ?? query }
    }
  }
  return findEntityMatch(query, product, 'product')
}
