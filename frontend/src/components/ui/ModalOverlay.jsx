/**
 * Modale avec fermeture au clic sur l'overlay (comportement standard type Odoo/Gmail).
 * Le contenu interne doit stopper la propagation pour ne pas fermer au clic dedans.
 */
export default function ModalOverlay({
  open,
  onClose,
  children,
  className = 'flex items-center justify-center',
  overlayClassName = 'fixed inset-0 bg-black/70 z-50 p-4',
  zIndex = 'z-50',
}) {
  if (!open) return null

  const handleBackdropMouseDown = (e) => {
    if (e.target === e.currentTarget) {
      onClose?.()
    }
  }

  return (
    <div
      className={`${overlayClassName} ${className}`.replace('z-50', zIndex)}
      onMouseDown={handleBackdropMouseDown}
      role="presentation"
    >
      <div onMouseDown={(e) => e.stopPropagation()} className="max-h-full overflow-auto">
        {children}
      </div>
    </div>
  )
}
