import { useEffect, useState } from 'react'
import { inventoryCountsApi, stockApi, productsApi } from '../api'
import { PageHeader, Card, Button, Loading, Tabs, Badge, EmptyState } from '../components/ui'
import { useAsyncAction } from '../hooks/useAsyncAction'
import { useNotification } from '../context/NotificationContext'
import { getErrorMessage } from '../utils/errors'

const emptyLine = () => ({
  productId: '',
  variantId: '',
  packagingId: '',
  quantityInput: '',
  notes: '',
})

const statusTone = {
  DRAFT: 'warning',
  IN_PROGRESS: 'info',
  VALIDATED: 'success',
  CANCELLED: 'default',
}

export default function InventoryCountsPage() {
  const notify = useNotification()
  const { run, submitting } = useAsyncAction()
  const [loading, setLoading] = useState(true)
  const [tab, setTab] = useState('list')
  const [inventories, setInventories] = useState([])
  const [selected, setSelected] = useState(null)
  const [warehouses, setWarehouses] = useState([])
  const [locations, setLocations] = useState([])
  const [products, setProducts] = useState([])
  const [packagings, setPackagings] = useState({})
  const [filters, setFilters] = useState({ status: '', warehouseId: '' })

  const [form, setForm] = useState({
    warehouseId: '',
    locationId: '',
    notes: '',
    lignes: [emptyLine()],
  })

  const loadInventories = async () => {
    const params = {}
    if (filters.status) params.status = filters.status
    if (filters.warehouseId) params.warehouseId = Number(filters.warehouseId)
    const data = await inventoryCountsApi.list(params)
    setInventories(data)
  }

  const load = async () => {
    setLoading(true)
    try {
      const [wh, prods] = await Promise.all([
        stockApi.getWarehouses(),
        productsApi.search({}),
      ])
      setWarehouses(wh)
      setProducts(prods)
      await loadInventories()
    } catch (e) {
      notify.error(getErrorMessage(e))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [])

  const onWarehouseChange = async (warehouseId) => {
    setForm((f) => ({ ...f, warehouseId, locationId: '' }))
    if (warehouseId) {
      const locs = await stockApi.getLocations(warehouseId)
      setLocations(locs)
      if (locs.length) setForm((f) => ({ ...f, locationId: String(locs[0].id) }))
    } else {
      setLocations([])
    }
  }

  const onProductChange = async (idx, productId) => {
    let defaultVariantId = ''
    if (productId) {
      try {
        const detail = await productsApi.getById(productId)
        if (detail.variantes?.length === 1) {
          defaultVariantId = String(detail.variantes[0].id)
        }
      } catch { /* ignore */ }
    }
    setForm((f) => {
      const lignes = [...f.lignes]
      lignes[idx] = { ...lignes[idx], productId, variantId: defaultVariantId, packagingId: '' }
      return { ...f, lignes }
    })
    if (productId && !packagings[productId]) {
      try {
        const pkgs = await productsApi.getPackagings(productId)
        setPackagings((p) => ({ ...p, [productId]: pkgs }))
      } catch { /* ignore */ }
    }
  }

  const buildPayload = () => ({
    warehouseId: Number(form.warehouseId),
    locationId: form.locationId ? Number(form.locationId) : null,
    notes: form.notes || null,
    lignes: form.lignes
      .filter((l) => l.productId && l.quantityInput !== '')
      .map((l) => ({
        productId: Number(l.productId),
        variantId: l.variantId ? Number(l.variantId) : null,
        packagingId: l.packagingId ? Number(l.packagingId) : null,
        quantityInput: Number(l.quantityInput),
        notes: l.notes || null,
      })),
  })

  const handleCreate = () => {
    if (!form.warehouseId || !form.locationId) {
      notify.error('Entrepôt et emplacement obligatoires.')
      return
    }
    const payload = buildPayload()
    if (!payload.lignes.length) {
      notify.error('Ajoutez au moins une ligne produit.')
      return
    }
    run(
      () => inventoryCountsApi.create(payload),
      {
        successMessage: 'Inventaire créé en brouillon',
        onSuccess: async (created) => {
          setSelected(created)
          setTab('detail')
          await loadInventories()
        },
      },
    )
  }

  const handleStart = (id) => {
    run(
      () => inventoryCountsApi.start(id),
      {
        successMessage: 'Inventaire démarré',
        onSuccess: async (updated) => {
          setSelected(updated)
          await loadInventories()
        },
      },
    )
  }

  const handleValidate = (id) => {
    if (!confirm('Valider cet inventaire ? Les écarts corrigeront le stock.')) return
    run(
      () => inventoryCountsApi.validate(id),
      {
        successMessage: 'Inventaire validé',
        onSuccess: async (updated) => {
          setSelected(updated)
          await loadInventories()
        },
      },
    )
  }

  const handleCancel = (id) => {
    if (!confirm('Annuler cet inventaire ?')) return
    run(
      () => inventoryCountsApi.cancel(id),
      {
        successMessage: 'Inventaire annulé',
        onSuccess: async (updated) => {
          setSelected(updated)
          await loadInventories()
        },
      },
    )
  }

  const openDetail = async (id) => {
    try {
      const detail = await inventoryCountsApi.getById(id)
      setSelected(detail)
      setTab('detail')
    } catch (e) {
      notify.error(getErrorMessage(e))
    }
  }

  if (loading) return <Loading />

  const tabs = [
    { id: 'list', label: 'Liste' },
    { id: 'create', label: 'Nouvel inventaire' },
    ...(selected ? [{ id: 'detail', label: 'Détail' }] : []),
  ]

  return (
    <div>
      <PageHeader
        title="Inventaires physiques"
        subtitle="Comptage, écarts et correction automatique du stock"
      />

      <Card className="p-4 mb-6 bg-amber-50 border-amber-100 text-sm text-amber-900">
        Le stock théorique est recalculé à la validation. Seuls les écarts génèrent un mouvement{' '}
        <strong>INVENTORY</strong>. Quantités en unité de base (conditionnements convertis).
      </Card>

      <Tabs tabs={tabs} active={tab} onChange={setTab} />

      {tab === 'list' && (
        <Card className="p-4 mt-4">
          <div className="flex flex-wrap gap-3 mb-4">
            <select
              value={filters.warehouseId}
              onChange={(e) => setFilters({ ...filters, warehouseId: e.target.value })}
              className="text-sm border rounded-lg px-2 py-2"
            >
              <option value="">Tous entrepôts</option>
              {warehouses.map((w) => (
                <option key={w.id} value={w.id}>{w.code}</option>
              ))}
            </select>
            <select
              value={filters.status}
              onChange={(e) => setFilters({ ...filters, status: e.target.value })}
              className="text-sm border rounded-lg px-2 py-2"
            >
              <option value="">Tous statuts</option>
              {['DRAFT', 'IN_PROGRESS', 'VALIDATED', 'CANCELLED'].map((s) => (
                <option key={s} value={s}>{s}</option>
              ))}
            </select>
            <Button variant="secondary" onClick={loadInventories}>Filtrer</Button>
          </div>

          {inventories.length === 0 ? (
            <EmptyState message="Aucun inventaire." />
          ) : (
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b text-left text-gray-500">
                  <th className="py-2">N° inventaire</th>
                  <th className="py-2">Entrepôt</th>
                  <th className="py-2">Lignes</th>
                  <th className="py-2">Statut</th>
                </tr>
              </thead>
              <tbody>
                {inventories.map((inv) => (
                  <tr
                    key={inv.id}
                    className="border-b border-gray-50 hover:bg-gray-50/50 cursor-pointer"
                    onClick={() => openDetail(inv.id)}
                  >
                    <td className="py-3 font-mono text-xs">{inv.inventoryNumber}</td>
                    <td className="py-3">{inv.warehouseCode}</td>
                    <td className="py-3">{inv.lignes?.length ?? 0}</td>
                    <td className="py-3">
                      <Badge tone={statusTone[inv.status] || 'default'}>{inv.status}</Badge>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </Card>
      )}

      {tab === 'create' && (
        <Card className="p-6 mt-4 space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div>
              <label className="text-xs text-gray-500">Entrepôt *</label>
              <select
                value={form.warehouseId}
                onChange={(e) => onWarehouseChange(e.target.value)}
                className="w-full mt-1 border rounded-lg px-3 py-2 text-sm"
              >
                <option value="">Choisir</option>
                {warehouses.map((w) => (
                  <option key={w.id} value={w.id}>{w.code} — {w.nom}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="text-xs text-gray-500">Emplacement *</label>
              <select
                value={form.locationId}
                onChange={(e) => setForm({ ...form, locationId: e.target.value })}
                className="w-full mt-1 border rounded-lg px-3 py-2 text-sm"
                disabled={!form.warehouseId}
              >
                <option value="">Choisir</option>
                {locations.map((l) => (
                  <option key={l.id} value={l.id}>{l.code}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="text-xs text-gray-500">Notes</label>
              <input
                value={form.notes}
                onChange={(e) => setForm({ ...form, notes: e.target.value })}
                className="w-full mt-1 border rounded-lg px-3 py-2 text-sm"
              />
            </div>
          </div>

          <div className="border-t pt-4">
            <div className="flex justify-between mb-3">
              <h3 className="text-sm font-semibold">Lignes de comptage</h3>
              <Button
                variant="secondary"
                className="text-xs"
                onClick={() => setForm({ ...form, lignes: [...form.lignes, emptyLine()] })}
              >
                + Ligne
              </Button>
            </div>
            {form.lignes.map((line, idx) => {
              const pkgs = packagings[line.productId] || []
              return (
                <div key={idx} className="grid grid-cols-1 md:grid-cols-5 gap-2 mb-3 p-3 bg-gray-50 rounded-lg">
                  <select
                    value={line.productId}
                    onChange={(e) => onProductChange(idx, e.target.value)}
                    className="text-sm border rounded-lg px-2 py-2"
                  >
                    <option value="">Produit *</option>
                    {products.map((p) => (
                      <option key={p.id} value={p.id}>{p.nom}</option>
                    ))}
                  </select>
                  <select
                    value={line.packagingId}
                    onChange={(e) => {
                      const lignes = [...form.lignes]
                      lignes[idx] = { ...lignes[idx], packagingId: e.target.value }
                      setForm({ ...form, lignes })
                    }}
                    className="text-sm border rounded-lg px-2 py-2"
                  >
                    <option value="">Unité de base</option>
                    {pkgs.map((pk) => (
                      <option key={pk.id} value={pk.id}>{pk.nom} (×{pk.quantiteBase})</option>
                    ))}
                  </select>
                  <input
                    type="number"
                    placeholder="Qté comptée *"
                    value={line.quantityInput}
                    onChange={(e) => {
                      const lignes = [...form.lignes]
                      lignes[idx] = { ...lignes[idx], quantityInput: e.target.value }
                      setForm({ ...form, lignes })
                    }}
                    className="text-sm border rounded-lg px-2 py-2"
                  />
                  <input
                    placeholder="Notes"
                    value={line.notes}
                    onChange={(e) => {
                      const lignes = [...form.lignes]
                      lignes[idx] = { ...lignes[idx], notes: e.target.value }
                      setForm({ ...form, lignes })
                    }}
                    className="text-sm border rounded-lg px-2 py-2"
                  />
                </div>
              )
            })}
          </div>

          <Button onClick={handleCreate} disabled={submitting}>Créer brouillon</Button>
        </Card>
      )}

      {tab === 'detail' && selected && (
        <Card className="p-6 mt-4">
          <div className="flex flex-wrap items-start justify-between gap-4 mb-4">
            <div>
              <h2 className="text-lg font-semibold font-mono">{selected.inventoryNumber}</h2>
              <p className="text-sm text-gray-500">
                {selected.warehouseCode}
                {selected.locationCode ? ` / ${selected.locationCode}` : ''}
              </p>
            </div>
            <Badge tone={statusTone[selected.status] || 'default'}>{selected.status}</Badge>
          </div>

          <div className="text-sm text-gray-600 mb-4 space-y-1">
            <p>Créé par : {selected.createdBy}</p>
            {selected.validatedBy && <p>Validé par : {selected.validatedBy}</p>}
            {selected.notes && <p>Notes : {selected.notes}</p>}
          </div>

          <table className="w-full text-sm mb-4">
            <thead>
              <tr className="border-b text-left text-gray-500">
                <th className="py-2">Produit</th>
                <th className="py-2">Système</th>
                <th className="py-2">Compté</th>
                <th className="py-2">Écart</th>
              </tr>
            </thead>
            <tbody>
              {selected.lignes?.map((l) => (
                <tr key={l.id} className="border-b border-gray-50">
                  <td className="py-2">{l.productNom}</td>
                  <td className="py-2">{l.quantitySystem}</td>
                  <td className="py-2">{l.quantityCounted}</td>
                  <td className={`py-2 font-medium ${l.differenceQuantity !== 0 ? 'text-amber-700' : ''}`}>
                    {l.differenceQuantity ?? 0}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          <div className="flex gap-2">
            {selected.status === 'DRAFT' && (
              <Button variant="secondary" onClick={() => handleStart(selected.id)} disabled={submitting}>
                Démarrer
              </Button>
            )}
            {(selected.status === 'DRAFT' || selected.status === 'IN_PROGRESS') && (
              <>
                <Button onClick={() => handleValidate(selected.id)} disabled={submitting}>
                  Valider
                </Button>
                <Button variant="secondary" onClick={() => handleCancel(selected.id)} disabled={submitting}>
                  Annuler
                </Button>
              </>
            )}
          </div>
        </Card>
      )}
    </div>
  )
}
