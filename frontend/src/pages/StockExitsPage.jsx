import { useEffect, useState } from 'react'
import { stockExitsApi, stockApi, productsApi } from '../api'
import { PageHeader, Card, Button, Loading, Tabs, Badge, EmptyState } from '../components/ui'
import { useAsyncAction } from '../hooks/useAsyncAction'
import { useNotification } from '../context/NotificationContext'
import { getErrorMessage } from '../utils/errors'

const REASONS = [
  { value: 'SALE', label: 'Vente' },
  { value: 'INTERNAL_USE', label: 'Consommation interne' },
  { value: 'DAMAGED', label: 'Casse' },
  { value: 'LOST', label: 'Perte' },
  { value: 'DONATION', label: 'Don' },
  { value: 'RETURN_SUPPLIER', label: 'Retour fournisseur' },
  { value: 'OTHER', label: 'Autre' },
]

const reasonLabel = (value) => REASONS.find((r) => r.value === value)?.label ?? value

const emptyLine = () => ({
  productId: '',
  variantId: '',
  packagingId: '',
  quantityInput: '',
  notes: '',
})

const statusTone = {
  DRAFT: 'warning',
  VALIDATED: 'success',
  CANCELLED: 'default',
}

export default function StockExitsPage() {
  const notify = useNotification()
  const { run, submitting } = useAsyncAction()
  const [loading, setLoading] = useState(true)
  const [tab, setTab] = useState('list')
  const [exits, setExits] = useState([])
  const [selected, setSelected] = useState(null)
  const [warehouses, setWarehouses] = useState([])
  const [locations, setLocations] = useState([])
  const [products, setProducts] = useState([])
  const [packagings, setPackagings] = useState({})
  const [productDetails, setProductDetails] = useState({})
  const [filters, setFilters] = useState({ status: '', warehouseId: '', reason: '' })

  const [form, setForm] = useState({
    warehouseId: '',
    locationId: '',
    exitDate: new Date().toISOString().slice(0, 10),
    reason: 'SALE',
    notes: '',
    lignes: [emptyLine()],
  })

  const loadExits = async () => {
    const params = {}
    if (filters.status) params.status = filters.status
    if (filters.warehouseId) params.warehouseId = Number(filters.warehouseId)
    if (filters.reason) params.reason = filters.reason
    setExits(await stockExitsApi.list(params))
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
      await loadExits()
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
        setProductDetails((prev) => ({ ...prev, [productId]: detail }))
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
    locationId: Number(form.locationId),
    exitDate: form.exitDate,
    reason: form.reason,
    notes: form.notes || null,
    lignes: form.lignes
      .filter((l) => l.productId && l.quantityInput)
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
      () => stockExitsApi.create(payload),
      {
        successMessage: 'Sortie créée en brouillon',
        onSuccess: async (created) => {
          setSelected(created)
          setTab('detail')
          await loadExits()
        },
      },
    )
  }

  const handleValidate = (id) => {
    if (!confirm('Valider cette sortie ? Le stock sera diminué.')) return
    run(
      () => stockExitsApi.validate(id),
      {
        successMessage: 'Sortie validée — stock mis à jour',
        onSuccess: async (updated) => {
          setSelected(updated)
          await loadExits()
        },
      },
    )
  }

  const handleCancel = (id) => {
    if (!confirm('Annuler cette sortie ?')) return
    run(
      () => stockExitsApi.cancel(id),
      {
        successMessage: 'Sortie annulée',
        onSuccess: async (updated) => {
          setSelected(updated)
          await loadExits()
        },
      },
    )
  }

  const openDetail = async (id) => {
    try {
      setSelected(await stockExitsApi.getById(id))
      setTab('detail')
    } catch (e) {
      notify.error(getErrorMessage(e))
    }
  }

  if (loading) return <Loading />

  return (
    <div>
      <PageHeader
        title="Sorties de stock"
        subtitle="Ventes, consommations, casse, pertes et autres sorties"
        action={
          tab === 'list' && (
            <Button onClick={() => {
              setForm({
                warehouseId: warehouses[0] ? String(warehouses[0].id) : '',
                locationId: '',
                exitDate: new Date().toISOString().slice(0, 10),
                reason: 'SALE',
                notes: '',
                lignes: [emptyLine()],
              })
              if (warehouses[0]) onWarehouseChange(String(warehouses[0].id))
              setTab('create')
            }}>
              Nouvelle sortie
            </Button>
          )
        }
      />

      <Tabs
        tabs={[
          { id: 'list', label: 'Liste' },
          { id: 'create', label: 'Nouvelle sortie' },
          ...(selected ? [{ id: 'detail', label: `Détail ${selected.exitNumber}` }] : []),
        ]}
        active={tab}
        onChange={setTab}
      />

      {tab === 'list' && (
        <Card className="p-6">
          <div className="flex flex-wrap gap-3 mb-4">
            <select
              value={filters.status}
              onChange={(e) => setFilters({ ...filters, status: e.target.value })}
              className="text-sm border rounded-lg px-3 py-2"
            >
              <option value="">Tous statuts</option>
              <option value="DRAFT">Brouillon</option>
              <option value="VALIDATED">Validée</option>
              <option value="CANCELLED">Annulée</option>
            </select>
            <select
              value={filters.warehouseId}
              onChange={(e) => setFilters({ ...filters, warehouseId: e.target.value })}
              className="text-sm border rounded-lg px-3 py-2"
            >
              <option value="">Tous entrepôts</option>
              {warehouses.map((w) => (
                <option key={w.id} value={w.id}>{w.code} — {w.nom}</option>
              ))}
            </select>
            <select
              value={filters.reason}
              onChange={(e) => setFilters({ ...filters, reason: e.target.value })}
              className="text-sm border rounded-lg px-3 py-2"
            >
              <option value="">Tous motifs</option>
              {REASONS.map((r) => (
                <option key={r.value} value={r.value}>{r.label}</option>
              ))}
            </select>
            <Button variant="secondary" onClick={loadExits}>Filtrer</Button>
          </div>

          {exits.length === 0 ? (
            <EmptyState message="Aucune sortie de stock." />
          ) : (
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b text-left text-gray-500">
                  <th className="py-2 font-medium">N° sortie</th>
                  <th className="py-2 font-medium">Date</th>
                  <th className="py-2 font-medium">Entrepôt</th>
                  <th className="py-2 font-medium">Motif</th>
                  <th className="py-2 font-medium">Lignes</th>
                  <th className="py-2 font-medium">Statut</th>
                </tr>
              </thead>
              <tbody>
                {exits.map((e) => (
                  <tr
                    key={e.id}
                    className="border-b border-gray-50 hover:bg-gray-50/50 cursor-pointer"
                    onClick={() => openDetail(e.id)}
                  >
                    <td className="py-3 font-mono text-xs">
                    <span className="inline-flex items-center gap-2">
                      {e.exitNumber}
                      {e.posOrigin && <Badge tone="info">POS</Badge>}
                    </span>
                  </td>
                    <td className="py-3">{e.exitDate}</td>
                    <td className="py-3">{e.warehouseCode}</td>
                    <td className="py-3">{reasonLabel(e.reason)}</td>
                    <td className="py-3">{e.lignes?.length ?? 0}</td>
                    <td className="py-3">
                      <Badge tone={statusTone[e.status] || 'default'}>{e.status}</Badge>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </Card>
      )}

      {tab === 'create' && (
        <Card className="p-6 space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
            <div>
              <label className="text-xs text-gray-500">Entrepôt *</label>
              <select
                value={form.warehouseId}
                onChange={(e) => onWarehouseChange(e.target.value)}
                className="w-full mt-1 border rounded-lg px-3 py-2 text-sm"
              >
                <option value="">—</option>
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
              >
                <option value="">—</option>
                {locations.map((l) => (
                  <option key={l.id} value={l.id}>{l.code}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="text-xs text-gray-500">Date</label>
              <input
                type="date"
                value={form.exitDate}
                onChange={(e) => setForm({ ...form, exitDate: e.target.value })}
                className="w-full mt-1 border rounded-lg px-3 py-2 text-sm"
              />
            </div>
            <div>
              <label className="text-xs text-gray-500">Motif *</label>
              <select
                value={form.reason}
                onChange={(e) => setForm({ ...form, reason: e.target.value })}
                className="w-full mt-1 border rounded-lg px-3 py-2 text-sm"
              >
                {REASONS.map((r) => (
                  <option key={r.value} value={r.value}>{r.label}</option>
                ))}
              </select>
            </div>
          </div>
          <div>
            <label className="text-xs text-gray-500">Notes</label>
            <input
              value={form.notes}
              onChange={(e) => setForm({ ...form, notes: e.target.value })}
              className="w-full mt-1 border rounded-lg px-3 py-2 text-sm"
              placeholder="Commentaire optionnel"
            />
          </div>

          <div className="space-y-3">
            <h3 className="text-sm font-medium">Lignes</h3>
            {form.lignes.map((line, idx) => {
              const detail = productDetails[line.productId]
              const variants = detail?.variantes ?? []
              const variantsLoading = Boolean(line.productId && !detail)
              const pkgs = packagings[line.productId] || []
              return (
                <div key={idx} className="grid grid-cols-1 md:grid-cols-5 gap-2 p-3 bg-gray-50 rounded-lg">
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
                    value={line.variantId}
                    disabled={!line.productId || variantsLoading}
                    onChange={(e) => {
                      const lignes = [...form.lignes]
                      lignes[idx] = { ...lignes[idx], variantId: e.target.value }
                      setForm({ ...form, lignes })
                    }}
                    className="text-sm border rounded-lg px-2 py-2 disabled:bg-gray-100 disabled:cursor-not-allowed"
                  >
                    <option value="">
                      {variantsLoading
                        ? 'Chargement…'
                        : `Variante${variants.length > 1 ? ' *' : ''}`}
                    </option>
                    {variants.map((v) => (
                      <option key={v.id} value={String(v.id)}>
                        {v.label || [v.couleur, v.taille].filter(Boolean).join(' ') || v.sku}
                      </option>
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
                    {pkgs.map((pkg) => (
                      <option key={pkg.id} value={pkg.id}>{pkg.nom}</option>
                    ))}
                  </select>
                  <input
                    type="number"
                    min="0"
                    step="any"
                    placeholder="Quantité *"
                    value={line.quantityInput}
                    onChange={(e) => {
                      const lignes = [...form.lignes]
                      lignes[idx] = { ...lignes[idx], quantityInput: e.target.value }
                      setForm({ ...form, lignes })
                    }}
                    className="text-sm border rounded-lg px-2 py-2"
                  />
                  <div className="flex gap-2">
                    <input
                      placeholder="Notes"
                      value={line.notes}
                      onChange={(e) => {
                        const lignes = [...form.lignes]
                        lignes[idx] = { ...lignes[idx], notes: e.target.value }
                        setForm({ ...form, lignes })
                      }}
                      className="flex-1 text-sm border rounded-lg px-2 py-2"
                    />
                    {form.lignes.length > 1 && (
                      <Button
                        variant="ghost"
                        className="text-xs text-red-600"
                        onClick={() => setForm({ ...form, lignes: form.lignes.filter((_, i) => i !== idx) })}
                      >
                        ✕
                      </Button>
                    )}
                  </div>
                </div>
              )
            })}
            <Button
              variant="secondary"
              className="text-xs"
              onClick={() => setForm({ ...form, lignes: [...form.lignes, emptyLine()] })}
            >
              + Ligne
            </Button>
          </div>

          <div className="flex gap-2 pt-2">
            <Button onClick={handleCreate} disabled={submitting}>Enregistrer brouillon</Button>
            <Button variant="secondary" onClick={() => setTab('list')}>Annuler</Button>
          </div>
        </Card>
      )}

      {tab === 'detail' && selected && (
        <Card className="p-6 space-y-4">
          <div className="flex flex-wrap items-start justify-between gap-4">
            <div>
              <h3 className="text-lg font-semibold font-mono">{selected.exitNumber}</h3>
              <p className="text-sm text-gray-500 mt-1">
                {selected.exitDate} · {selected.warehouseCode}/{selected.locationCode} · {reasonLabel(selected.reason)}
              </p>
              <div className="mt-2 flex flex-wrap gap-2">
                <Badge tone={statusTone[selected.status] || 'default'}>{selected.status}</Badge>
                {selected.posOrigin && <Badge tone="info">POS</Badge>}
              </div>
              {selected.saleNumber && (
                <p className="text-sm text-gray-600 mt-2">
                  Vente caisse : <span className="font-mono">{selected.saleNumber}</span>
                </p>
              )}
            </div>
            <div className="flex gap-2">
              {!selected.posOrigin && selected.status === 'DRAFT' && (
                <Button onClick={() => handleValidate(selected.id)} disabled={submitting}>
                  Valider
                </Button>
              )}
              {!selected.posOrigin && selected.status !== 'CANCELLED' && (
                <Button variant="secondary" onClick={() => handleCancel(selected.id)} disabled={submitting}>
                  Annuler
                </Button>
              )}
            </div>
          </div>

          {selected.notes && (
            <p className="text-sm text-gray-600">{selected.notes}</p>
          )}

          <table className="w-full text-sm">
            <thead>
              <tr className="border-b text-left text-gray-500">
                <th className="py-2">Produit</th>
                <th className="py-2">Saisie</th>
                <th className="py-2">Unité base</th>
              </tr>
            </thead>
            <tbody>
              {selected.lignes?.map((l) => (
                <tr key={l.id} className="border-b border-gray-50">
                  <td className="py-2">{l.productNom}</td>
                  <td className="py-2">
                    {l.quantityInput} {l.packagingNom || l.unitSymbole || ''}
                  </td>
                  <td className="py-2 font-medium">
                    {l.quantityInBaseUnit} {l.unitSymbole || ''}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          <p className="text-xs text-gray-400">
            Créée par {selected.createdBy}
            {selected.validatedBy && ` · Validée par ${selected.validatedBy}`}
            {selected.cancelledBy && ` · Annulée par ${selected.cancelledBy}`}
            {selected.posOrigin && ' · Document généré automatiquement depuis le POS (lecture seule)'}
          </p>
        </Card>
      )}
    </div>
  )
}
