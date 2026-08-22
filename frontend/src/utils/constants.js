export const PRODUCT_STATUS = ['ACTIF', 'INACTIF', 'ARCHIVE']
export const LIFECYCLE_STATUS = ['BROUILLON', 'EN_ATTENTE_VALIDATION', 'ACTIF', 'SUSPENDU', 'ARRETE', 'ARCHIVE']
export const PRICE_TYPES = ['ACHAT', 'VENTE', 'PROMOTIONNEL']
export const BARCODE_TYPES = ['EAN13', 'UPC', 'CODE128', 'QR_CODE']
export const DOCUMENT_TYPES = ['FICHE_TECHNIQUE', 'CERTIFICAT_QUALITE', 'NOTICE_UTILISATEUR', 'GARANTIE', 'AUTRE']

export const lifecycleLabel = {
  BROUILLON: 'Brouillon',
  EN_ATTENTE_VALIDATION: 'En attente validation',
  ACTIF: 'Actif',
  SUSPENDU: 'Suspendu',
  ARRETE: 'Arrêté',
  ARCHIVE: 'Archivé',
}

export const statusLabel = {
  ACTIF: 'Actif',
  INACTIF: 'Inactif',
  ARCHIVE: 'Archivé',
}

// Devise affichee, alimentee une fois au demarrage depuis /api/settings/public (voir AuthContext).
let currencyCode = 'XAF'

export function setCurrency(code) {
  if (code && typeof code === 'string' && code.trim()) {
    currencyCode = code.trim().toUpperCase()
  }
}

export function getCurrency() {
  return currencyCode
}

export const formatPrice = (value) => {
  if (value == null) return '—'
  try {
    return new Intl.NumberFormat('fr-FR', { style: 'currency', currency: currencyCode }).format(Number(value))
  } catch {
    return `${Number(value).toFixed(2)} ${currencyCode}`
  }
}

export const formatDate = (value) =>
  value ? new Date(value).toLocaleDateString('fr-FR') : '—'
