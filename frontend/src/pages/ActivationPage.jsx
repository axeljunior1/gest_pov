import { useRef, useState } from 'react'
import { useLicense } from '../context/LicenseContext'
import { useNotification } from '../context/NotificationContext'
import { getErrorMessage } from '../utils/errors'
import { Card, Button, Loading } from '../components/ui'

export default function ActivationPage() {
  const { status, installationId, reasonLabel, error, importLicense, refresh } = useLicense()
  const notify = useNotification()
  const fileRef = useRef(null)
  const [importing, setImporting] = useState(false)
  const [copied, setCopied] = useState(false)

  const copyInstallationId = async () => {
    if (!installationId) return
    try {
      await navigator.clipboard.writeText(installationId)
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    } catch {
      notify.error('Copie impossible — sélectionnez l’identifiant manuellement.')
    }
  }

  const handleImport = async () => {
    const file = fileRef.current?.files?.[0]
    if (!file) {
      notify.error('Sélectionnez un fichier .lic')
      return
    }
    setImporting(true)
    try {
      const result = await importLicense(file)
      if (result.valid) {
        notify.success('Licence activée — rechargement…')
        window.location.reload()
      } else {
        notify.error(reasonLabel || 'Licence refusée')
        await refresh()
      }
    } catch (e) {
      notify.error(getErrorMessage(e))
    } finally {
      setImporting(false)
    }
  }

  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center p-6">
      <Card className="w-full max-w-lg p-8 space-y-6">
        <div className="text-center space-y-2">
          <h1 className="text-2xl font-semibold text-gray-900">Activation Gest_POV</h1>
          <p className="text-sm text-gray-600">
            Une licence valide est requise pour utiliser l’application sur cette installation.
          </p>
        </div>

        {error && (
          <p className="text-sm text-red-700 bg-red-50 border border-red-200 rounded-lg px-4 py-3">
            {error}
          </p>
        )}

        {status?.reason && (
          <div className="text-sm text-amber-800 bg-amber-50 border border-amber-200 rounded-lg px-4 py-3">
            <p className="font-medium">État : {status.reason}</p>
            <p className="mt-1">{reasonLabel}</p>
          </div>
        )}

        <div className="space-y-2">
          <label className="text-xs font-medium text-gray-500 uppercase">Identifiant machine (installation)</label>
          <div className="flex gap-2 items-stretch">
            <code
              className="flex-1 text-sm font-mono bg-gray-100 border rounded-lg px-3 py-2.5 break-all select-all"
              title={installationId || undefined}
            >
              {installationId || 'Chargement…'}
            </code>
            <Button type="button" variant="secondary" onClick={copyInstallationId} disabled={!installationId}>
              {copied ? 'Copié' : 'Copier'}
            </Button>
          </div>
          <p className="text-xs text-gray-500">
            Copiez cet identifiant et transmettez-le à votre éditeur pour obtenir un fichier <strong>.lic</strong> lié à cette machine.
          </p>
        </div>

        <div className="space-y-2">
          <label className="text-xs font-medium text-gray-500 uppercase">Fichier licence (.lic)</label>
          <input
            ref={fileRef}
            type="file"
            accept=".lic,application/json"
            className="block w-full text-sm text-gray-600 file:mr-4 file:py-2 file:px-4 file:rounded-lg file:border-0 file:bg-gray-900 file:text-white hover:file:bg-gray-800"
          />
        </div>

        <Button type="button" className="w-full" disabled={importing} onClick={handleImport}>
          {importing ? 'Vérification…' : 'Importer la licence'}
        </Button>
      </Card>
    </div>
  )
}

export function LicenseGate({ children }) {
  const { loading, isLicensed } = useLicense()

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <Loading />
      </div>
    )
  }

  if (!isLicensed) {
    return <ActivationPage />
  }

  return children
}
