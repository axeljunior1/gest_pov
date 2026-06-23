/** Critères de recherche affichés par type d'entité (alignés sur les APIs / filtres réels). */

export const ENTITY_SEARCH_CONFIG = {
  product: {
    label: 'produit',
    placeholder: 'Rechercher un produit (nom, SKU, code-barres, EAN13, catégorie…)',
    criteria: ['Nom produit', 'SKU', 'Code-barres', 'EAN13', 'Catégorie', 'Marque'],
  },
  customer: {
    label: 'client',
    placeholder: 'Rechercher un client (nom, téléphone, email…)',
    criteria: ['Nom', 'Prénom', 'Téléphone', 'Email', 'Référence client'],
  },
  supplier: {
    label: 'fournisseur',
    placeholder: 'Rechercher un fournisseur (nom, téléphone, ville…)',
    criteria: ['Raison sociale', 'Téléphone', 'Email', 'Adresse / ville'],
  },
  user: {
    label: 'utilisateur',
    placeholder: 'Rechercher un utilisateur (nom, email…)',
    criteria: ['Nom', 'Prénom', 'Email'],
  },
  category: {
    label: 'catégorie',
    placeholder: 'Rechercher une catégorie (nom, parent…)',
    criteria: ['Nom catégorie', 'Catégorie parente'],
  },
  brand: {
    label: 'marque',
    placeholder: 'Rechercher une marque (nom…)',
    criteria: ['Nom marque'],
  },
}

export function getEntitySearchConfig(entityType) {
  return ENTITY_SEARCH_CONFIG[entityType] ?? {
    label: 'élément',
    placeholder: 'Rechercher…',
    criteria: [],
  }
}
