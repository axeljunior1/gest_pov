import { useEffect, useState } from 'react'
import { stockEntriesApi, stockApi, productsApi, suppliersApi } from '../api'
import { PageHeader, Card, Button, Loading, Tabs, Badge, EmptyState } from '../components/ui'
import { useAsyncAction } from '../hooks/useAsyncAction'
import { useNotification } from '../context/NotificationContext'
import { getErrorMessage } from '../utils/errors'

const emptyLine = () => ({
  productId: '',
  variantId: '',
  packagingId: '',
  quantityInput: '',
  unitCost: '',
  lotNumber: '',
  expiryDate: '',
  notes: '',
})

const statusTone = {
  DRAFT: 'warning',
  VALIDATED: 'success',
  CANCELLED: 'default',
}

export default function StockEntriesPage() {
  const notify = useNotification()
  const { run, submitting } = useAsyncAction()
  const [loading, setLoading] = useState(true)
  const [tab, setTab] = useState('list')
  const [entries, setEntries] = useState([])
  const [selected, setSelected] = useState(null)
  const [warehouses, setWarehouses] = useState([])
  const [locations, setLocations] = useState([])
  const [products, setProducts] = useState([])
  const [suppliers, setSuppliers] = useState([])
  const [packagings, setPackagings] = useState({})
  const [productDetails, setProductDetails] = useState({})
  const [filters, setFilters] = useState({ status: '', warehouseId: '' })

  const [form, setForm] = useState({
    supplierId: '',
    warehouseId: '',
    locationId: '',
    entryDate: new Date().toISOString().slice(0, 10),
    referenceDocument: '',
    notes: '',
    lignes: [emptyLine()],
  })

  const loadEntries = async () => {
    const params = {}
    if (filters.status) params.status = filters.status
    if (filters.warehouseId) params.warehouseId = Number(filters.warehouseId)
    const data = await stockEntriesApi.list(params)
    setEntries(data)
  }

  const load = async () => {
    setLoading(true)
    try {
      const [wh, prods, sups] = await Promise.all([
        stockApi.getWarehouses(),
        productsApi.search({}),
        suppliersApi.getAll(),
      ])
      setWarehouses(wh)
      setProducts(prods)
      setSuppliers(sups)
      await loadEntries()
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
    supplierId: form.supplierId ? Number(form.supplierId) : null,
    warehouseId: Number(form.warehouseId),
    locationId: Number(form.locationId),
    entryDate: form.entryDate,
    referenceDocument: form.referenceDocument || null,
    notes: form.notes || null,
    lignes: form.lignes
      .filter((l) => l.productId && l.quantityInput)
      .map((l) => ({
        productId: Number(l.productId),
        variantId: l.variantId ? Number(l.variantId) : null,
        packagingId: l.packagingId ? Number(l.packagingId) : null,
        quantityInput: Number(l.quantityInput),
        unitCost: l.unitCost ? Number(l.unitCost) : null,
        lotNumber: l.lotNumber || null,
        expiryDate: l.expiryDate || null,
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
    for (const line of form.lignes.filter((l) => l.productId && l.quantityInput)) {
      const detail = productDetails[line.productId]
      if (detail?.variantes?.length > 1 && !line.variantId) {
        notify.error(`Sélectionnez une variante pour « ${detail.nom} ».`)
        return
      }
    }
    run(
      () => stockEntriesApi.create(payload),
      {
        successMessage: 'Entrée créée en brouillon',
        onSuccess: async (created) => {
          setSelected(created)
          setTab('detail')
          await loadEntries()
        },
      },
    )
  }

  const handleValidate = (id) => {
    if (!confirm('Valider cette entrée ? Le stock sera augmenté.')) return
    run(
      () => stockEntriesApi.validate(id),
      {
        successMessage: 'Entrée validée — stock mis à jour',
        onSuccess: async (updated) => {
          setSelected(updated)
          await loadEntries()
        },
      },
    )
  }

  const handleCancel = (id) => {
    if (!confirm('Annuler cette entrée ?')) return
    run(
      () => stockEntriesApi.cancel(id),
      {
        successMessage: 'Entrée annulée',
        onSuccess: async (updated) => {
          setSelected(updated)
          await loadEntries()
        },
      },
    )
  }

  const openDetail = async (id) => {
    try {
      const detail = await stockEntriesApi.getById(id)
      setSelected(detail)
      setTab('detail')
    } catch (e) {
      notify.error(getErrorMessage(e))
    }
  }

  if (loading) return <Loading />

  return (
    <div>
      <PageHeader
        title="Entrées de stock"
        subtitle="Enregistrement des réceptions après achat externe"
        action={
          tab === 'list' && (
            <Button onClick={() => {
              setForm({
                supplierId: '',
                warehouseId: warehouses[0] ? String(warehouses[0].id) : '',
                locationId: '',
                entryDate: new Date().toISOString().slice(0, 10),
                referenceDocument: '',
                notes: '',
                lignes: [emptyLine()],
              })
              if (warehouses[0]) onWarehouseChange(String(warehouses[0].id))
              setTab('create')
            }}>
              Nouvelle entrée
            </Button>
          )
        }
      />

      <Tabs
        tabs={[
          { id: 'list', label: 'Liste' },
          { id: 'create', label: 'Nouvelle entrée' },
          ...(selected ? [{ id: 'detail', label: `Détail ${selected.entryNumber}` }] : []),
        ]}
        active={tab}
        onChange={setTab}
      />

      {tab === 'list' && (
        <Card className="p-6">
          <div className="flex gap-3 mb-4">
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
            <Button variant="secondary" onClick={loadEntries}>Filtrer</Button>
          </div>

          {entries.length === 0 ? (
            <EmptyState message="Aucune entrée de stock." />
          ) : (
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b text-left text-gray-500">
                  <th className="py-2 font-medium">N° entrée</th>
                  <th className="py-2 font-medium">Date</th>
                  <th className="py-2 font-medium">Entrepôt</th>
                  <th className="py-2 font-medium">Fournisseur</th>
                  <th className="py-2 font-medium">Lignes</th>
                  <th className="py-2 font-medium">Statut</th>
                </tr>
              </thead>
              <tbody>
                {entries.map((e) => (
                  <tr
                    key={e.id}
                    className="border-b border-gray-50 hover:bg-gray-50/50 cursor-pointer"
                    onClick={() => openDetail(e.id)}
                  >
                    <td className="py-3 font-mono text-xs">{e.entryNumber}</td>
                    <td className="py-3">{e.entryDate}</td>
                    <td className="py-3">{e.warehouseCode}</td>
                    <td className="py-3">{e.supplierNom || '—'}</td>
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
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
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
              <label className="text-xs text-gray-500">Fournisseur</label>
              <select
                value={form.supplierId}
                onChange={(e) => setForm({ ...form, supplierId: e.target.value })}
                className="w-full mt-1 border rounded-lg px-3 py-2 text-sm"
              >
                <option value="">—</option>
                {suppliers.map((s) => (
                  <option key={s.id} value={s.id}>{s.nom}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="text-xs text-gray-500">Date d'entrée</label>
              <input
                type="date"
                value={form.entryDate}
                onChange={(e) => setForm({ ...form, entryDate: e.target.value })}
                className="w-full mt-1 border rounded-lg px-3 py-2 text-sm"
              />
            </div>
            <div>
              <label className="text-xs text-gray-500">Document justificatif</label>
              <input
                value={form.referenceDocument}
                onChange={(e) => setForm({ ...form, referenceDocument: e.target.value })}
                placeholder="N° BL, facture..."
                className="w-full mt-1 border rounded-lg px-3 py-2 text-sm"
              />
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
            <div className="flex items-center justify-between mb-3">
              <h3 className="text-sm font-semibold">Lignes produits</h3>
              <Button
                variant="secondary"
                className="text-xs"
                onClick={() => setForm({ ...form, lignes: [...form.lignes, emptyLine()] })}
              >
                + Ligne
              </Button>
            </div>

            {form.lignes.map((line, idx) => {
              const detail = productDetails[line.productId]
              const variants = detail?.variantes ?? []
              const variantsLoading = Boolean(line.productId && !detail)
              const pkgs = packagings[line.productId] || []
              return (
                <div key={idx} className="grid grid-cols-1 md:grid-cols-6 gap-2 mb-3 p-3 bg-gray-50 rounded-lg">
                  <select
                    value={line.productId}
                    onChange={(e) => onProductChange(idx, e.target.value)}
                    className="border rounded-lg px-2 py-1.5 text-sm"
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
                    className="border rounded-lg px-2 py-1.5 text-sm disabled:bg-gray-100 disabled:cursor-not-allowed"
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
                    className="border rounded-lg px-2 py-1.5 text-sm"
                  >
                    <option value="">Unité de base</option>
                    {pkgs.map((pk) => (
                      <option key={pk.id} value={pk.id}>{pk.nom} (×{pk.quantiteBase})</option>
                    ))}
                  </select>
                  <input
                    type="number"
                    placeholder="Quantité *"
                    value={line.quantityInput}
                    onChange={(e) => {
                      const lignes = [...form.lignes]
                      lignes[idx] = { ...lignes[idx], quantityInput: e.target.value }
                      setForm({ ...form, lignes })
                    }}
                    className="border rounded-lg px-2 py-1.5 text-sm"
                  />
                  <input
                    placeholder="N° lot"
                    value={line.lotNumber}
                    onChange={(e) => {
                      const lignes = [...form.lignes]
                      lignes[idx] = { ...lignes[idx], lotNumber: e.target.value }
                      setForm({ ...form, lignes })
                    }}
                    className="border rounded-lg px-2 py-1.5 text-sm"
                  />
                  <input
                    type="date"
                    title="Date péremption"
                    value={line.expiryDate}
                    onChange={(e) => {
                      const lignes = [...form.lignes]
                      lignes[idx] = { ...lignes[idx], expiryDate: e.target.value }
                      setForm({ ...form, lignes })
                    }}
                    className="border rounded-lg px-2 py-1.5 text-sm"
                  />
                </div>
              )
            })}
          </div>

          <div className="flex gap-2 pt-2">
            <Button disabled={submitting} onClick={handleCreate}>Enregistrer brouillon</Button>
            <Button variant="secondary" onClick={() => setTab('list')}>Annuler</Button>
          </div>
        </Card>
      )}

      {tab === 'detail' && selected && (
        <Card className="p-6">
          <div className="flex items-start justify-between mb-4">
            <div>
              <div className="flex items-center gap-2 mb-1">
                <h3 className="text-lg font-semibold">{selected.entryNumber}</h3>
                <Badge tone={statusTone[selected.status] || 'default'}>{selected.status}</Badge>
              </div>
              <p className="text-sm text-gray-500">
                {selected.entryDate} · {selected.warehouseCode}/{selected.locationCode}
                {selected.supplierNom && ` · ${selected.supplierNom}`}
              </p>
              {selected.referenceDocument && (
                <p className="text-sm text-gray-500">Doc : {selected.referenceDocument}</p>
              )}
            </div>
            <div className="flex gap-2">
              {selected.status === 'DRAFT' && (
                <Button disabled={submitting} onClick={() => handleValidate(selected.id)}>
                  Valider
                </Button>
              )}
              {selected.status !== 'CANCELLED' && (
                <Button variant="secondary" disabled={submitting} onClick={() => handleCancel(selected.id)}>
                  Annuler
                </Button>
              )}
            </div>
          </div>

          <table className="w-full text-sm">
            <thead>
              <tr className="border-b text-left text-gray-500">
                <th className="py-2">Produit</th>
                <th className="py-2">Qté saisie</th>
                <th className="py-2">Qté base</th>
                <th className="py-2">Conditionnement</th>
                <th className="py-2">Lot</th>
                <th className="py-2">Coût unit.</th>
              </tr>
            </thead>
            <tbody>
              {selected.lignes?.map((l) => (
                <tr key={l.id} className="border-b border-gray-50">
                  <td className="py-2">{l.productNom}</td>
                  <td className="py-2">{l.quantityInput}</td>
                  <td className="py-2 font-medium">{l.quantityInBaseUnit} {l.unitSymbole || ''}</td>
                  <td className="py-2">{l.packagingNom || '—'}</td>
                  <td className="py-2">{l.lotNumber || '—'}</td>
                  <td className="py-2">{l.unitCost ?? '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>

          {selected.validatedAt && (
            <p className="text-xs text-gray-400 mt-4">
              Validée le {new Date(selected.validatedAt).toLocaleString('fr-FR')} par {selected.validatedBy}
            </p>
          )}
        </Card>
      )}
    </div>
  )
}
