import { useEffect, useState } from 'react'
import { stockApi, productsApi } from '../api'
import { PageHeader, Card, Button, Loading, Alert, Tabs, Badge } from '../components/ui'
import { useAsyncAction } from '../hooks/useAsyncAction'
import { useNotification } from '../context/NotificationContext'
import { getErrorMessage } from '../utils/errors'

export default function StockPage() {
  const notify = useNotification()
  const { run, submitting } = useAsyncAction()
  const [loading, setLoading] = useState(true)
  const [tab, setTab] = useState('stock')
  const [items, setItems] = useState([])
  const [movements, setMovements] = useState([])
  const [warehouses, setWarehouses] = useState([])
  const [locations, setLocations] = useState([])
  const [products, setProducts] = useState([])
  const [form, setForm] = useState({
    type: 'receipt',
    productId: '',
    variantId: '',
    warehouseId: '',
    locationId: '',
    quantityBase: '',
    packagingId: '',
    packagingQuantity: '',
    reference: '',
  })

  const load = async () => {
    setLoading(true)
    try {
      const [wh, prods, stk, mov] = await Promise.all([
        stockApi.getWarehouses(),
        productsApi.search({}),
        stockApi.getItems({}),
        stockApi.getMovements({}),
      ])
      setWarehouses(wh)
      setProducts(prods)
      setItems(stk)
      setMovements(mov)
      if (wh.length && !form.warehouseId) {
        setForm((f) => ({ ...f, warehouseId: String(wh[0].id) }))
        const locs = await stockApi.getLocations(wh[0].id)
        setLocations(locs)
        if (locs.length) setForm((f) => ({ ...f, locationId: String(locs[0].id) }))
      }
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

  const selectedProduct = products.find((p) => String(p.id) === form.productId)
  const variants = selectedProduct?.variantes || []

  const buildPayload = () => {
    const payload = {
      productId: Number(form.productId),
      variantId: form.variantId ? Number(form.variantId) : null,
      warehouseId: Number(form.warehouseId),
      locationId: Number(form.locationId),
      reference: form.reference || undefined,
    }
    if (form.packagingId && form.packagingQuantity) {
      payload.packagingId = Number(form.packagingId)
      payload.packagingQuantity = Number(form.packagingQuantity)
    } else {
      payload.quantityBase = Number(form.quantityBase)
    }
    return payload
  }

  const handleSubmit = () => {
    if (!form.productId || !form.warehouseId || !form.locationId) {
      notify.error('Produit, entrepot et emplacement obligatoires.')
      return
    }
    const apiCall = form.type === 'receipt' ? stockApi.receipt
      : form.type === 'issue' ? stockApi.issue
        : stockApi.adjust
    run(
      () => apiCall(buildPayload()),
      { successMessage: 'Mouvement enregistre', onSuccess: load },
    )
  }

  if (loading) return <Loading />

  const tabs = [
    { id: 'stock', label: 'Stock' },
    { id: 'move', label: 'Mouvement' },
    { id: 'history', label: 'Historique' },
  ]

  return (
    <>
      <PageHeader
        title="Gestion de stock"
        subtitle="Module 2 — entrepots, mouvements, reservations (unité de base)"
      />

      <Card className="p-4 mb-6 bg-emerald-50 border-emerald-100 text-sm text-emerald-900">
        Stock toujours en <strong>unité de base</strong>. Chaque operation crée un mouvement auditable.
        Disponible = physique − réservé.
      </Card>

      <Tabs tabs={tabs} active={tab} onChange={setTab} />

      {tab === 'stock' && (
        <Card className="overflow-hidden mt-4">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b text-left text-gray-500">
                <th className="px-5 py-3">Produit</th>
                <th className="px-5 py-3">Entrepot</th>
                <th className="px-5 py-3">Emplacement</th>
                <th className="px-5 py-3">Lot</th>
                <th className="px-5 py-3">Physique</th>
                <th className="px-5 py-3">Reserve</th>
                <th className="px-5 py-3">Disponible</th>
              </tr>
            </thead>
            <tbody>
              {items.length === 0 ? (
                <tr><td colSpan={7} className="px-5 py-8 text-center text-gray-400">Aucun stock</td></tr>
              ) : items.map((i) => (
                <tr key={i.id} className="border-b border-gray-50">
                  <td className="px-5 py-3">{i.productNom}</td>
                  <td className="px-5 py-3">{i.warehouseCode}</td>
                  <td className="px-5 py-3">{i.locationCode}</td>
                  <td className="px-5 py-3">{i.lotNumero || '—'}</td>
                  <td className="px-5 py-3">{i.quantityOnHand} {i.unitSymbole}</td>
                  <td className="px-5 py-3">{i.quantityReserved}</td>
                  <td className="px-5 py-3 font-medium">{i.quantityAvailable} {i.unitSymbole}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </Card>
      )}

      {tab === 'move' && (
        <Card className="p-6 mt-4 space-y-4">
          <div className="flex gap-2">
            {['receipt', 'issue', 'adjust'].map((t) => (
              <Button key={t} variant={form.type === t ? 'primary' : 'secondary'} onClick={() => setForm({ ...form, type: t })}>
                {t === 'receipt' ? 'Reception' : t === 'issue' ? 'Sortie' : 'Ajustement'}
              </Button>
            ))}
          </div>
          <div className="grid grid-cols-2 gap-4">
            <select value={form.productId} onChange={(e) => setForm({ ...form, productId: e.target.value, variantId: '' })}>
              <option value="">Produit</option>
              {products.map((p) => <option key={p.id} value={p.id}>{p.nom} ({p.sku})</option>)}
            </select>
            <select value={form.variantId} onChange={(e) => setForm({ ...form, variantId: e.target.value })}>
              <option value="">Variante (optionnel)</option>
              {variants.map((v) => <option key={v.id} value={v.id}>{v.sku}</option>)}
            </select>
            <select value={form.warehouseId} onChange={(e) => onWarehouseChange(e.target.value)}>
              <option value="">Entrepot</option>
              {warehouses.map((w) => <option key={w.id} value={w.id}>{w.nom} ({w.code})</option>)}
            </select>
            <select value={form.locationId} onChange={(e) => setForm({ ...form, locationId: e.target.value })}>
              <option value="">Emplacement</option>
              {locations.map((l) => <option key={l.id} value={l.id}>{l.nom} ({l.code})</option>)}
            </select>
            <input placeholder="Quantite (unité de base)" type="number" step="any" value={form.quantityBase} onChange={(e) => setForm({ ...form, quantityBase: e.target.value })} />
            <input placeholder="Reference" value={form.reference} onChange={(e) => setForm({ ...form, reference: e.target.value })} />
          </div>
          <Button onClick={handleSubmit} disabled={submitting}>Enregistrer le mouvement</Button>
        </Card>
      )}

      {tab === 'history' && (
        <Card className="overflow-hidden mt-4">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b text-left text-gray-500">
                <th className="px-5 py-3">Date</th>
                <th className="px-5 py-3">Type</th>
                <th className="px-5 py-3">Produit</th>
                <th className="px-5 py-3">Qté</th>
                <th className="px-5 py-3">Apres</th>
                <th className="px-5 py-3">Ref</th>
              </tr>
            </thead>
            <tbody>
              {movements.map((m) => (
                <tr key={m.id} className="border-b border-gray-50">
                  <td className="px-5 py-3 text-xs">{new Date(m.movementDate).toLocaleString()}</td>
                  <td className="px-5 py-3"><Badge>{m.movementType}</Badge></td>
                  <td className="px-5 py-3">{m.productNom}</td>
                  <td className="px-5 py-3">{m.quantity} {m.unitSymbole}</td>
                  <td className="px-5 py-3">{m.quantityOnHandAfter}</td>
                  <td className="px-5 py-3 text-xs">{m.reference || '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </Card>
      )}
    </>
  )
}
