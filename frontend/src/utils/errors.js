/**
 * Gestion centralisée des erreurs API pour l'interface utilisateur.
 * Ne jamais afficher de messages techniques bruts (stack, codes HTTP seuls, etc.).
 */

export const SESSION_EXPIRED_KEY = 'erp_session_expired'
export const SESSION_EXPIRED_MESSAGE = 'Votre session a expiré. Veuillez vous reconnecter.'

const TECHNICAL_PATTERNS = [
  /^request failed with status code \d+$/i,
  /^network error$/i,
  /^timeout of \d+ms exceeded$/i,
  /^axios/i,
  /^internal server error$/i,
  /^unauthorized$/i,
  /^forbidden$/i,
  /^bad request$/i,
  /^not found$/i,
]

/** Correspondance exacte des messages backend (déjà en français ou à normaliser). */
const EXACT_MESSAGES = {
  'Authentification requise': 'Email ou mot de passe incorrect',
  'Acces refuse — permission insuffisante': 'Vous n\'avez pas l\'autorisation d\'effectuer cette action.',
  'Accès refusé — permission insuffisante': 'Vous n\'avez pas l\'autorisation d\'effectuer cette action.',
}

/** Correspondance par motif (message backend → message utilisateur). */
const PATTERN_MESSAGES = [
  { test: /stock insuffisant/i, map: (m) => m },
  { test: /stock disponible insuffisant/i, map: (m) => m },
  { test: /aucun produit trouvé pour ce code-barres/i, map: () => 'Aucun produit ne correspond à ce code-barres.' },
  { test: /code-barres requis/i, map: () => 'Scannez ou saisissez un code-barres.' },
  { test: /scan code-barres désactivé/i, map: () => 'Le scan code-barres est désactivé dans les paramètres.' },
  { test: /ce produit n'est pas vendable/i, map: () => 'Ce produit n\'est pas disponible à la vente.' },
  { test: /cette variante est inactive/i, map: () => 'Cette variante de produit est inactive.' },
  { test: /cette variante n'est pas vendable/i, map: () => 'Cette variante n\'est pas disponible à la vente.' },
  { test: /cette vente est deja payee/i, map: () => 'Cette vente est déjà payée.' },
  { test: /cette vente est déjà payée/i, map: () => 'Cette vente est déjà payée.' },
  { test: /montant paye insuffisant/i, map: () => 'Le montant reçu est insuffisant pour encaisser cette vente.' },
  { test: /montant payé insuffisant/i, map: () => 'Le montant reçu est insuffisant pour encaisser cette vente.' },
  { test: /moyen de paiement non autorise/i, map: (m) => m.replace(/Moyen de paiement non autorise:?\s*/i, 'Moyen de paiement non autorisé : ') },
  { test: /paiement fractionne desactive/i, map: () => 'Le paiement fractionné n\'est pas activé dans la configuration.' },
  { test: /paiement fractionné désactivé/i, map: () => 'Le paiement fractionné n\'est pas activé dans la configuration.' },
  { test: /panier vide/i, map: () => 'Le panier est vide. Ajoutez des articles avant de continuer.' },
  { test: /paiement requis/i, map: () => 'Indiquez un mode et un montant de paiement.' },
  { test: /quantite invalide/i, map: () => 'La quantité saisie n\'est pas valide.' },
  { test: /quantité invalide/i, map: () => 'La quantité saisie n\'est pas valide.' },
  { test: /produit introuvable/i, map: (m) => m.replace(/Produit introuvable:?\s*/i, 'Produit introuvable : ') },
  { test: /produit inconnu/i, map: () => 'Produit introuvable.' },
  { test: /seule une vente brouillon/i, map: () => 'Cette action n\'est possible que sur une vente en cours de saisie.' },
  { test: /seule une vente en attente/i, map: () => 'Cette action n\'est possible que sur une vente en attente.' },
  { test: /session.*non ouverte|aucune session/i, map: () => 'Aucune session de caisse n\'est ouverte. Ouvrez une session pour continuer.' },
  { test: /fichier logo vide/i, map: () => 'Le fichier logo est vide. Choisissez une image valide.' },
  { test: /logo doit etre une image|logo doit être une image/i, map: () => 'Le logo doit être une image (JPG, PNG, etc.).' },
  { test: /fichier vide/i, map: () => 'Le fichier sélectionné est vide.' },
  { test: /fichier obligatoire/i, map: () => 'Sélectionnez un fichier à importer.' },
  { test: /sku existant/i, map: (m) => m },
  { test: /en-tete invalide|en-tête invalide/i, map: () => 'Le fichier importé ne correspond pas au modèle attendu.' },
  { test: /unite inconnue|unité inconnue/i, map: () => 'Unité inconnue dans le fichier importé.' },
  { test: /inventaire déjà validé|inventaire deja valide/i, map: () => 'Cet inventaire est déjà clôturé.' },
  { test: /inventaire annulé/i, map: () => 'Cet inventaire a été annulé.' },
  { test: /seule une sortie brouillon/i, map: () => 'Seul un brouillon peut être modifié ou validé.' },
  { test: /seule une entrée brouillon/i, map: () => 'Seul un brouillon peut être modifié ou validé.' },
  { test: /entrepot|entrepôt/i, map: (m) => m },
  { test: /email deja utilise|email déjà utilisé/i, map: () => 'Cet email est déjà utilisé par un autre compte.' },
  { test: /mot de passe obligatoire/i, map: () => 'Le mot de passe est obligatoire.' },
  { test: /valeur numerique invalide|valeur numérique invalide/i, map: () => 'Valeur numérique invalide.' },
  { test: /valeur booleenne invalide/i, map: () => 'Valeur invalide (oui/non attendu).' },
]

