import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { demoDataApi } from '../api'
import { useAuth } from '../context/AuthContext'
import { PageHeader, Card, Button, Loading } from '../components/ui'
import { useNotification } from '../context/NotificationContext'
import { getErrorDetails } from '../utils/errors'

function Stat({ label, value }) {
  return (
    <div className="rounded-lg border border-gray-200 bg-gray-50 px-4 py-3">
      <p className="text-xs text-gray-500 uppercase tracking-wide">{label}</p>
      <p className="text-2xl font-semibold text-gray-900 mt-1">{value ?? 0}</p>
    </div>
  )
}

export default function DemoDataPage() {
  const notify = useNotification()
  const { hasPermission } = useAuth()
  const canManage = hasPermission('settings.update')
  const [loading, setLoading] = useState(true)
  const [status, setStatus] = useState(null)
  const [generating, setGenerating] = useState(false)
  const [cleaning, setCleaning] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      setStatus(await demoDataApi.status())
    } catch (e) {
      notify.error(getErrorDetails(e, { module: 'config' }).message)
    } finally {
      setLoading(false)
    }
  }, [notify])

  useEffect(() => { load() }, [load])

  const handleGenerate = async (force = false) => {
    if (!canManage || generating) return
    if (status?.demoPresent && !force) {
      const ok = window.confirm(
        'Des données de démonstration existent déjà. Voulez-vous les remplacer ? Les données réelles ne seront pas touchées.',
      )
      if (!ok) return
      return handleGenerate(true)
    }
    setGenerating(true)
    try {
      const result = await demoDataApi.generate(force)
      notify.success(result.message || 'Données de test générées.')
      await load()
    } catch (e) {
      notify.error(getErrorDetails(e, { module: 'config' }).message)
    } finally {
      setGenerating(false)
    }
  }

  const handleCleanup = async () => {
    if (!canManage || cleaning) return
    const ok = window.confirm(
      'Supprimer toutes les données de démonstration ?\n\n'
      + 'Seuls les éléments marqués DEMO seront supprimés (produits, clients, ventes de test…).\n'
      + 'Vos données réelles ne seront pas affectées.',
    )
    if (!ok) return
    setCleaning(true)
    try {
      const result = await demoDataApi.cleanup()
      notify.success(result.message || 'Données de test supprimées.')
      await load()
    } catch (e) {
      notify.error(getErrorDetails(e, { module: 'config' }).message)
    } finally {
      setCleaning(false)
    }
  }

  if (loading) return <Loading />

  return (
    <div>
      <PageHeader
        title="Données de démonstration"
        subtitle="Génération et suppression de données de test — réservé aux administrateurs"
      />

      <div className="mb-6 rounded-lg border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-900">
        <strong>Attention :</strong> ces données sont uniquement destinées aux tests et formations.
        {' '}<strong>Ne les utilisez pas en production réelle.</strong>
        {' '}Les produits demo sont identifiables par le préfixe SKU <code className="text-xs bg-white px-1 rounded">DEMO-</code>.
      </div>

      {!status?.setupCompleted && (
        <div className="mb-6 rounded-lg border border-blue-200 bg-blue-50 px-4 py-3 text-sm text-blue-900">
          Pensez à terminer la{' '}
          <Link to="/configuration" className="underline font-medium">configuration client</Link>
          {' '}(nom entreprise, devise) avant d&apos;utiliser l&apos;application en production.
        </div>
      )}

      <Card className="p-6 mb-6">
        <h3 className="text-sm font-semibold text-gray-900 mb-4">Statut actuel</h3>
        <p className="text-sm text-gray-600 mb-4">{status?.message}</p>
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-3 mb-4">
          <Stat label="Produits demo" value={status?.demoProducts} />
          <Stat label="Catégories demo" value={status?.demoCategories} />
          <Stat label="Clients demo" value={status?.demoCustomers} />
          <Stat label="Fournisseurs demo" value={status?.demoSuppliers} />
          <Stat label="Ventes demo" value={status?.demoSales} />
          <Stat label="Mouvements demo" value={status?.demoStockMovements} />
        </div>
        <p className="text-xs text-gray-500">
          SKU de référence : {status?.markerSku}
          {' · '}
          Génération auto au démarrage : {status?.demoAutoEnabled ? 'activée' : 'désactivée'}
        </p>
      </Card>

      {canManage && (
        <div className="flex flex-wrap gap-3">
          <Button onClick={() => handleGenerate(false)} disabled={generating || cleaning}>
            {generating ? 'Génération…' : 'Générer des données de test'}
          </Button>
          <Button variant="secondary" onClick={handleCleanup} disabled={cleaning || generating || !status?.demoPresent}>
            {cleaning ? 'Suppression…' : 'Supprimer les données de test'}
          </Button>
        </div>
      )}
    </div>
  )
}
