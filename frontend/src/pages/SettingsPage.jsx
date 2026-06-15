import { useEffect, useState } from 'react'
import { settingsApi } from '../api'
import { useAuth } from '../context/AuthContext'
import { PageHeader, Card, Button, Loading } from '../components/ui'
import InstallationIdPanel from '../components/InstallationIdPanel'
import { useNotification } from '../context/NotificationContext'
import { getErrorMessage } from '../utils/errors'

const GROUPS = [
  {
    title: 'Entreprise & affichage',
    keys: ['company.name', 'company.logo', 'app.currency', 'app.language', 'app.timezone', 'app.date_format'],
  },
  {
    title: 'Stock & alertes',
    keys: ['stock.allow_negative', 'stock.low_threshold_default', 'stock.valuation_method', 'alert.expiry_days_default'],
  },
  {
    title: 'Numérotation documents',
    keys: ['numbering.entry_prefix', 'numbering.exit_prefix', 'numbering.inventory_prefix', 'numbering.movement_prefix', 'numbering.sale_prefix'],
  },
  {
    title: 'Point de vente (POS)',
    keys: [
      'pos_sales_flow_mode',
      'pos.allow_seller_cash_collection',
      'pos.allow_partial_payment',
      'pos.allow_split_payment',
      'pos.max_pending_payment_duration',
      'pos.alert.pending_payment_minutes',
      'pos.alert.cash_difference_threshold',
      'pos.require_manager_validation_for_cash_difference',
      'pos.register_name',
      'pos.default_warehouse_code',
      'pos.tax_rate_default',
    ],
  },
  {
    title: 'Fidélité',
    keys: [
      'loyalty.enabled', 'loyalty.points_per_currency_unit', 'loyalty.currency_unit_amount',
      'loyalty.point_value', 'loyalty.minimum_points_to_redeem', 'loyalty.maximum_discount_percent',
      'loyalty.points_expiration_enabled', 'loyalty.points_expiration_days',
      'loyalty.earn_points_on_discounted_sales', 'loyalty.earn_points_on_tax_included_amount',
      'loyalty.allow_points_redemption', 'loyalty.tiers_config',
    ],
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
  const [referenceValues, setReferenceValues] = useState({})

  useEffect(() => {
    Promise.all([settingsApi.getAll(), settingsApi.getReferenceValues()])
      .then(([data, refs]) => {
        setSettings(data)
        setReferenceValues(refs)
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

  const getSelectOptions = (meta) => {
    if (!meta?.referenceCategory) return null
    const options = referenceValues[meta.referenceCategory] ?? []
    return options.map((opt) => ({ value: opt.code, label: opt.label }))
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
        <Card className="p-6">
          <InstallationIdPanel />
        </Card>
        {GROUPS.map((group) => (
          <Card key={group.title} className="p-6">
            <h3 className="text-sm font-semibold text-gray-900 mb-4">{group.title}</h3>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {group.keys.map((key) => {
                const meta = byKey[key]
                if (!meta) return null
                const isBool = meta.type === 'BOOLEAN'
                const isJson = meta.type === 'JSON'
                const selectOptions = getSelectOptions(meta)
                return (
                  <label key={key} className={`block ${isJson ? 'md:col-span-2' : ''}`}>
                    <span className="text-xs font-medium text-gray-500 uppercase tracking-wide">
                      {meta.description || key}
                    </span>
                    {selectOptions ? (
                      <select
                        className="mt-1 w-full border border-gray-200 rounded-lg px-3 py-2 text-sm"
                        value={values[key] ?? selectOptions[0]?.value ?? ''}
                        disabled={!canUpdate}
                        onChange={(e) => handleChange(key, e.target.value)}
                      >
                        {selectOptions.map((opt) => (
                          <option key={opt.value} value={opt.value}>{opt.label}</option>
                        ))}
                      </select>
                    ) : isBool ? (
                      <select
                        className="mt-1 w-full border border-gray-200 rounded-lg px-3 py-2 text-sm"
                        value={values[key] ?? 'false'}
                        disabled={!canUpdate}
                        onChange={(e) => handleChange(key, e.target.value)}
                      >
                        <option value="true">Oui</option>
                        <option value="false">Non</option>
                      </select>
                    ) : isJson ? (
                      <textarea
                        className="mt-1 w-full border border-gray-200 rounded-lg px-3 py-2 text-sm font-mono min-h-[120px]"
                        value={values[key] ?? ''}
                        disabled={!canUpdate}
                        onChange={(e) => handleChange(key, e.target.value)}
                      />
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
