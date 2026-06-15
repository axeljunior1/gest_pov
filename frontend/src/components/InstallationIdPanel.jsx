import { useState } from 'react'
import { useLicense } from '../context/LicenseContext'
import { useNotification } from '../context/NotificationContext'
import { Button } from './ui'

export default function InstallationIdPanel({ compact = false }) {
  const { installationId, status } = useLicense()
  const notify = useNotification()
  const [copied, setCopied] = useState(false)

  const copy = async () => {
    if (!installationId) return
    try {
      await navigator.clipboard.writeText(installationId)
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    } catch {
      notify.error('Copie impossible — sélectionnez l’identifiant manuellement.')
    }
  }

  if (!installationId && compact) return null

  return (
    <div className={compact ? 'space-y-2' : 'space-y-3'}>
      {!compact && (
        <div>
          <h3 className="text-sm font-semibold text-gray-900">Licence Gest_POV</h3>
          {status?.valid && status.expiresAt && (
            <p className="text-sm text-gray-600 mt-1">
              Licence active — expiration : {status.expiresAt}
              {status.maxUsers != null && ` · ${status.maxUsers} utilisateurs max`}
            </p>
          )}
        </div>
      )}
      <div>
        <label className="text-xs font-medium text-gray-500 uppercase">Identifiant machine</label>
        <div className="mt-1 flex gap-2 items-stretch">
          <code className="flex-1 text-sm font-mono bg-gray-50 border rounded-lg px-3 py-2 break-all select-all">
            {installationId || '—'}
          </code>
          <Button type="button" variant="secondary" onClick={copy} disabled={!installationId}>
            {copied ? 'Copié' : 'Copier'}
          </Button>
        </div>
        <p className="text-xs text-gray-500 mt-1">
          À transmettre à l’éditeur pour générer ou renouveler le fichier <strong>.lic</strong>.
        </p>
      </div>
    </div>
  )
}