const FIELD_LABELS = {
  'company.name': 'Nom de l\'entreprise',
  'company.currency': 'Devise',
  'company.email': 'Email',
  'company.phone': 'Téléphone',
  'tax.defaultRate': 'Taux de taxe',
  'tax.name': 'Nom de la taxe',
  'stock.lowStockThresholdDefault': 'Seuil stock faible',
  'pos.salePrefix': 'Préfixe ticket',
  name: 'Nom',
  email: 'Email',
  password: 'Mot de passe',
  sku: 'SKU',
  quantity: 'Quantité',
  quantityBase: 'Quantité',
}

const HTTP_MESSAGES = {
  400: 'Les données envoyées sont invalides. Vérifiez le formulaire.',
  401: SESSION_EXPIRED_MESSAGE,
  403: 'Vous n\'avez pas l\'autorisation d\'effectuer cette action.',
  404: 'Élément introuvable.',
  409: 'Cette action est impossible : un conflit a été détecté.',
  422: 'Certaines informations ne sont pas valides. Corrigez le formulaire.',
  500: 'Une erreur technique est survenue. Réessayez dans quelques instants.',
  502: 'Le serveur est temporairement indisponible. Réessayez dans quelques instants.',
  503: 'Le serveur est temporairement indisponible. Réessayez dans quelques instants.',
  504: 'Le serveur met trop de temps à répondre. Réessayez dans quelques instants.',
}

export function markSessionExpired() {
  try {
    sessionStorage.setItem(SESSION_EXPIRED_KEY, '1')
  } catch {
    /* ignore */
  }
}

export function consumeSessionExpiredFlag() {
  try {
    if (sessionStorage.getItem(SESSION_EXPIRED_KEY)) {
      sessionStorage.removeItem(SESSION_EXPIRED_KEY)
      return true
    }
  } catch {
    /* ignore */
  }
  return false
}

export function isNetworkError(error) {
  if (!error) return false
  return error.code === 'ERR_NETWORK'
    || error.message === 'Network Error'
    || (!error.response && !!error.request)
}

export function isAuthError(error) {
  return error?.response?.status === 401
}

export function isValidationError(error) {
  const status = error?.response?.status
  return status === 400 || status === 422
}

export function getFieldErrors(error) {
  const data = error?.response?.data
  if (!data?.errors || typeof data.errors !== 'object') return null
  const mapped = {}
  for (const [field, message] of Object.entries(data.errors)) {
    if (!message) continue
    const label = FIELD_LABELS[field] || field
    mapped[field] = typeof message === 'string'
      ? translateBusinessMessage(message, { field })
      : String(message)
  }
  return Object.keys(mapped).length ? mapped : null
}

export function getRawBackendMessage(error) {
  const data = error?.response?.data
  if (typeof data === 'string' && data.trim()) return data.trim()
  if (data?.message && typeof data.message === 'string') return data.message.trim()
  if (data?.detail && typeof data.detail === 'string') return data.detail.trim()
  return null
}

function isTechnicalMessage(message) {
  if (!message || typeof message !== 'string') return true
  const trimmed = message.trim()
  if (!trimmed) return true
  return TECHNICAL_PATTERNS.some((re) => re.test(trimmed))
}

function translateBusinessMessage(message, context = {}) {
  if (!message) return null
  const trimmed = message.trim()
  if (EXACT_MESSAGES[trimmed]) {
    if (context.module === 'login' && trimmed === 'Authentification requise') {
      return EXACT_MESSAGES[trimmed]
    }
    if (trimmed !== 'Authentification requise') {
      return EXACT_MESSAGES[trimmed]
    }
  }
  for (const { test, map } of PATTERN_MESSAGES) {
    if (test.test(trimmed)) return map(trimmed)
  }
  if (!isTechnicalMessage(trimmed)) return trimmed
  return null
}

