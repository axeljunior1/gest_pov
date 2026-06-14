import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { brandsApi } from '../api'
import { PageHeader, Card, Button, Loading, Alert } from '../components/ui'
import { useAsyncAction } from '../hooks/useAsyncAction'
import { getErrorMessage } from '../utils/errors'
import { useNotification } from '../context/NotificationContext'

function BrandRow({ brand, onRefresh }) {
  const { run, submitting } = useAsyncAction()
  const [editing, setEditing] = useState(false)
  const [nom, setNom] = useState(brand.nom)

  const handleUpdate = () => run(
    () => brandsApi.update(brand.id, { nom }),
    {
      successMessage: 'Marque mise à jour',
      onSuccess: () => { setEditing(false); onRefresh() },
    },
  )

  const handleDelete = () => {
    if (!confirm('Supprimer cette marque ?')) return
    run(
      () => brandsApi.delete(brand.id),
      { successMessage: 'Marque supprimée', onSuccess: onRefresh },
    )
  }

  return (
    <div className="flex items-center gap-2 py-1.5 group border-b border-gray-50 last:border-0">
      {editing ? (
        <>
          <input value={nom} onChange={(e) => setNom(e.target.value)} className="text-sm flex-1" />
          <Button variant="secondary" className="text-xs px-2 py-1" onClick={handleUpdate} disabled={submitting}>OK</Button>
          <Button variant="ghost" className="text-xs px-2 py-1" onClick={() => { setEditing(false); setNom(brand.nom) }}>Annuler</Button>
        </>
      ) : (
        <>
          <span className="text-sm font-medium flex-1">{brand.nom}</span>
          <div className="opacity-0 group-hover:opacity-100 flex gap-1">
            <button type="button" onClick={() => setEditing(true)} className="text-xs text-gray-400 hover:text-gray-700">Modifier</button>
            <button type="button" onClick={handleDelete} className="text-xs text-red-400 hover:text-red-600">Suppr.</button>
          </div>
        </>
      )}
    </div>
  )
}

export default function BrandsPage() {
  const notify = useNotification()
  const { run, submitting } = useAsyncAction()
  const [brands, setBrands] = useState([])
  const [loading, setLoading] = useState(true)
  const [pageError, setPageError] = useState('')
  const [newNom, setNewNom] = useState('')
  const [search, setSearch] = useState('')
  const [searchResults, setSearchResults] = useState(null)

  const load = async () => {
    setLoading(true)
    setPageError('')
    try {
      setBrands(await brandsApi.getAll())
    } catch (error) {
      const message = getErrorMessage(error)
      setPageError(message)
      notify.error(message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [])

  const handleCreate = () => {
    if (!newNom.trim()) {
      notify.error('Le nom de la marque est obligatoire.')
      return
    }
    run(
      () => brandsApi.create({ nom: newNom }),
      {
        successMessage: 'Marque créée',
        onSuccess: () => { setNewNom(''); load() },
      },
    )
  }

  const handleSearch = async () => {
    if (!search.trim()) { setSearchResults(null); return }
    try {
      setSearchResults(await brandsApi.search(search))
    } catch (error) {
      notify.error(getErrorMessage(error))
    }
  }

  return (
    <>
      <PageHeader
        title="Marques"
        subtitle="Liste des marques produits — sélectionnables sur chaque fiche"
      />

      <Alert message={pageError} onDismiss={() => setPageError('')} />

      <Card className="p-5 mb-6">
        <div className="flex gap-3">
          <input
            placeholder="Nouvelle marque"
            value={newNom}
            onChange={(e) => setNewNom(e.target.value)}
            className="flex-1"
            onKeyDown={(e) => e.key === 'Enter' && handleCreate()}
          />
          <Button onClick={handleCreate} disabled={submitting}>Créer</Button>
        </div>
        <div className="flex gap-3 mt-3">
          <input
            placeholder="Rechercher..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="flex-1"
            onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
          />
          <Button variant="secondary" onClick={handleSearch}>Rechercher</Button>
        </div>
        <p className="text-xs text-gray-400 mt-3">
          Les marques existantes sur les produits ont été importées automatiquement.{' '}
          <Link to="/products/new" className="text-emerald-600 hover:underline">Créer un produit</Link>
        </p>
      </Card>

      {loading ? <Loading /> : pageError ? (
        <Card><p className="text-center py-8 text-sm text-gray-400">Impossible de charger les marques.</p></Card>
      ) : (
        <Card className="p-5">
          {searchResults ? (
            <div>
              <p className="text-xs text-gray-400 mb-3">Résultats de recherche</p>
              {searchResults.length === 0 ? (
                <p className="text-sm text-gray-400">Aucun résultat</p>
              ) : searchResults.map((b) => (
                <div key={b.id} className="py-1 text-sm">{b.nom}</div>
              ))}
              <Button variant="ghost" className="mt-3 text-xs" onClick={() => setSearchResults(null)}>Retour à la liste</Button>
            </div>
          ) : brands.length === 0 ? (
            <p className="text-sm text-gray-400 text-center py-8">Aucune marque — créez-en une ci-dessus.</p>
          ) : (
            brands.map((b) => <BrandRow key={b.id} brand={b} onRefresh={load} />)
          )}
        </Card>
      )}
    </>
  )
}
