import { Link } from 'react-router-dom'

export default function SetupBanner({ publicSettings }) {
  if (!publicSettings || publicSettings.setupCompleted) {
    return null
  }
  const missingCurrency = !publicSettings.currency
  const missingName = !publicSettings.companyName?.trim()
  if (!missingCurrency && !missingName) {
    return null
  }
  return (
    <div className="mb-4 rounded-lg border border-blue-200 bg-blue-50 px-4 py-3 text-sm text-blue-900 flex flex-wrap items-center justify-between gap-2">
      <span>
        <strong>Configuration initiale incomplète.</strong>
        {missingName && ' Renseignez le nom de l\'entreprise.'}
        {missingCurrency && ' Choisissez la devise.'}
        {' '}Ces paramètres sont requis avant une mise en production.
      </span>
      <Link
        to="/configuration"
        className="shrink-0 rounded-lg bg-blue-900 text-white px-3 py-1.5 text-xs font-medium hover:bg-blue-800"
      >
        Configurer maintenant
      </Link>
    </div>
  )
}
