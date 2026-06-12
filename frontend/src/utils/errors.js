export function getErrorMessage(error) {
  if (!error) return 'Une erreur est survenue.'

  const status = error.response?.status

  if (error.code === 'ECONNABORTED') {
    return 'Le serveur met trop de temps à répondre. Attendez quelques secondes puis réessayez (le backend peut être en cours de démarrage).'
  }

  if (status === 502 || status === 503 || status === 504) {
    return 'Le backend est indisponible ou redémarre. Relancez avec .\\dev.ps1 puis réessayez dans 30 secondes.'
  }

  if (error.code === 'ERR_NETWORK' || error.message === 'Network Error' || !error.response) {
    return 'Impossible de joindre le serveur. Vérifiez que le backend tourne sur le port 8080 (commande : .\\dev.ps1).'
  }

  const data = error.response?.data
  if (typeof data === 'string' && data.trim()) return data
  if (data?.message) return data.message

  if (data?.errors && typeof data.errors === 'object') {
    const messages = Object.values(data.errors).filter(Boolean)
    if (messages.length) return messages.join(' · ')
  }

  if (data?.detail) return data.detail

  if (status === 400) {
    if (data?.error === 'Bad Request') {
      return 'Requête invalide : vérifiez les valeurs sélectionnées (catégorie, statut, type de code-barres…).'
    }
    return 'Données invalides. Vérifiez le formulaire.'
  }
  if (status === 404) return 'Élément introuvable.'
  if (status === 403) return data?.message || 'Accès refusé — permission insuffisante.'
  if (status === 409) return 'Conflit : cette ressource existe déjà.'
  if (status >= 500) return 'Erreur serveur. Réessayez plus tard.'

  return error.message || 'Une erreur est survenue.'
}
