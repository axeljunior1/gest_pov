import { useEffect, useState } from 'react'
import { settingsApi } from '../api'
import { useAuth } from '../context/AuthContext'
import { PageHeader, Card, Button, Loading } from '../components/ui'
import { useNotification } from '../context/NotificationContext'
import { getErrorMessage } from '../utils/errors'

const GROUPS = [
  {
    title: 'Entreprise & affichage',
    keys: ['company.name', 'company.logo', 'app.currency', 'app.language', 'app.timezone', 'app.date_format'],
  },
  {
    title: 'Stock & alertes',
    keys: ['stock.allow_negative', 'stock.low_threshold_default', 'alert.expiry_days_default'],
  },
  {
    title: 'Numérotation documents',
    keys: ['numbering.entry_prefix', 'numbering.exit_prefix', 'numbering.inventory_prefix', 'numbering.movement_prefix'],
  },
]

export default function SettingsPage() {
  const notify = useNotification()
  const { hasPermission } = useAuth()
  const canUpdate = hasPermission('settings.update')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [settings, setSettings] = useState([])
  const [values, setValues] = useState({})

  useEffect(() => {
    settingsApi.getAll()
      .then((data) => {
        setSettings(data)
        const map = {}
        data.forEach((s) => { map[s.key] = s.value ?? '' })
        setValues(map)
      })
      .catch((e) => notify.error(getErrorMessage(e)))
      .finally(() => setLoading(false))
  }, [notify])

  const handleChange = (key, value) => {
    setValues((prev) => ({ ...prev, [key]: value }))
  }

  const handleSave = async () => {
    if (!canUpdate) return
    setSaving(true)
    try {
      await settingsApi.updateBulk(values)
      notify.success('Paramètres enregistrés')
      const data = await settingsApi.getAll()
      setSettings(data)
    } catch (e) {
      notify.error(getErrorMessage(e))
    } finally {
      setSaving(false)
    }
  }

  if (loading) return <Loading />

  const byKey = Object.fromEntries(settings.map((s) => [s.key, s]))

  return (
    <div>
      <PageHeader
        title="Paramètres généraux"
        subtitle="Configuration centralisée de l'application"
        action={canUpdate && (
          <Button onClick={handleSave} disabled={saving}>
            {saving ? 'Enregistrement…' : 'Enregistrer'}
          </Button>
        )}
      />

      <div className="space-y-6">
        {GROUPS.map((group) => (
          <Card key={group.title} className="p-6">
            <h3 className="text-sm font-semibold text-gray-900 mb-4">{group.title}</h3>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {group.keys.map((key) => {
                const meta = byKey[key]
                if (!meta) return null
                const isBool = meta.type === 'BOOLEAN'
                return (
                  <label key={key} className="block">
                    <span className="text-xs font-medium text-gray-500 uppercase tracking-wide">
                      {meta.description || key}
                    </span>
                    {isBool ? (
                      <select
                        className="mt-1 w-full border border-gray-200 rounded-lg px-3 py-2 text-sm"
                        value={values[key] ?? 'false'}
                        disabled={!canUpdate}
                        onChange={(e) => handleChange(key, e.target.value)}
                      >
                        <option value="true">Oui</option>
                        <option value="false">Non</option>
                      </select>
                    ) : (
                      <input
                        type={meta.type === 'NUMBER' ? 'number' : 'text'}
                        className="mt-1 w-full border border-gray-200 rounded-lg px-3 py-2 text-sm"
                        value={values[key] ?? ''}
                        disabled={!canUpdate}
                        onChange={(e) => handleChange(key, e.target.value)}
                      />
                    )}
                  </label>
                )
              })}
            </div>
          </Card>
        ))}
      </div>
    </div>
  )
}
