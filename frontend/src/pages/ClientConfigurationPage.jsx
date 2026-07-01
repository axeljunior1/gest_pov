import { useCallback, useEffect, useMemo, useState } from 'react'
import { settingsApi } from '../api'
import { useAuth } from '../context/AuthContext'
import { PageHeader, Card, Button, Loading } from '../components/ui'
import { useNotification } from '../context/NotificationContext'
import { getErrorDetails, getErrorMessage, validateClientConfig, validateLogoFile } from '../utils/errors'

const PAYMENT_CODES = [
  { code: 'CASH', label: 'Espèces' },
  { code: 'CARD', label: 'Carte' },
  { code: 'MOBILE_MONEY', label: 'Mobile money' },
  { code: 'BANK_TRANSFER', label: 'Virement' },
]

const EMPTY_CONFIG = {
  company: {
    name: '', address: '', city: '', country: '', phone: '', email: '', taxId: '',
    logoUrl: '', currency: '', language: 'fr', timezone: 'Europe/Paris', dateFormat: 'dd/MM/yyyy',
  },
  pos: {
    registerName: 'Caisse 1', salePrefix: 'TK', ticketFormat: 'SIMPLE', ticketFooter: '',
    ticketShowLogo: true, autoPrintAfterSale: false, changeGivingEnabled: true,
    paymentMethods: PAYMENT_CODES.map((p) => ({ ...p, enabled: p.code !== 'BANK_TRANSFER' })),
    allowPartialPayment: false, allowSplitPayment: true,
  },
  stock: {
    allowNegativeStock: false, lowStockThresholdDefault: 10, valuationMethod: 'WEIGHTED_AVERAGE',
    lowStockAlertsEnabled: true, multiWarehouseEnabled: true,
  },
  tax: {
    enabled: false, name: 'TVA', defaultRate: 0, pricesIncludeTax: true, autoApplyOnSales: true,
  },
}

function mergeConfig(data, referenceValues = {}) {
  const stock = { ...EMPTY_CONFIG.stock, ...data?.stock }
  const methods = referenceValues.STOCK_VALUATION_METHOD || []
  if (methods.length > 0 && !methods.some((m) => m.code === stock.valuationMethod)) {
    const preferred = methods.find((m) => m.code === 'WEIGHTED_AVERAGE') || methods[0]
    stock.valuationMethod = preferred.code
  }
  return {
    company: { ...EMPTY_CONFIG.company, ...data?.company },
    pos: {
      ...EMPTY_CONFIG.pos,
      ...data?.pos,
      paymentMethods: data?.pos?.paymentMethods?.length
        ? data.pos.paymentMethods
        : EMPTY_CONFIG.pos.paymentMethods,
    },
    stock,
    tax: { ...EMPTY_CONFIG.tax, ...data?.tax },
  }
}

function TicketPreview({ config }) {
  const c = config.company
  const p = config.pos
  const t = config.tax
  return (
    <div className="font-mono text-xs border border-dashed border-gray-300 rounded-lg p-4 bg-white max-w-xs mx-auto">
      {p.ticketShowLogo && c.logoUrl && (
        <img src={c.logoUrl} alt="Logo" className="h-10 mx-auto mb-2 object-contain" />
      )}
      <p className="font-bold text-center">{c.name || 'Nom entreprise'}</p>
      {c.address && <p className="text-center text-gray-500">{c.address}</p>}
      {(c.city || c.country) && (
        <p className="text-center text-gray-500">{[c.city, c.country].filter(Boolean).join(', ')}</p>
      )}
      {c.phone && <p className="text-center text-gray-500">{c.phone}</p>}
      <p className="text-center text-gray-500 mt-1">{p.registerName}</p>
      <p className="text-center mb-2">{p.salePrefix}-00001</p>
      <hr className="border-dashed my-2" />
      <div className="flex justify-between"><span>Article x1</span><span>10.00</span></div>
      {t.enabled && (
        <div className="flex justify-between text-gray-600">
          <span>{t.name || 'TVA'}</span><span>{Number(t.defaultRate || 0).toFixed(2)}%</span>
        </div>
      )}
      <div className="flex justify-between font-bold mt-1">
        <span>TOTAL {t.pricesIncludeTax ? 'TTC' : 'HT'}</span>
        <span>10.00 {c.currency}</span>
      </div>
      {p.ticketFooter && (
        <p className="text-center text-gray-500 mt-3 whitespace-pre-wrap">{p.ticketFooter}</p>
      )}
    </div>
  )
}

