import { useEffect, useState } from 'react'
import AlertThresholdsPanel from '../components/AlertThresholdsPanel'
import { alertsApi, notificationsApi, productsApi } from '../api'
import { useAuth } from '../context/AuthContext'
import { PageHeader, Card, Button, Loading, Tabs, Badge, EmptyState } from '../components/ui'
import { useAsyncAction } from '../hooks/useAsyncAction'
import { useNotification } from '../context/NotificationContext'
import { getErrorMessage } from '../utils/errors'

const severityTone = {
  CRITICAL: 'danger',
  WARNING: 'warning',
  INFO: 'info',
}

const statusTone = {
  OPEN: 'danger',
  ACKNOWLEDGED: 'warning',
  RESOLVED: 'success',
  IGNORED: 'default',
}

const ALERT_TYPES = [
  { value: '', label: 'Tous les types' },
  { value: 'LOW_STOCK', label: 'Stock faible' },
  { value: 'OUT_OF_STOCK', label: 'Rupture' },
  { value: 'EXPIRY_SOON', label: 'Péremption proche' },
  { value: 'EXPIRED', label: 'Expiré' },
]

const ALERT_STATUSES = [
  { value: '', label: 'Tous les statuts' },
  { value: 'OPEN', label: 'Ouvertes' },
  { value: 'ACKNOWLEDGED', label: 'Acquittées' },
  { value: 'RESOLVED', label: 'Résolues' },
]

