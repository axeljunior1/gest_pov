import { useEffect, useState } from 'react'
import { stockMovementsApi, stockApi, productsApi } from '../api'
import { PageHeader, Card, Button, Loading, Badge, EmptyState } from '../components/ui'
import { useNotification } from '../context/NotificationContext'
import { useAuth } from '../context/AuthContext'
import { getErrorMessage } from '../utils/errors'

const MOVEMENT_TYPES = [
  'IN', 'OUT', 'ADJUSTMENT', 'INVENTORY', 'TRANSFER_IN', 'TRANSFER_OUT',
  'RETURN_IN', 'RETURN_OUT', 'RESERVATION', 'RELEASE',
]

const REFERENCE_TYPES = [
  'STOCK_ENTRY', 'STOCK_EXIT', 'INVENTORY_COUNT', 'ADJUSTMENT', 'TRANSFER', 'RETURN', 'MANUAL',
]

const typeTone = {
  IN: 'success',
  OUT: 'danger',
  ADJUSTMENT: 'warning',
  TRANSFER_IN: 'info',
  TRANSFER_OUT: 'info',
  RESERVATION: 'default',
  RELEASE: 'default',
  INVENTORY: 'warning',
}

export default function StockMovementsPage() {
  const notify = useNotification()
  const { hasPermission } = useAuth()
  const [loading, setLoading] = useState(true)
  const [movements, setMovements] = useState([])
  const [selected, setSelected] = useState(null)
  const [warehouses, setWarehouses] = useState([])
  const [products, setProducts] = useState([])
  const [filters, setFilters] = useState({
    productId: '',
    warehouseId: '',
    locationId: '',
    type: '',
    referenceType: '',
    referenceId: '',
    reference: '',
    createdBy: '',
    dateFrom: '',
    dateTo: '',
  })
  const [locations, setLocations] = useState([])

  const buildParams = () => {
    const params = {}
    if (filters.productId) params.productId = Number(filters.productId)
    if (filters.warehouseId) params.warehouseId = Number(filters.warehouseId)
    if (filters.locationId) params.locationId = Number(filters.locationId)
    if (filters.type) params.type = filters.type
    if (filters.referenceType) params.referenceType = filters.referenceType
    if (filters.referenceId) params.referenceId = Number(filters.referenceId)
    if (filters.reference) params.reference = filters.reference
    if (filters.createdBy) params.createdBy = filters.createdBy
    if (filters.dateFrom) params.dateFrom = filters.dateFrom
    if (filters.dateTo) params.dateTo = filters.dateTo
    return params
  }

  const loadMovements = async () => {
    setLoading(true)
    try {
      const data = await stockMovementsApi.list(buildParams())
      setMovements(data)
    } catch (e) {
      notify.error(getErrorMessage(e))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    Promise.all([stockApi.getWarehouses(), productsApi.search({})])
      .then(([wh, prods]) => {
        setWarehouses(wh)
        setProducts(prods)
      })
      .catch((e) => notify.error(getErrorMessage(e)))
      .finally(() => loadMovements())
  }, [])

  const onWarehouseFilterChange = async (warehouseId) => {
    setFilters((f) => ({ ...f, warehouseId, locationId: '' }))
    if (warehouseId) {
      const locs = await stockApi.getLocations(warehouseId)
      setLocations(locs)
    } else {
      setLocations([])
    }
  }

  const openDetail = async (id) => {
    try {
      const detail = await stockMovementsApi.getById(id)
      setSelected(detail)
    } catch (e) {
      notify.error(getErrorMessage(e))
    }
  }

  const handleExport = async () => {
    try {
      const blob = await stockMovementsApi.export(buildParams())
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = 'stock-movements.csv'
      a.click()
      URL.revokeObjectURL(url)
    } catch (e) {
      notify.error(getErrorMessage(e))
    }
  }

  const formatDate = (value) => {
    if (!value) return '—'
    return new Date(value).toLocaleString('fr-FR')
  }

  return (
    <div>
      <PageHeader
        title="Historique des mouvements"
        subtitle="Traçabilité complète de chaque changement de stock (lecture seule)"
        action={
          hasPermission('stock_movement.export') && (
            <Button variant="secondary" onClick={handleExport}>
              Exporter CSV
            </Button>
          )
        }
      />

      <Card className="p-4 mb-6 bg-slate-50 border-slate-100 text-sm text-slate-800">
        Chaque mouvement est <strong>immuable</strong> : créé automatiquement lors d'une entrée, sortie,
        ajustement, transfert, réservation ou inventaire. Quantités en unité de base.
      </Card>

      <Card className="p-4 mb-4">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-3">
          <select
            value={filters.productId}
            onChange={(e) => setFilters({ ...filters, productId: e.target.value })}
            className="text-sm border rounded-lg px-2 py-2"
          >
            <option value="">Tous produits</option>
            {products.map((p) => (
              <option key={p.id} value={p.id}>{p.nom}</option>
            ))}
          </select>
          <select
            value={filters.warehouseId}
            onChange={(e) => onWarehouseFilterChange(e.target.value)}
            className="text-sm border rounded-lg px-2 py-2"
          >
            <option value="">Tous entrepôts</option>
            {warehouses.map((w) => (
              <option key={w.id} value={w.id}>{w.code}</option>
            ))}
          </select>
          <select
            value={filters.locationId}
            onChange={(e) => setFilters({ ...filters, locationId: e.target.value })}
            className="text-sm border rounded-lg px-2 py-2"
            disabled={!filters.warehouseId}
          >
            <option value="">Tous emplacements</option>
            {locations.map((l) => (
              <option key={l.id} value={l.id}>{l.code}</option>
            ))}
          </select>
          <select
            value={filters.type}
            onChange={(e) => setFilters({ ...filters, type: e.target.value })}
            className="text-sm border rounded-lg px-2 py-2"
          >
            <option value="">Tous types</option>
            {MOVEMENT_TYPES.map((t) => (
              <option key={t} value={t}>{t}</option>
            ))}
          </select>
          <select
            value={filters.referenceType}
            onChange={(e) => setFilters({ ...filters, referenceType: e.target.value })}
            className="text-sm border rounded-lg px-2 py-2"
          >
            <option value="">Toutes origines</option>
            {REFERENCE_TYPES.map((t) => (
              <option key={t} value={t}>{t}</option>
            ))}
          </select>
          <input
            placeholder="ID document source"
            value={filters.referenceId}
            onChange={(e) => setFilters({ ...filters, referenceId: e.target.value })}
            className="text-sm border rounded-lg px-2 py-2"
          />
          <input
            placeholder="Référence"
            value={filters.reference}
            onChange={(e) => setFilters({ ...filters, reference: e.target.value })}
            className="text-sm border rounded-lg px-2 py-2"
          />
          <input
            placeholder="Utilisateur"
            value={filters.createdBy}
            onChange={(e) => setFilters({ ...filters, createdBy: e.target.value })}
            className="text-sm border rounded-lg px-2 py-2"
          />
          <input
            type="date"
            value={filters.dateFrom}
            onChange={(e) => setFilters({ ...filters, dateFrom: e.target.value })}
            className="text-sm border rounded-lg px-2 py-2"
          />
          <input
            type="date"
            value={filters.dateTo}
            onChange={(e) => setFilters({ ...filters, dateTo: e.target.value })}
            className="text-sm border rounded-lg px-2 py-2"
          />
        </div>
        <div className="mt-3 flex gap-2">
          <Button onClick={loadMovements}>Filtrer</Button>
          <Button
            variant="secondary"
            onClick={() => {
              setFilters({
                productId: '', warehouseId: '', locationId: '', type: '',
                referenceType: '', referenceId: '', reference: '', createdBy: '',
                dateFrom: '', dateTo: '',
              })
              setLocations([])
            }}
          >
            Réinitialiser
          </Button>
        </div>
      </Card>

      {loading ? (
        <Loading />
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
          <Card className="lg:col-span-2 overflow-hidden">
            {movements.length === 0 ? (
              <EmptyState message="Aucun mouvement trouvé." />
            ) : (
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b text-left text-gray-500">
                    <th className="px-4 py-3">Date</th>
                    <th className="px-4 py-3">Type</th>
                    <th className="px-4 py-3">Produit</th>
                    <th className="px-4 py-3">Qté</th>
                    <th className="px-4 py-3">Avant → Après</th>
                    <th className="px-4 py-3">Origine</th>
                  </tr>
                </thead>
                <tbody>
                  {movements.map((m) => (
                    <tr
                      key={m.id}
                      className="border-b border-gray-50 hover:bg-gray-50/50 cursor-pointer"
                      onClick={() => openDetail(m.id)}
                    >
                      <td className="px-4 py-3 text-xs">{formatDate(m.movementDate)}</td>
                      <td className="px-4 py-3">
                        <Badge tone={typeTone[m.movementType] || 'default'}>{m.movementType}</Badge>
                      </td>
                      <td className="px-4 py-3">{m.productNom}</td>
                      <td className="px-4 py-3">{m.quantity} {m.unitSymbole}</td>
                      <td className="px-4 py-3 text-xs">
                        {m.quantityBefore ?? '—'} → {m.quantityAfter ?? '—'}
                      </td>
                      <td className="px-4 py-3 text-xs">
                        {m.referenceType || '—'}
                        {m.referenceId ? ` #${m.referenceId}` : ''}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </Card>

          <Card className="p-4 h-fit">
            <h3 className="text-sm font-semibold mb-3">Détail mouvement</h3>
            {!selected ? (
              <p className="text-sm text-gray-400">Sélectionnez un mouvement dans la liste.</p>
            ) : (
              <dl className="space-y-2 text-sm">
                <div><dt className="text-gray-500">ID</dt><dd>{selected.id}</dd></div>
                <div><dt className="text-gray-500">Type</dt><dd>{selected.movementType}</dd></div>
                <div><dt className="text-gray-500">Produit</dt><dd>{selected.productNom}</dd></div>
                <div><dt className="text-gray-500">Entrepôt / Emplacement</dt>
                  <dd>{selected.warehouseCode} / {selected.locationCode}</dd></div>
                <div><dt className="text-gray-500">Quantité</dt>
                  <dd>{selected.quantity} {selected.unitSymbole}</dd></div>
                <div><dt className="text-gray-500">Stock avant / après</dt>
                  <dd>{selected.quantityBefore} → {selected.quantityAfter}</dd></div>
                <div><dt className="text-gray-500">Réservé avant / après</dt>
                  <dd>{selected.quantityReservedBefore} → {selected.quantityReservedAfter}</dd></div>
                <div><dt className="text-gray-500">Origine</dt>
                  <dd>{selected.referenceType || '—'} {selected.referenceId ? `#${selected.referenceId}` : ''}</dd></div>
                <div><dt className="text-gray-500">Référence</dt><dd>{selected.reference || '—'}</dd></div>
                <div><dt className="text-gray-500">Motif</dt><dd>{selected.reason || '—'}</dd></div>
                <div><dt className="text-gray-500">Utilisateur</dt><dd>{selected.createdBy || selected.utilisateur}</dd></div>
                <div><dt className="text-gray-500">Date</dt><dd>{formatDate(selected.movementDate)}</dd></div>
                {selected.stockEntryId && (
                  <div><dt className="text-gray-500">Entrée stock</dt><dd>#{selected.stockEntryId}</dd></div>
                )}
                {selected.stockExitId && (
                  <div><dt className="text-gray-500">Sortie stock</dt><dd>#{selected.stockExitId}</dd></div>
                )}
              </dl>
            )}
          </Card>
        </div>
      )}
    </div>
  )
}
