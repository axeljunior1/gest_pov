import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import { licenseApi } from '../api'
import { getErrorMessage } from '../utils/errors'

const LicenseContext = createContext(null)

const REASON_LABELS = {
  LICENSE_MISSING: 'Aucune licence installée sur cette machine.',
  INVALID_FORMAT: 'Le fichier licence est illisible ou mal formé.',
  INVALID_SIGNATURE: 'Signature RSA invalide — fichier non authentique.',
  INSTALLATION_MISMATCH: 'Cette licence n’est pas émise pour cet identifiant d’installation.',
  EXPIRED: 'La licence est expirée.',
  INVALID_APP: 'Cette licence n’est pas destinée à Gest_POV.',
}

export function LicenseProvider({ children }) {
  const [loading, setLoading] = useState(true)
  const [status, setStatus] = useState(null)
  const [installationId, setInstallationId] = useState('')
  const [error, setError] = useState('')

  const loadInstallationId = useCallback(async () => {
    try {
      const data = await licenseApi.installationId()
      if (data?.installationId) {
        setInstallationId(data.installationId)
        return data.installationId
      }
    } catch {
      /* endpoint dédié indisponible — fallback via status ou erreur API */
    }
    return null
  }, [])

  const refresh = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const [statusData, idFromEndpoint] = await Promise.all([
        licenseApi.status(),
        loadInstallationId(),
      ])
      setStatus(statusData)
      if (!idFromEndpoint && statusData?.installationId) {
        setInstallationId(statusData.installationId)
      }
    } catch (e) {
      setError(getErrorMessage(e))
      setStatus({ valid: false, reason: 'LICENSE_MISSING', activated: false })
      const idFromError = e.response?.data?.installationId
      if (idFromError) {
        setInstallationId(idFromError)
      } else {
        await loadInstallationId()
      }
    } finally {
      setLoading(false)
    }
  }, [loadInstallationId])

  useEffect(() => {
    refresh()
  }, [refresh])

  const importLicense = useCallback(async (file) => {
    const result = await licenseApi.importFile(file)
    setStatus(result)
    return result
  }, [])

  const reasonLabel = status?.reason ? (REASON_LABELS[status.reason] || status.reason) : ''

  const value = useMemo(() => ({
    loading,
    status,
    installationId: installationId || status?.installationId || '',
    error,
    reasonLabel,
    refresh,
    importLicense,
    isLicensed: !!status?.valid,
    daysRemaining: status?.daysRemaining,
  }), [loading, status, installationId, error, reasonLabel, refresh, importLicense])

  return (
    <LicenseContext.Provider value={value}>
      {children}
    </LicenseContext.Provider>
  )
}

export function useLicense() {
  const ctx = useContext(LicenseContext)
  if (!ctx) throw new Error('useLicense must be used within LicenseProvider')
  return ctx
}
