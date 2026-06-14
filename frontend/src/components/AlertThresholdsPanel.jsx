import { useEffect, useState } from 'react'
import { alertSettingsApi, productsApi, stockApi } from '../api'
import { useAuth } from '../context/AuthContext'
import { Card, Button, Loading, Badge } from '../components/ui'
import { useAsyncAction } from '../hooks/useAsyncAction'
import { useNotification } from '../context/NotificationContext'
import { getErrorMessage } from '../utils/errors'

const SCOPES = [
  { value: 'PRODUCT', label: 'Par produit' },
  { value: 'WAREHOUSE', label: 'Par entrepôt' },
  { value: 'PRODUCT_WAREHOUSE', label: 'Produit + entrepôt' },
]

const scopeLabel = {
  GLOBAL: 'Global',
  PRODUCT: 'Produit',
  WAREHOUSE: 'Entrepôt',
  PRODUCT_WAREHOUSE: 'Produit + entrepôt',
}

const emptyForm = () => ({
  scope: 'PRODUCT',
  productId: '',
  warehouseId: '',
  minStockLevel: '',
  maxStockLevel: '',
  expiryAlertDays: '',
  dormantDays: '',
})

export default function AlertThresholdsPanel() {
  const notify = useNotification()
  const { hasPermission } = useAuth()
  const canManage = hasPermission('alerts.manage')
  const { run, submitting } = useAsyncAction()
  const [loading, setLoading] = useState(true)
  const [settings, setSettings] = useState([])
  const [products, setProducts] = useState([])
  const [warehouses, setWarehouses] = useState([])
  const [form, setForm] = useState(emptyForm())
  const [editingId, setEditingId] = useState(null)

  const load = async () => {
    setLoading(true)
    try {
      const [s, p, w] = await Promise.all([
        alertSettingsApi.list(),
        productsApi.search({}),
        stockApi.getWarehouses(),
      ])
      setSettings(s.filter((x) => x.scope !== 'GLOBAL'))
      setProducts(p)
      setWarehouses(w)
    } catch (e) {
      notify.error(getErrorMessage(e))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [])

  const resetForm = () => {
    setForm(emptyForm())
    setEditingId(null)
  }

  const handleSubmit = () => {
    const payload = {
      scope: form.scope,
      productId: form.productId ? Number(form.productId) : null,
      warehouseId: form.warehouseId ? Number(form.warehouseId) : null,
      minStockLevel: form.minStockLevel !== '' ? Number(form.minStockLevel) : null,
      maxStockLevel: form.maxStockLevel !== '' ? Number(form.maxStockLevel) : null,
      expiryAlertDays: form.expiryAlertDays !== '' ? Number(form.expiryAlertDays) : null,
      dormantDays: form.dormantDays !== '' ? Number(form.dormantDays) : null,
      actif: true,
    }
    run(
      () => editingId ? alertSettingsApi.update(editingId, payload) : alertSettingsApi.create(payload),
      {
        successMessage: editingId ? 'Seuil mis à jour' : 'Seuil créé',
        onSuccess: () => { resetForm(); load() },
      },
    )
  }

  const handleEdit = (s) => {
    setEditingId(s.id)
    setForm({
      scope: s.scope,
      productId: s.productId || '',
      warehouseId: s.warehouseId || '',
      minStockLevel: s.minStockLevel ?? '',
      maxStockLevel: s.maxStockLevel ?? '',
      expiryAlertDays: s.expiryAlertDays ?? '',
      dormantDays: s.dormantDays ?? '',
    })
  }

  const handleDelete = (id) => {
    if (!confirm('Supprimer ce seuil ?')) return
    run(
      () => alertSettingsApi.delete(id),
      { successMessage: 'Seuil supprimé', onSuccess: load },
    )
  }

  if (loading) return <Loading />

  return (
    <div className="space-y-6 mt-6">
      {canManage && (
        <Card className="p-5 space-y-3">
          <h3 className="text-sm font-medium">{editingId ? 'Modifier le seuil' : 'Nouveau seuil'}</h3>
          <div className="grid md:grid-cols-3 gap-3">
            <select value={form.scope} onChange={(e) => setForm({ ...form, scope: e.target.value })} className="text-sm">
              {SCOPES.map((s) => <option key={s.value} value={s.value}>{s.label}</option>)}
            </select>
            {(form.scope === 'PRODUCT' || form.scope === 'PRODUCT_WAREHOUSE') && (
              <select value={form.productId} onChange={(e) => setForm({ ...form, productId: e.target.value })} className="text-sm">
                <option value="">Produit *</option>
                {products.map((p) => <option key={p.id} value={p.id}>{p.sku} — {p.nom}</option>)}
              </select>
            )}
            {(form.scope === 'WAREHOUSE' || form.scope === 'PRODUCT_WAREHOUSE') && (
              <select value={form.warehouseId} onChange={(e) => setForm({ ...form, warehouseId: e.target.value })} className="text-sm">
                <option value="">Entrepôt *</option>
                {warehouses.map((w) => <option key={w.id} value={w.id}>{w.code}</option>)}
              </select>
            )}
          </div>
          <div className="grid md:grid-cols-4 gap-3">
            <input type="number" placeholder="Stock min" value={form.minStockLevel} onChange={(e) => setForm({ ...form, minStockLevel: e.target.value })} className="text-sm" />
            <input type="number" placeholder="Stock max" value={form.maxStockLevel} onChange={(e) => setForm({ ...form, maxStockLevel: e.target.value })} className="text-sm" />
            <input type="number" placeholder="Jours péremption" value={form.expiryAlertDays} onChange={(e) => setForm({ ...form, expiryAlertDays: e.target.value })} className="text-sm" />
            <input type="number" placeholder="Jours dormance" value={form.dormantDays} onChange={(e) => setForm({ ...form, dormantDays: e.target.value })} className="text-sm" />
          </div>
          <div className="flex gap-2">
            <Button onClick={handleSubmit} disabled={submitting}>{editingId ? 'Mettre à jour' : 'Créer'}</Button>
            {editingId && <Button variant="secondary" onClick={resetForm}>Annuler</Button>}
          </div>
        </Card>
      )}

      <Card className="overflow-hidden">
        <table className="w-full text-sm">
          <thead className="text-left text-gray-500 border-b">
            <tr>
              <th className="px-4 py-3">Scope</th>
              <th className="px-4 py-3">Cible</th>
              <th className="px-4 py-3">Min</th>
              <th className="px-4 py-3">Max</th>
              <th className="px-4 py-3">Péremption</th>
              <th className="px-4 py-3" />
            </tr>
          </thead>
          <tbody>
            {settings.length === 0 ? (
              <tr><td colSpan={6} className="p-8 text-center text-gray-400">Aucun seuil personnalisé (seuil global dans Paramètres)</td></tr>
            ) : settings.map((s) => (
              <tr key={s.id} className="border-b">
                <td className="px-4 py-3"><Badge tone="info">{scopeLabel[s.scope]}</Badge></td>
                <td className="px-4 py-3 text-gray-600">
                  {s.productNom && <span>{s.productSku}</span>}
                  {s.warehouseCode && <span>{s.productNom ? ' · ' : ''}{s.warehouseCode}</span>}
                </td>
                <td className="px-4 py-3">{s.minStockLevel ?? '—'}</td>
                <td className="px-4 py-3">{s.maxStockLevel ?? '—'}</td>
                <td className="px-4 py-3">{s.expiryAlertDays ?? '—'}</td>
                <td className="px-4 py-3">
                  {canManage && (
                    <div className="flex gap-2">
                      <Button variant="ghost" className="text-xs" onClick={() => handleEdit(s)}>Modifier</Button>
                      <Button variant="ghost" className="text-xs text-red-600" onClick={() => handleDelete(s.id)}>Suppr.</Button>
                    </div>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </Card>
    </div>
  )
}
