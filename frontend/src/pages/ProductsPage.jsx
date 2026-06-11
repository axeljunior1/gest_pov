import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { productsApi, categoriesApi, suppliersApi } from '../api'
import { PageHeader, Card, Button, Badge, EmptyState, Loading, Alert } from '../components/ui'
import { useAsyncAction } from '../hooks/useAsyncAction'
import { useNotification } from '../context/NotificationContext'
import { getErrorMessage } from '../utils/errors'
import { formatPrice, lifecycleLabel, statusLabel } from '../utils/constants'

export default function ProductsPage() {
  const navigate = useNavigate()
  const notify = useNotification()
  const { run, submitting } = useAsyncAction()
  const [products, setProducts] = useState([])
  const [categories, setCategories] = useState([])
  const [suppliers, setSuppliers] = useState([])
  const [loading, setLoading] = useState(true)
  const [pageError, setPageError] = useState('')
  const [filters, setFilters] = useState({
    query: '',
    categorieId: '',
    fournisseurId: '',
    marque: '',
    stockFaible: false,
    rupture: false,
  })

  const load = async () => {
    setLoading(true)
    setPageError('')
    try {
      const params = {}
      if (filters.query?.trim()) params.query = filters.query.trim()
      if (filters.categorieId) params.categorieId = Number(filters.categorieId)
      if (filters.fournisseurId) params.fournisseurId = Number(filters.fournisseurId)
      if (filters.marque?.trim()) params.marque = filters.marque.trim()
      if (filters.stockFaible) params.stockFaible = true
      if (filters.rupture) params.rupture = true
      const data = await productsApi.search(params)
      setProducts(data)
    } catch (error) {
      const message = getErrorMessage(error)
      setPageError(message)
      notify.error(message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    Promise.all([categoriesApi.getTree(), suppliersApi.getAll()])
      .then(([cats, sups]) => {
        setCategories(cats)
        setSuppliers(sups)
      })
      .catch((error) => notify.error(getErrorMessage(error)))
  }, [notify])

  useEffect(() => {
    load()
  }, [filters.stockFaible, filters.rupture])

  const flatCategories = (cats, prefix = '') =>
    cats.flatMap((c) => [
      { id: c.id, label: prefix + c.nom },
      ...(c.children ? flatCategories(c.children, prefix + c.nom + ' > ') : []),
    ])

  const categoryOptions = flatCategories(categories)

  const handleDelete = (e, product) => {
    e.stopPropagation()
    if (!confirm(`Supprimer « ${product.nom} » ? Cette action est irréversible.`)) return
    run(
      () => productsApi.delete(product.id),
      { successMessage: 'Produit supprimé', onSuccess: load },
    )
  }

  return (
    <>
      <PageHeader
        title="Produits"
        subtitle="Catalogue et fiches produits"
        action={
          <Button onClick={() => navigate('/products/new')}>Nouveau produit</Button>
        }
      />

      <Alert message={pageError} onDismiss={() => setPageError('')} />

      <Card className="p-4 mb-6">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-3">
          <input
            placeholder="Rechercher nom, SKU, marque..."
            value={filters.query}
            onChange={(e) => setFilters({ ...filters, query: e.target.value })}
            onKeyDown={(e) => e.key === 'Enter' && load()}
          />
          <select
            value={filters.categorieId}
            onChange={(e) => setFilters({ ...filters, categorieId: e.target.value })}
          >
            <option value="">Toutes catégories</option>
            {categoryOptions.map((c) => (
              <option key={c.id} value={c.id}>{c.label}</option>
            ))}
          </select>
          <select
            value={filters.fournisseurId}
            onChange={(e) => setFilters({ ...filters, fournisseurId: e.target.value })}
          >
            <option value="">Tous fournisseurs</option>
            {suppliers.map((s) => (
              <option key={s.id} value={s.id}>{s.nom}</option>
            ))}
          </select>
          <input
            placeholder="Marque"
            value={filters.marque}
            onChange={(e) => setFilters({ ...filters, marque: e.target.value })}
          />
        </div>
        <div className="flex items-center gap-4 mt-3">
          <label className="flex items-center gap-2 text-sm text-gray-600">
            <input
              type="checkbox"
              checked={filters.stockFaible}
              onChange={(e) => setFilters({ ...filters, stockFaible: e.target.checked })}
            />
            Stock faible
          </label>
          <label className="flex items-center gap-2 text-sm text-gray-600">
            <input
              type="checkbox"
              checked={filters.rupture}
              onChange={(e) => setFilters({ ...filters, rupture: e.target.checked })}
            />
            Rupture
          </label>
          <Button variant="secondary" onClick={load}>Filtrer</Button>
        </div>
      </Card>

      {loading ? (
        <Loading />
      ) : pageError ? (
        <Card><EmptyState message="Chargement impossible. Vérifiez la connexion au serveur." /></Card>
      ) : products.length === 0 ? (
        <Card><EmptyState message="Aucun produit trouvé" /></Card>
      ) : (
        <Card className="overflow-hidden">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-gray-100 text-left text-gray-500">
                <th className="px-5 py-3 font-medium">Produit</th>
                <th className="px-5 py-3 font-medium">SKU</th>
                <th className="px-5 py-3 font-medium">Catégorie</th>
                <th className="px-5 py-3 font-medium">Prix vente</th>
                <th className="px-5 py-3 font-medium">Stock</th>
                <th className="px-5 py-3 font-medium">Statut</th>
                <th className="px-5 py-3 font-medium">Cycle de vie</th>
                <th className="px-5 py-3 font-medium w-24"></th>
              </tr>
            </thead>
            <tbody>
              {products.map((p) => (
                <tr
                  key={p.id}
                  className="border-b border-gray-50 hover:bg-gray-50/50 cursor-pointer"
                  onClick={() => navigate(`/products/${p.id}`)}
                >
                  <td className="px-5 py-3.5">
                    <div className="font-medium text-gray-900">{p.nom}</div>
                    {p.marque && <div className="text-xs text-gray-400">{p.marque}</div>}
                  </td>
                  <td className="px-5 py-3.5 font-mono text-xs">{p.sku}</td>
                  <td className="px-5 py-3.5 text-gray-600">{p.categorieNom || '—'}</td>
                  <td className="px-5 py-3.5">{formatPrice(p.prixVente)}</td>
                  <td className="px-5 py-3.5">
                    <span className={p.stockTotal === 0 ? 'text-red-600 font-medium' : ''}>
                      {p.stockTotal ?? 0}{p.baseUnitSymbole ? ` ${p.baseUnitSymbole}` : ''}
                    </span>
                  </td>
                  <td className="px-5 py-3.5">
                    <Badge tone={p.statut === 'ACTIF' ? 'success' : 'default'}>
                      {statusLabel[p.statut] || p.statut}
                    </Badge>
                  </td>
                  <td className="px-5 py-3.5">
                    <Badge tone={p.cycleVie === 'ACTIF' ? 'info' : 'default'}>
                      {lifecycleLabel[p.cycleVie] || p.cycleVie}
                    </Badge>
                  </td>
                  <td className="px-5 py-3.5">
                    <Button
                      variant="ghost"
                      className="text-xs text-red-600"
                      disabled={submitting}
                      onClick={(e) => handleDelete(e, p)}
                    >
                      Suppr.
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </Card>
      )}
    </>
  )
}
