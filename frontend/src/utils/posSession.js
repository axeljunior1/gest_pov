/** Notifie la barre de navigation POS qu'une session a été ouverte ou fermée. */
export function notifyPosSessionChanged() {
  window.dispatchEvent(new Event('pos-session-changed'))
}