export default function AlertsPage() {
  const notify = useNotification()
  const { hasPermission } = useAuth()
  const canManage = hasPermission('alerts.manage')
  const { run, submitting } = useAsyncAction()
  const [loading, setLoading] = useState(true)
  const [tab, setTab] = useState('open')
  const [alerts, setAlerts] = useState([])
  const [notifications, setNotifications] = useState([])
  const [products, setProducts] = useState([])
  const [filterType, setFilterType] = useState('')
  const [filterStatus, setFilterStatus] = useState('')
  const [filterProductId, setFilterProductId] = useState('')
  const [selectedAlert, setSelectedAlert] = useState(null)

  useEffect(() => {
    productsApi.list().then(setProducts).catch(() => {})
  }, [])

  const buildListParams = () => {
    const params = {}
    if (filterType) params.type = filterType
    if (filterProductId) params.productId = filterProductId
    if (tab === 'open') {
      params.status = 'OPEN'
    } else if (filterStatus) {
      params.status = filterStatus
    }
    return params
  }

  const load = async () => {
    setLoading(true)
    try {
      const params = buildListParams()
      const [alertList, notifs] = await Promise.all([
        alertsApi.list(params),
        notificationsApi.list(),
      ])
      setAlerts(alertList)
      setNotifications(notifs)
      if (selectedAlert) {
        const stillThere = alertList.find((a) => a.id === selectedAlert.id)
        setSelectedAlert(stillThere || null)
      }
    } catch (e) {
      notify.error(getErrorMessage(e))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [tab, filterType, filterStatus, filterProductId])

  const openDetail = async (alert) => {
    try {
      const detail = await alertsApi.getById(alert.id)
      setSelectedAlert(detail)
    } catch (e) {
      notify.error(getErrorMessage(e))
    }
  }

  const handleAction = (id, action) => {
    const apiCall = action === 'acknowledge'
      ? () => alertsApi.acknowledge(id)
      : action === 'resolve'
        ? () => alertsApi.resolve(id)
        : () => alertsApi.ignore(id)
    run(
      () => apiCall(),
      () => {
        notify.success(`Alerte ${action === 'acknowledge' ? 'acquittée' : action === 'resolve' ? 'résolue' : 'ignorée'}`)
        load()
      },
      (e) => notify.error(getErrorMessage(e)),
    )
  }

  const markRead = (id) => {
    run(
      () => notificationsApi.markRead(id),
      () => load(),
      (e) => notify.error(getErrorMessage(e)),
    )
  }

  if (loading) return <Loading />

  return (
    <div>
      <PageHeader
        title="Alertes & Notifications"
        subtitle="Surveillance automatique du stock et des événements critiques"
      />

      <Tabs
        tabs={[
          { id: 'open', label: 'Alertes ouvertes' },
          { id: 'all', label: 'Historique' },
          { id: 'notifications', label: 'Notifications' },
          { id: 'thresholds', label: 'Seuils' },
        ]}
        active={tab}
        onChange={(id) => {
          setTab(id)
          setSelectedAlert(null)
        }}
      />

      {tab !== 'notifications' && (
        <Card className="p-4 mt-6">
          <div className="flex flex-wrap gap-3">
            <select
              className="border border-gray-200 rounded-lg px-3 py-2 text-sm"
              value={filterType}
              onChange={(e) => { setFilterType(e.target.value); setSelectedAlert(null) }}
            >
              {ALERT_TYPES.map((t) => (
                <option key={t.value || 'all'} value={t.value}>{t.label}</option>
              ))}
            </select>
            {tab === 'all' && (
              <select
                className="border border-gray-200 rounded-lg px-3 py-2 text-sm"
                value={filterStatus}
                onChange={(e) => { setFilterStatus(e.target.value); setSelectedAlert(null) }}
              >
                {ALERT_STATUSES.map((s) => (
                  <option key={s.value || 'all'} value={s.value}>{s.label}</option>
                ))}
              </select>
            )}
            <select
              className="border border-gray-200 rounded-lg px-3 py-2 text-sm min-w-[180px]"
              value={filterProductId}
              onChange={(e) => { setFilterProductId(e.target.value); setSelectedAlert(null) }}
            >
              <option value="">Tous les produits</option>
              {products.map((p) => (
                <option key={p.id} value={p.id}>{p.nom}</option>
              ))}
            </select>
          </div>
        </Card>
      )}

      {tab === 'notifications' ? (
        <Card className="p-6 mt-6">
          {notifications.length === 0 ? (
            <p className="text-sm text-gray-500">Aucune notification.</p>
          ) : (
            <ul className="divide-y divide-gray-100">
              {notifications.map((n) => (
                <li key={n.id} className="py-4 flex items-start justify-between gap-4">
                  <div>
                    <div className="flex items-center gap-2 mb-1">
                      <Badge tone={severityTone[n.alertType === 'OUT_OF_STOCK' ? 'CRITICAL' : 'WARNING'] || 'default'}>
                        {n.alertType}
                      </Badge>
                      <span className="text-xs text-gray-400">{n.channel}</span>
                      {n.status === 'SENT' && (
                        <Badge tone="warning">Non lue</Badge>
                      )}
                    </div>
                    <p className="text-sm text-gray-800">{n.alertMessage}</p>
                    <p className="text-xs text-gray-400 mt-1">
                      {n.sentAt ? new Date(n.sentAt).toLocaleString('fr-FR') : 'En attente'}
                    </p>
                  </div>
                  {n.status === 'SENT' && (
                    <Button className="px-3 py-1.5 text-xs" variant="secondary" onClick={() => markRead(n.id)}>
                      Marquer lue
                    </Button>
                  )}
                </li>
              ))}
            </ul>
          )}
        </Card>
      ) : tab === 'thresholds' ? (
        <AlertThresholdsPanel />
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mt-6">
          <Card className="p-6 lg:col-span-2">
            {alerts.length === 0 ? (
              <EmptyState message={`Aucune alerte ${tab === 'open' ? 'ouverte' : 'enregistrée'}.`} />
            ) : (
              <ul className="divide-y divide-gray-100">
                {alerts.map((a) => (
                  <li key={a.id} className="py-4">
                    <div className="flex items-start justify-between gap-4">
                      <button
                        type="button"
                        className="min-w-0 flex-1 text-left hover:bg-gray-50 -m-2 p-2 rounded-lg transition"
                        onClick={() => openDetail(a)}
                      >
                        <div className="flex flex-wrap items-center gap-2 mb-1">
                          <Badge tone={severityTone[a.severity] || 'default'}>{a.type}</Badge>
                          <Badge tone={statusTone[a.status] || 'default'}>{a.status}</Badge>
                          {a.productNom && (
                            <span className="text-sm font-medium text-gray-900">{a.productNom}</span>
                          )}
                        </div>
                        <p className="text-sm text-gray-700">{a.message}</p>
                        <p className="text-xs text-gray-400 mt-1">
                          {a.warehouseCode && `${a.warehouseCode}/${a.locationCode || '-'} · `}
                          {a.lotNumero && `Lot ${a.lotNumero} · `}
                          Déclenchée {new Date(a.lastTriggeredAt).toLocaleString('fr-FR')}
                          {a.triggerCount > 1 && ` · ${a.triggerCount} fois`}
                        </p>
                      </button>
                      {canManage && a.status === 'OPEN' && (
                        <div className="flex gap-2 shrink-0">
                          <Button className="px-3 py-1.5 text-xs" variant="secondary" disabled={submitting}
                            onClick={() => handleAction(a.id, 'acknowledge')}>
                            Acquitter
                          </Button>
                          <Button className="px-3 py-1.5 text-xs" disabled={submitting}
                            onClick={() => handleAction(a.id, 'resolve')}>
                            Résoudre
                          </Button>
                        </div>
                      )}
                      {canManage && a.status === 'ACKNOWLEDGED' && (
                        <Button className="px-3 py-1.5 text-xs" disabled={submitting}
                          onClick={() => handleAction(a.id, 'resolve')}>
                          Résoudre
                        </Button>
                      )}
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </Card>

          <Card className="p-6">
            <h3 className="text-sm font-semibold text-gray-900 mb-4">Détail de l'alerte</h3>
            {!selectedAlert ? (
              <p className="text-sm text-gray-500">Sélectionnez une alerte pour voir le détail.</p>
            ) : (
              <dl className="space-y-3 text-sm">
                <div>
                  <dt className="text-gray-500">Type</dt>
                  <dd className="font-medium">{selectedAlert.type}</dd>
                </div>
                <div>
                  <dt className="text-gray-500">Gravité</dt>
                  <dd><Badge tone={severityTone[selectedAlert.severity] || 'default'}>{selectedAlert.severity}</Badge></dd>
                </div>
                <div>
                  <dt className="text-gray-500">Statut</dt>
                  <dd><Badge tone={statusTone[selectedAlert.status] || 'default'}>{selectedAlert.status}</Badge></dd>
                </div>
                <div>
                  <dt className="text-gray-500">Produit</dt>
                  <dd className="font-medium">{selectedAlert.productNom || '—'}</dd>
                </div>
                <div>
                  <dt className="text-gray-500">Entrepôt / Emplacement</dt>
                  <dd>{selectedAlert.warehouseCode || '—'} / {selectedAlert.locationCode || '—'}</dd>
                </div>
                {selectedAlert.lotNumero && (
                  <div>
                    <dt className="text-gray-500">Lot</dt>
                    <dd>{selectedAlert.lotNumero}</dd>
                  </div>
                )}
                <div>
                  <dt className="text-gray-500">Valeur déclenchée / Seuil</dt>
                  <dd>
                    {selectedAlert.triggeredValue ?? '—'} / {selectedAlert.thresholdValue ?? '—'}
                  </dd>
                </div>
                <div>
                  <dt className="text-gray-500">Message</dt>
                  <dd className="text-gray-700">{selectedAlert.message}</dd>
                </div>
                <div>
                  <dt className="text-gray-500">Créée le</dt>
                  <dd>{selectedAlert.createdAt ? new Date(selectedAlert.createdAt).toLocaleString('fr-FR') : '—'}</dd>
                </div>
                {selectedAlert.resolvedAt && (
                  <div>
                    <dt className="text-gray-500">Résolue le</dt>
                    <dd>{new Date(selectedAlert.resolvedAt).toLocaleString('fr-FR')}</dd>
                  </div>
                )}
              </dl>
            )}
          </Card>
        </div>
      )}
    </div>
  )
}
