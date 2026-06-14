/** Notifie les écrans POS qu'une vente a changé d'état (hold, envoi caisse, retour, etc.). */
export function notifyPosSaleStateChanged() {
  window.dispatchEvent(new Event('pos-sale-state-changed'))
}