function getNetworkMessage(error) {
  if (error?.code === 'ECONNABORTED') {
    return 'Le serveur met trop de temps à répondre. Réessayez dans quelques instants.'
  }
  return 'Impossible de joindre le serveur. Vérifiez votre connexion réseau et que l\'application est bien démarrée.'
}

/**
 * @param {unknown} error - Erreur axios ou autre
 * @param {{ fallback?: string, module?: 'login'|'pos'|'stock'|'config'|'import' }} [options]
 */
export function getErrorMessage(error, options = {}) {
  const fallback = typeof options === 'string' ? options : options?.fallback
  const context = typeof options === 'string' ? {} : options

  if (!error) return fallback || 'Une erreur est survenue.'

  const status = error.response?.status

  if (isNetworkError(error)) {
    return getNetworkMessage(error)
  }

  const fieldErrors = getFieldErrors(error)
  if (fieldErrors) {
    const labels = Object.keys(fieldErrors)
      .map((f) => FIELD_LABELS[f] || f)
    const firstMsg = Object.values(fieldErrors)[0]
    if (labels.length === 1) {
      return `${labels[0]} : ${firstMsg}`
    }
    return `Vérifiez les champs suivants : ${labels.join(', ')}.`
  }

  const raw = getRawBackendMessage(error)
  if (raw) {
    const translated = translateBusinessMessage(raw, context)
    if (translated) return translated
  }

  if (status && HTTP_MESSAGES[status]) {
    if (status === 401 && context.module === 'login') {
      return 'Email ou mot de passe incorrect.'
    }
    if (status === 401) return SESSION_EXPIRED_MESSAGE
    return HTTP_MESSAGES[status]
  }

  if (status === 400) {
    if (raw?.toLowerCase().includes('bad request')) {
      return 'Requête invalide. Vérifiez les informations saisies.'
    }
    return HTTP_MESSAGES[400]
  }
  if (status === 404) return HTTP_MESSAGES[404]
  if (status === 403) return HTTP_MESSAGES[403]
  if (status === 409) return raw && !isTechnicalMessage(raw) ? translateBusinessMessage(raw, context) || HTTP_MESSAGES[409] : HTTP_MESSAGES[409]
  if (status === 422) return HTTP_MESSAGES[422]
  if (status >= 500) return HTTP_MESSAGES[500]

  const msg = error.message
  if (msg && !isTechnicalMessage(msg)) return msg

  return fallback || 'Une erreur est survenue. Réessayez ou contactez votre administrateur.'
}

/** Détails complets pour formulaires (message global + erreurs par champ). */
export function getErrorDetails(error, options = {}) {
  return {
    message: getErrorMessage(error, options),
    fieldErrors: getFieldErrors(error),
    status: error?.response?.status ?? null,
    isNetwork: isNetworkError(error),
    isAuth: isAuthError(error),
  }
}

/** Validation locale — configuration client. */
export function validateClientConfig(config) {
  const fieldErrors = {}
  if (!config?.company?.name?.trim()) {
    fieldErrors['company.name'] = 'Le nom de l\'entreprise est obligatoire.'
  }
  const email = config?.company?.email?.trim()
  if (email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    fieldErrors['company.email'] = 'Adresse email invalide.'
  }
  const taxRate = config?.tax?.defaultRate
  if (taxRate != null && (Number.isNaN(Number(taxRate)) || Number(taxRate) < 0 || Number(taxRate) > 100)) {
    fieldErrors['tax.defaultRate'] = 'Le taux de taxe doit être entre 0 et 100 %.'
  }
  const threshold = config?.stock?.lowStockThresholdDefault
  if (threshold != null && (Number.isNaN(Number(threshold)) || Number(threshold) < 0)) {
    fieldErrors['stock.lowStockThresholdDefault'] = 'Le seuil doit être un nombre positif.'
  }
  const enabledPayments = config?.pos?.paymentMethods?.filter((m) => m.enabled) ?? []
  if (enabledPayments.length === 0) {
    fieldErrors['pos.paymentMethods'] = 'Activez au moins un moyen de paiement.'
  }
  return Object.keys(fieldErrors).length ? fieldErrors : null
}

/** Validation fichier logo côté client. */
export function validateLogoFile(file) {
  if (!file) return 'Sélectionnez une image.'
  if (!file.type?.startsWith('image/')) {
    return 'Le logo doit être une image (JPG, PNG, WebP…).'
  }
  const maxBytes = 5 * 1024 * 1024
  if (file.size > maxBytes) {
    return 'Le logo est trop volumineux (maximum 5 Mo).'
  }
  return null
}
