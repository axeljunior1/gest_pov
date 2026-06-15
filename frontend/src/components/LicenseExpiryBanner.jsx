import { useLicense } from '../context/LicenseContext'

export default function LicenseExpiryBanner() {
  const { status, daysRemaining } = useLicense()

  if (!status?.valid || daysRemaining == null) return null
  if (daysRemaining > 30) return null

  const urgent = daysRemaining <= 7
  const classes = urgent
    ? 'bg-red-50 border-red-200 text-red-900'
    : 'bg-amber-50 border-amber-200 text-amber-900'

  return (
    <div className={`mb-4 rounded-lg border px-4 py-3 text-sm ${classes}`}>
      <p className="font-medium">
        {urgent ? 'Licence bientôt expirée' : 'Licence expire prochainement'}
      </p>
      <p className="mt-0.5">
        {daysRemaining <= 0
          ? 'La licence expire aujourd’hui.'
          : `Il reste ${daysRemaining} jour${daysRemaining > 1 ? 's' : ''} (expiration : ${status.expiresAt}).`}
        {' '}Contactez votre éditeur pour renouveler le fichier .lic.
      </p>
    </div>
  )
}
