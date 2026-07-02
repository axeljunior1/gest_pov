/** Calcule l'âge d'une vente en attente de paiement (minutes). */
export function pendingAgeMinutes(sale) {
  const raw = sale?.sentToPaymentAt || sale?.submittedAt
  if (!raw) return null
  const ms = Date.now() - new Date(raw).getTime()
  if (Number.isNaN(ms) || ms < 0) return null
  return Math.floor(ms / 60000)
}

/** Classe CSS selon seuils config POS (alertPendingPaymentMinutes). */
export function pendingAgeClass(ageMinutes, alertMinutes) {
  if (ageMinutes == null || alertMinutes == null || alertMinutes <= 0) return ''
  if (ageMinutes >= alertMinutes) return 'bg-red-950/40'
  if (ageMinutes >= Math.max(1, Math.floor(alertMinutes / 2))) return 'bg-amber-950/30'
  return ''
}
