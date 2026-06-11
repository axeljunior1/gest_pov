export function getErrorMessage(error) {
  if (!error) return 'Une erreur est survenue.'

  if (error.code === 'ERR_NETWORK' || error.message === 'Network Error' || !error.response) {
    return 'Impossible de joindre le serveur. Vérifiez que le backend est lancé sur le port 8080.'
  }

  const data = error.response?.data
  if (typeof data === 'string' && data.trim()) return data
  if (data?.message) return data.message

  if (data?.errors && typeof data.errors === 'object') {
    const messages = Object.values(data.errors).filter(Boolean)
    if (messages.length) return messages.join(' · ')
  }

  if (data?.detail) return data.detail

  const status = error.response?.status
  if (status === 400) {
    if (data?.error === 'Bad Request') {
      return 'Requête invalide : vérifiez les valeurs sélectionnées (catégorie, statut, type de code-barres…).'
    }
    return 'Données invalides. Vérifiez le formulaire.'
  }
  if (status === 404) return 'Élément introuvable.'
  if (status === 409) return 'Conflit : cette ressource existe déjà.'
  if (status >= 500) return 'Erreur serveur. Réessayez plus tard.'

  return error.message || 'Une erreur est survenue.'
}