export default function ClientConfigurationPage() {
  const notify = useNotification()
  const { hasPermission } = useAuth()
  const canUpdate = hasPermission('settings.update')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [uploading, setUploading] = useState(false)
  const [refs, setRefs] = useState({})
  const [config, setConfig] = useState(EMPTY_CONFIG)
  const [fieldErrors, setFieldErrors] = useState({})
  const [saveSuccess, setSaveSuccess] = useState(false)

  const fieldError = (key) => fieldErrors[key]

  const clearFieldError = (key) => {
    setFieldErrors((prev) => {
      if (!prev[key]) return prev
      const next = { ...prev }
      delete next[key]
      return next
    })
  }

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const [data, referenceValues] = await Promise.all([
        settingsApi.getClientConfig(),
        settingsApi.getReferenceValues(),
      ])
      setConfig(mergeConfig(data, referenceValues))
      setRefs(referenceValues)
    } catch (e) {
      notify.error(getErrorMessage(e, { module: 'config' }))
    } finally {
      setLoading(false)
    }
  }, [notify])

  useEffect(() => { load() }, [load])

  const setCompany = (field, value) => {
    clearFieldError(`company.${field}`)
    setSaveSuccess(false)
    setConfig((prev) => ({
      ...prev, company: { ...prev.company, [field]: value },
    }))
  }
  const setPos = (field, value) => {
    clearFieldError(`pos.${field}`)
    setSaveSuccess(false)
    setConfig((prev) => ({
      ...prev, pos: { ...prev.pos, [field]: value },
    }))
  }
  const setStock = (field, value) => {
    clearFieldError(`stock.${field}`)
    setSaveSuccess(false)
    setConfig((prev) => ({
      ...prev, stock: { ...prev.stock, [field]: value },
    }))
  }
  const setTax = (field, value) => {
    clearFieldError(`tax.${field}`)
    setSaveSuccess(false)
    setConfig((prev) => ({
      ...prev, tax: { ...prev.tax, [field]: value },
    }))
  }

  const togglePayment = (code) => {
    clearFieldError('pos.paymentMethods')
    setSaveSuccess(false)
    setConfig((prev) => ({
      ...prev,
      pos: {
        ...prev.pos,
        paymentMethods: prev.pos.paymentMethods.map((m) =>
          m.code === code ? { ...m, enabled: !m.enabled } : m),
      },
    }))
  }

  const handleSave = async () => {
    if (!canUpdate || saving) return
    const localErrors = validateClientConfig(config)
    if (localErrors) {
      setFieldErrors(localErrors)
      notify.error('Corrigez les champs signalés avant d\'enregistrer.')
      return
    }
    setFieldErrors({})
    setSaving(true)
    setSaveSuccess(false)
    try {
      const saved = await settingsApi.updateClientConfig(config)
      setConfig(mergeConfig(saved))
      setSaveSuccess(true)
      notify.success('Configuration enregistrée avec succès.')
    } catch (e) {
      const details = getErrorDetails(e, { module: 'config' })
      if (details.fieldErrors) setFieldErrors(details.fieldErrors)
      notify.error(details.message)
    } finally {
      setSaving(false)
    }
  }

  const handleLogoUpload = async (e) => {
    const file = e.target.files?.[0]
    if (!file) return
    const logoError = validateLogoFile(file)
    if (logoError) {
      setFieldErrors((prev) => ({ ...prev, 'company.logo': logoError }))
      notify.error(logoError)
      e.target.value = ''
      return
    }
    clearFieldError('company.logo')
    if (uploading) return
    setUploading(true)
    try {
      const saved = await settingsApi.uploadCompanyLogo(file)
      setConfig(mergeConfig(saved))
      notify.success('Logo mis à jour avec succès.')
    } catch (err) {
      const details = getErrorDetails(err, { module: 'config' })
      if (details.fieldErrors) setFieldErrors(details.fieldErrors)
      else setFieldErrors((prev) => ({ ...prev, 'company.logo': details.message }))
      notify.error(details.message)
    } finally {
      setUploading(false)
      e.target.value = ''
    }
  }

  const currencyOptions = useMemo(
    () => (refs.CURRENCY || []).map((o) => ({ value: o.code, label: o.label })),
    [refs],
  )

  if (loading) return <Loading />

  return (
    <div>
      <PageHeader
        title="Configuration client"
        subtitle="Paramètres entreprise, caisse, stock et taxes pour le déploiement"
        action={canUpdate && (
          <Button onClick={handleSave} disabled={saving || uploading}>
            {saving ? 'Enregistrement…' : 'Enregistrer'}
          </Button>
        )}
      />

      {saveSuccess && (
        <div className="mb-4 rounded-lg border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-800">
          Configuration enregistrée avec succès.
        </div>
      )}

      <div className="grid grid-cols-1 xl:grid-cols-3 gap-6">
        <div className="xl:col-span-2 space-y-6">
          <Card className="p-6">
            <h3 className="text-sm font-semibold text-gray-900 mb-4">Entreprise</h3>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <label className="block md:col-span-2">
                <span className="text-xs font-medium text-gray-500 uppercase">Nom *</span>
                <input className={`input mt-1 w-full ${fieldError('company.name') ? 'border-red-500' : ''}`} value={config.company.name}
                  disabled={!canUpdate} onChange={(e) => setCompany('name', e.target.value)} />
                {fieldError('company.name') && (
                  <p className="text-xs text-red-600 mt-1">{fieldError('company.name')}</p>
                )}
              </label>
              <label className="block md:col-span-2">
                <span className="text-xs font-medium text-gray-500 uppercase">Adresse</span>
                <input className="input mt-1 w-full" value={config.company.address}
                  disabled={!canUpdate} onChange={(e) => setCompany('address', e.target.value)} />
              </label>
              <label className="block">
                <span className="text-xs font-medium text-gray-500 uppercase">Ville</span>
                <input className="input mt-1 w-full" value={config.company.city}
                  disabled={!canUpdate} onChange={(e) => setCompany('city', e.target.value)} />
              </label>
              <label className="block">
                <span className="text-xs font-medium text-gray-500 uppercase">Pays</span>
                <input className="input mt-1 w-full" value={config.company.country}
                  disabled={!canUpdate} onChange={(e) => setCompany('country', e.target.value)} />
              </label>
              <label className="block">
                <span className="text-xs font-medium text-gray-500 uppercase">Téléphone</span>
                <input className="input mt-1 w-full" value={config.company.phone}
                  disabled={!canUpdate} onChange={(e) => setCompany('phone', e.target.value)} />
              </label>
              <label className="block">
                <span className="text-xs font-medium text-gray-500 uppercase">Email</span>
                <input type="email" className={`input mt-1 w-full ${fieldError('company.email') ? 'border-red-500' : ''}`} value={config.company.email}
                  disabled={!canUpdate} onChange={(e) => setCompany('email', e.target.value)} />
                {fieldError('company.email') && (
                  <p className="text-xs text-red-600 mt-1">{fieldError('company.email')}</p>
                )}
              </label>
              <label className="block">
                <span className="text-xs font-medium text-gray-500 uppercase">N° fiscal / RCCM</span>
                <input className="input mt-1 w-full" value={config.company.taxId}
                  disabled={!canUpdate} onChange={(e) => setCompany('taxId', e.target.value)} />
              </label>
              <label className="block">
                <span className="text-xs font-medium text-gray-500 uppercase">Devise</span>
                <select className="input mt-1 w-full" value={config.company.currency}
                  disabled={!canUpdate} onChange={(e) => setCompany('currency', e.target.value)}>
                  {currencyOptions.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
                </select>
              </label>
              <label className="block">
                <span className="text-xs font-medium text-gray-500 uppercase">Fuseau horaire</span>
                <select className="input mt-1 w-full" value={config.company.timezone}
                  disabled={!canUpdate} onChange={(e) => setCompany('timezone', e.target.value)}>
                  {(refs.TIMEZONE || []).map((o) => <option key={o.code} value={o.code}>{o.label}</option>)}
                </select>
              </label>
              <label className="block">
                <span className="text-xs font-medium text-gray-500 uppercase">Langue</span>
                <select className="input mt-1 w-full" value={config.company.language}
                  disabled={!canUpdate} onChange={(e) => setCompany('language', e.target.value)}>
                  {(refs.LANGUAGE || []).map((o) => <option key={o.code} value={o.code}>{o.label}</option>)}
                </select>
              </label>
              <div className="md:col-span-2">
                <span className="text-xs font-medium text-gray-500 uppercase">Logo</span>
                <div className="mt-1 flex items-center gap-4">
                  {config.company.logoUrl && (
                    <img src={config.company.logoUrl} alt="Logo" className="h-12 object-contain border rounded p-1" />
                  )}
                  {canUpdate && (
                    <input type="file" accept="image/*" disabled={uploading} onChange={handleLogoUpload} />
                  )}
                  {fieldError('company.logo') && (
                    <p className="text-xs text-red-600 mt-1">{fieldError('company.logo')}</p>
                  )}
                </div>
              </div>
            </div>
          </Card>

          <Card className="p-6">
            <h3 className="text-sm font-semibold text-gray-900 mb-4">Caisse & tickets</h3>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <label className="block">
                <span className="text-xs font-medium text-gray-500 uppercase">Nom caisse</span>
                <input className="input mt-1 w-full" value={config.pos.registerName}
                  disabled={!canUpdate} onChange={(e) => setPos('registerName', e.target.value)} />
              </label>
              <label className="block">
                <span className="text-xs font-medium text-gray-500 uppercase">Préfixe ticket</span>
                <input className="input mt-1 w-full" value={config.pos.salePrefix}
                  disabled={!canUpdate} onChange={(e) => setPos('salePrefix', e.target.value)} />
              </label>
              <label className="block md:col-span-2">
                <span className="text-xs font-medium text-gray-500 uppercase">Pied de ticket</span>
                <textarea className="input mt-1 w-full min-h-[80px]" value={config.pos.ticketFooter}
                  disabled={!canUpdate} onChange={(e) => setPos('ticketFooter', e.target.value)} />
              </label>
              {[
                ['ticketShowLogo', 'Afficher le logo sur le ticket'],
                ['autoPrintAfterSale', 'Impression automatique après vente'],
                ['changeGivingEnabled', 'Rendu monnaie activé'],
                ['allowPartialPayment', 'Paiement partiel'],
                ['allowSplitPayment', 'Paiement fractionné'],
              ].map(([key, label]) => (
                <label key={key} className="flex items-center gap-2 text-sm">
                  <input type="checkbox" checked={!!config.pos[key]} disabled={!canUpdate}
                    onChange={(e) => setPos(key, e.target.checked)} />
                  {label}
                </label>
              ))}
            </div>
            <div className="mt-4">
              <p className="text-xs font-medium text-gray-500 uppercase mb-2">Moyens de paiement</p>
              {fieldError('pos.paymentMethods') && (
                <p className="text-xs text-red-600 mb-2">{fieldError('pos.paymentMethods')}</p>
              )}
              <div className="flex flex-wrap gap-3">
                {config.pos.paymentMethods.map((m) => (
                  <label key={m.code} className="flex items-center gap-2 text-sm border rounded-lg px-3 py-2">
                    <input type="checkbox" checked={m.enabled} disabled={!canUpdate}
                      onChange={() => togglePayment(m.code)} />
                    {m.label}
                  </label>
                ))}
              </div>
            </div>
          </Card>

          <Card className="p-6">
            <h3 className="text-sm font-semibold text-gray-900 mb-4">Stock</h3>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <label className="flex items-center gap-2 text-sm">
                <input type="checkbox" checked={config.stock.allowNegativeStock} disabled={!canUpdate}
                  onChange={(e) => setStock('allowNegativeStock', e.target.checked)} />
                Autoriser vente si stock insuffisant
              </label>
              <label className="flex items-center gap-2 text-sm">
                <input type="checkbox" checked={config.stock.lowStockAlertsEnabled} disabled={!canUpdate}
                  onChange={(e) => setStock('lowStockAlertsEnabled', e.target.checked)} />
                Alertes stock faible
              </label>
              <label className="flex items-center gap-2 text-sm">
                <input type="checkbox" checked={config.stock.multiWarehouseEnabled} disabled={!canUpdate}
                  onChange={(e) => setStock('multiWarehouseEnabled', e.target.checked)} />
                Multi-entrepôt
              </label>
              <label className="block">
                <span className="text-xs font-medium text-gray-500 uppercase">Seuil stock faible</span>
                <input type="number" className={`input mt-1 w-full ${fieldError('stock.lowStockThresholdDefault') ? 'border-red-500' : ''}`} value={config.stock.lowStockThresholdDefault}
                  disabled={!canUpdate}
                  onChange={(e) => setStock('lowStockThresholdDefault', Number(e.target.value))} />
                {fieldError('stock.lowStockThresholdDefault') && (
                  <p className="text-xs text-red-600 mt-1">{fieldError('stock.lowStockThresholdDefault')}</p>
                )}
              </label>
              <label className="block">
                <span className="text-xs font-medium text-gray-500 uppercase">Valorisation</span>
                <select className="input mt-1 w-full" value={config.stock.valuationMethod}
                  disabled={!canUpdate}
                  onChange={(e) => setStock('valuationMethod', e.target.value)}>
                  {(refs.STOCK_VALUATION_METHOD || []).map((o) => (
                    <option key={o.code} value={o.code}>{o.label}</option>
                  ))}
                </select>
              </label>
            </div>
          </Card>

          <Card className="p-6">
            <h3 className="text-sm font-semibold text-gray-900 mb-4">Taxes</h3>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <label className="flex items-center gap-2 text-sm md:col-span-2">
                <input type="checkbox" checked={config.tax.enabled} disabled={!canUpdate}
                  onChange={(e) => setTax('enabled', e.target.checked)} />
                Activer les taxes
              </label>
              <label className="block">
                <span className="text-xs font-medium text-gray-500 uppercase">Nom taxe</span>
                <input className="input mt-1 w-full" value={config.tax.name}
                  disabled={!canUpdate} onChange={(e) => setTax('name', e.target.value)} />
              </label>
              <label className="block">
                <span className="text-xs font-medium text-gray-500 uppercase">Taux par défaut (%)</span>
                <input type="number" step="0.01" className={`input mt-1 w-full ${fieldError('tax.defaultRate') ? 'border-red-500' : ''}`} value={config.tax.defaultRate}
                  disabled={!canUpdate} onChange={(e) => setTax('defaultRate', Number(e.target.value))} />
                {fieldError('tax.defaultRate') && (
                  <p className="text-xs text-red-600 mt-1">{fieldError('tax.defaultRate')}</p>
                )}
              </label>
              <label className="flex items-center gap-2 text-sm">
                <input type="checkbox" checked={config.tax.pricesIncludeTax} disabled={!canUpdate}
                  onChange={(e) => setTax('pricesIncludeTax', e.target.checked)} />
                Prix affichés TTC
              </label>
              <label className="flex items-center gap-2 text-sm">
                <input type="checkbox" checked={config.tax.autoApplyOnSales} disabled={!canUpdate}
                  onChange={(e) => setTax('autoApplyOnSales', e.target.checked)} />
                Application auto sur les ventes
              </label>
            </div>
          </Card>
        </div>

        <div className="space-y-4">
          <Card className="p-6 sticky top-4">
            <h3 className="text-sm font-semibold text-gray-900 mb-4">Aperçu ticket</h3>
            <TicketPreview config={config} />
          </Card>
        </div>
      </div>
    </div>
  )
}
