import { useEffect, useState } from 'react'
import { alertsApi, notificationsApi } from '../api'
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

export default function AlertsPage() {
  const notify = useNotification()
  const { run, submitting } = useAsyncAction()
  const [loading, setLoading] = useState(true)
  const [tab, setTab] = useState('open')
  const [alerts, setAlerts] = useState([])
  const [notifications, setNotifications] = useState([])

  const load = async () => {
    setLoading(true)
    try {
      const [openAlerts, allAlerts, notifs] = await Promise.all([
        alertsApi.list({ status: 'OPEN' }),
        alertsApi.list({}),
        notificationsApi.list(),
      ])
      setAlerts(tab === 'open' ? openAlerts : allAlerts)
      setNotifications(notifs)
    } catch (e) {
      notify.error(getErrorMessage(e))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [tab])

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
        ]}
        active={tab}
        onChange={setTab}
      />

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
      ) : (
        <Card className="p-6 mt-6">
          {alerts.length === 0 ? (
            <EmptyState message={`Aucune alerte ${tab === 'open' ? 'ouverte' : 'enregistrée'}.`} />
          ) : (
            <ul className="divide-y divide-gray-100">
              {alerts.map((a) => (
                <li key={a.id} className="py-4">
                  <div className="flex items-start justify-between gap-4">
                    <div className="min-w-0 flex-1">
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
                        Déclenchée {new Date(a.lastTriggeredAt).toLocaleString('fr-FR')}
                        {a.triggerCount > 1 && ` · ${a.triggerCount} fois`}
                      </p>
                    </div>
                    {a.status === 'OPEN' && (
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
                    {a.status === 'ACKNOWLEDGED' && (
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
      )}
    </div>
  )
}
