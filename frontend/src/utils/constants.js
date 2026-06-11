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

export const formatPrice = (value) =>
  value != null ? `${Number(value).toFixed(2)} €` : '—'

export const formatDate = (value) =>
  value ? new Date(value).toLocaleDateString('fr-FR') : '—'
