import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  purchaseOrdersApi, stockApi, productsApi, suppliersApi,
} from '../api'
import SmartEntitySelector from '../components/search/SmartEntitySelector'
import { useAuth } from '../context/AuthContext'
import { PageHeader, Card, Button, Loading, Badge, EmptyState } from '../components/ui'
import { useAsyncAction } from '../hooks/useAsyncAction'
import { useNotification } from '../context/NotificationContext'
import { getErrorMessage } from '../utils/errors'

const emptyLine = () => ({ productId: '', variantId: '', quantity: '', unitPrice: '', notes: '' })

const statusTone = {
  PENDING: 'warning',
  PARTIALLY_RECEIVED: 'info',
  DELIVERED: 'success',
  CANCELLED: 'default',
}

const statusLabel = {
  PENDING: 'En cours',
  PARTIALLY_RECEIVED: 'Part. reçue',
  DELIVERED: 'Livrée',
  CANCELLED: 'Annulée',
}

export default function PurchaseOrdersPage() {
  const notify = useNotification()
  const { hasPermission } = useAuth()
  const { run, submitting } = useAsyncAction()
  const [loading, setLoading] = useState(true)
  const [orders, setOrders] = useState([])
  const [selected, setSelected] = useState(null)
  const [suppliers, setSuppliers] = useState([])
  const [products, setProducts] = useState([])
  const [warehouses, setWarehouses] = useState([])
  const [locations, setLocations] = useState([])
  const [filterStatus, setFilterStatus] = useState('')
  const [showForm, setShowForm] = useState(false)

  const [form, setForm] = useState({
    supplierId: '',
    warehouseId: '',
    expectedDeliveryDate: new Date().toISOString().slice(0, 10),
    notes: '',
    lines: [emptyLine()],
  })

  const [receiveForm, setReceiveForm] = useState({ warehouseId: '', locationId: '', lines: {} })

  const canCreate = hasPermission('stock_entry.create')
  const canReceive = hasPermission('stock_entry.validate')

  const load = async () => {
    setLoading(true)
    try {
      const params = {}
      if (filterStatus) params.status = filterStatus
      const [ords, sups, prods, wh] = await Promise.all([
        purchaseOrdersApi.list(params),
        suppliersApi.getAll(),
        productsApi.search({}),
        stockApi.getWarehouses(),
      ])
      setOrders(ords)
      setSuppliers(sups)
      setProducts(prods)
      setWarehouses(wh)
    } catch (e) {
      notify.error(getErrorMessage(e))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [filterStatus])

  const openDetail = async (id) => {
    try {
      const detail = await purchaseOrdersApi.getById(id)
      setSelected(detail)
      const lines = {}
      detail.lines?.forEach((l) => {
        const remaining = Number(l.remainingQuantity ?? 0)
        if (remaining > 0) lines[l.id] = remaining
      })
      setReceiveForm({
        warehouseId: detail.warehouseId || warehouses[0]?.id || '',
        locationId: '',
        lines,
      })
      if (detail.warehouseId || warehouses[0]?.id) {
        const locs = await stockApi.getLocations(detail.warehouseId || warehouses[0].id)
        setLocations(locs)
        if (locs[0]) setReceiveForm((f) => ({ ...f, locationId: locs[0].id }))
      }
    } catch (e) {
      notify.error(getErrorMessage(e))
    }
  }

  const handleCreate = () => {
    if (!form.supplierId) {
      notify.error('Fournisseur obligatoire.')
      return
    }
    const lines = form.lines
      .filter((l) => l.productId && l.quantity)
      .map((l) => ({
        productId: Number(l.productId),
        variantId: l.variantId ? Number(l.variantId) : null,
        quantity: Number(l.quantity),
        unitPrice: l.unitPrice ? Number(l.unitPrice) : null,
        notes: l.notes || null,
      }))
    if (!lines.length) {
      notify.error('Ajoutez au moins une ligne produit.')
      return
    }
    run(
      () => purchaseOrdersApi.create({
        supplierId: Number(form.supplierId),
        warehouseId: form.warehouseId ? Number(form.warehouseId) : null,
        expectedDeliveryDate: form.expectedDeliveryDate,
        notes: form.notes || null,
        lines,
      }),
      {
        successMessage: 'Commande créée',
        onSuccess: () => { setShowForm(false); load() },
      },
    )
  }

  const handleReceive = () => {
    if (!selected) return
    const linePayload = Object.entries(receiveForm.lines)
      .filter(([, qty]) => Number(qty) > 0)
      .map(([lineId, qty]) => ({ lineId: Number(lineId), quantity: Number(qty) }))
    if (!linePayload.length) {
      notify.error('Indiquez les quantités à recevoir.')
      return
    }
    run(
      () => purchaseOrdersApi.receive(selected.id, {
        warehouseId: Number(receiveForm.warehouseId),
        locationId: Number(receiveForm.locationId),
        validateEntry: true,
        lines: linePayload,
      }),
      {
        successMessage: 'Réception enregistrée — entrée stock créée',
        onSuccess: () => { openDetail(selected.id); load() },
      },
    )
  }

  const handleCancel = (id) => {
    if (!confirm('Annuler cette commande ?')) return
    run(
      () => purchaseOrdersApi.cancel(id),
      { successMessage: 'Commande annulée', onSuccess: () => { setSelected(null); load() } },
    )
  }

  return (
    <>
      <PageHeader
        title="Commandes fournisseur"
        subtitle="Passer commande et réceptionner en entrée de stock"
        action={canCreate && (
          <Button onClick={() => setShowForm(!showForm)}>{showForm ? 'Fermer' : 'Nouvelle commande'}</Button>
        )}
      />

      {showForm && canCreate && (
        <Card className="p-5 mb-6 space-y-4">
          <div className="grid md:grid-cols-3 gap-3">
            <SmartEntitySelector
              entityType="supplier"
              value={form.supplierId}
              onChange={(id) => setForm({ ...form, supplierId: id })}
              options={suppliers}
              variant="compact"
            />
            <select value={form.warehouseId} onChange={(e) => setForm({ ...form, warehouseId: e.target.value })} className="w-full">
              <option value="">Entrepôt (optionnel)</option>
              {warehouses.map((w) => <option key={w.id} value={w.id}>{w.code} — {w.nom}</option>)}
            </select>
            <input type="date" value={form.expectedDeliveryDate} onChange={(e) => setForm({ ...form, expectedDeliveryDate: e.target.value })} className="w-full" />
          </div>
          <textarea placeholder="Notes" value={form.notes} onChange={(e) => setForm({ ...form, notes: e.target.value })} className="w-full text-sm" rows={2} />
          {form.lines.map((line, idx) => (
            <div key={idx} className="grid md:grid-cols-4 gap-2">
              <SmartEntitySelector
                entityType="product"
                value={line.productId}
                onChange={(id) => {
                  const next = [...form.lines]
                  next[idx] = { ...next[idx], productId: id, variantId: '' }
                  setForm({ ...form, lines: next })
                }}
                options={products}
                getOptionLabel={(p) => `${p.sku} — ${p.nom}`}
                variant="compact"
                showCriteriaHelp={idx === 0}
              />
              <input type="number" placeholder="Qté *" value={line.quantity} onChange={(e) => {
                const next = [...form.lines]
                next[idx] = { ...next[idx], quantity: e.target.value }
                setForm({ ...form, lines: next })
              }} className="w-full text-sm" />
              <input type="number" step="0.01" placeholder="P.U." value={line.unitPrice} onChange={(e) => {
                const next = [...form.lines]
                next[idx] = { ...next[idx], unitPrice: e.target.value }
                setForm({ ...form, lines: next })
              }} className="w-full text-sm" />
              <Button variant="ghost" className="text-xs" onClick={() => setForm({ ...form, lines: form.lines.filter((_, i) => i !== idx) })}>Suppr.</Button>
            </div>
          ))}
          <div className="flex gap-2">
            <Button variant="secondary" onClick={() => setForm({ ...form, lines: [...form.lines, emptyLine()] })}>+ Ligne</Button>
            <Button onClick={handleCreate} disabled={submitting}>Créer la commande</Button>
          </div>
        </Card>
      )}

      <Card className="p-4 mb-4">
        <select value={filterStatus} onChange={(e) => setFilterStatus(e.target.value)} className="text-sm">
          <option value="">Tous les statuts</option>
          {Object.entries(statusLabel).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
        </select>
      </Card>

      {loading ? <Loading /> : (
        <div className="grid lg:grid-cols-2 gap-6">
          <Card className="overflow-hidden">
            <table className="w-full text-sm">
              <thead className="text-left text-gray-500 border-b">
                <tr>
                  <th className="px-4 py-3">Réf.</th>
                  <th className="px-4 py-3">Fournisseur</th>
                  <th className="px-4 py-3">Livraison</th>
                  <th className="px-4 py-3">Statut</th>
                </tr>
              </thead>
              <tbody>
                {orders.length === 0 ? (
                  <tr><td colSpan={4} className="p-8 text-center text-gray-400"><EmptyState message="Aucune commande" /></td></tr>
                ) : orders.map((o) => (
                  <tr
                    key={o.id}
                    className={`border-b cursor-pointer hover:bg-gray-50 ${selected?.id === o.id ? 'bg-emerald-50' : ''}`}
                    onClick={() => openDetail(o.id)}
                  >
                    <td className="px-4 py-3 font-medium">{o.reference}</td>
                    <td className="px-4 py-3">{o.supplierNom}</td>
                    <td className="px-4 py-3">{o.expectedDeliveryDate}</td>
                    <td className="px-4 py-3"><Badge tone={statusTone[o.status]}>{statusLabel[o.status] || o.status}</Badge></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </Card>

          {selected && (
            <Card className="p-5 space-y-4">
              <div className="flex justify-between items-start">
                <div>
                  <h3 className="font-semibold">{selected.reference}</h3>
                  <p className="text-sm text-gray-500">{selected.supplierNom}</p>
                </div>
                <Badge tone={statusTone[selected.status]}>{statusLabel[selected.status]}</Badge>
              </div>
              {selected.stockEntryNumber && (
                <p className="text-sm">
                  Entrée stock :{' '}
                  <Link to="/stock/entries" className="text-emerald-600 hover:underline">{selected.stockEntryNumber}</Link>
                </p>
              )}
              <table className="w-full text-sm">
                <thead className="text-gray-500">
                  <tr>
                    <th className="text-left py-1">Produit</th>
                    <th className="text-right py-1">Cmd.</th>
                    <th className="text-right py-1">Reçu</th>
                    <th className="text-right py-1">Reste</th>
                  </tr>
                </thead>
                <tbody>
                  {selected.lines?.map((l) => (
                    <tr key={l.id} className="border-t">
                      <td className="py-2">{l.productSku} — {l.productNom}</td>
                      <td className="text-right">{l.quantity}</td>
                      <td className="text-right">{l.receivedQuantity}</td>
                      <td className="text-right font-medium">{l.remainingQuantity}</td>
                    </tr>
                  ))}
                </tbody>
              </table>

              {canReceive && selected.status !== 'DELIVERED' && selected.status !== 'CANCELLED' && (
                <div className="border-t pt-4 space-y-3">
                  <p className="text-sm font-medium">Réceptionner</p>
                  <div className="grid grid-cols-2 gap-2">
                    <select value={receiveForm.warehouseId} onChange={async (e) => {
                      const whId = e.target.value
                      setReceiveForm({ ...receiveForm, warehouseId: whId, locationId: '' })
                      if (whId) {
                        const locs = await stockApi.getLocations(whId)
                        setLocations(locs)
                        if (locs[0]) setReceiveForm((f) => ({ ...f, warehouseId: whId, locationId: locs[0].id }))
                      }
                    }} className="text-sm">
                      {warehouses.map((w) => <option key={w.id} value={w.id}>{w.code}</option>)}
                    </select>
                    <select value={receiveForm.locationId} onChange={(e) => setReceiveForm({ ...receiveForm, locationId: e.target.value })} className="text-sm">
                      {locations.map((l) => <option key={l.id} value={l.id}>{l.code}</option>)}
                    </select>
                  </div>
                  {selected.lines?.filter((l) => Number(l.remainingQuantity) > 0).map((l) => (
                    <div key={l.id} className="flex items-center gap-2 text-sm">
                      <span className="flex-1 truncate">{l.productNom}</span>
                      <input
                        type="number"
                        className="w-24 text-sm"
                        max={l.remainingQuantity}
                        value={receiveForm.lines[l.id] ?? ''}
                        onChange={(e) => setReceiveForm({ ...receiveForm, lines: { ...receiveForm.lines, [l.id]: e.target.value } })}
                      />
                    </div>
                  ))}
                  <Button onClick={handleReceive} disabled={submitting}>Réceptionner & valider entrée</Button>
                </div>
              )}

              {hasPermission('stock_entry.update') && selected.status !== 'DELIVERED' && selected.status !== 'CANCELLED' && (
                <Button variant="secondary" className="text-red-600" onClick={() => handleCancel(selected.id)} disabled={submitting}>
                  Annuler la commande
                </Button>
              )}
            </Card>
          )}
        </div>
      )}
    </>
  )
}
