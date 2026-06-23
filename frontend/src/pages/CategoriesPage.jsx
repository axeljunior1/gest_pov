import { useEffect, useState } from 'react'
import { categoriesApi } from '../api'
import { PageHeader, Card, Button, Loading, Alert } from '../components/ui'
import EntitySearchField from '../components/search/EntitySearchField'
import { SearchMatchHint } from '../components/search/SearchCriteriaHelp'
import { findEntityMatch } from '../utils/entitySearchMatch'
import { useAsyncAction } from '../hooks/useAsyncAction'
import { getErrorMessage } from '../utils/errors'
import { useNotification } from '../context/NotificationContext'

function CategoryNode({ category, level = 0, onRefresh }) {
  const { run, submitting } = useAsyncAction()
  const [expanded, setExpanded] = useState(true)
  const [editing, setEditing] = useState(false)
  const [nom, setNom] = useState(category.nom)
  const [addingChild, setAddingChild] = useState(false)
  const [childNom, setChildNom] = useState('')

  const handleUpdate = () => run(
    () => categoriesApi.update(category.id, { nom, parentId: category.parentId }),
    {
      successMessage: 'Catégorie mise à jour',
      onSuccess: () => { setEditing(false); onRefresh() },
    },
  )

  const handleDelete = () => {
    if (!confirm('Supprimer cette catégorie ?')) return
    run(
      () => categoriesApi.delete(category.id),
      { successMessage: 'Catégorie supprimée', onSuccess: onRefresh },
    )
  }

  const handleAddChild = () => {
    if (!childNom.trim()) return
    run(
      () => categoriesApi.create({ nom: childNom, parentId: category.id }),
      {
        successMessage: 'Sous-catégorie créée',
        onSuccess: () => { setChildNom(''); setAddingChild(false); onRefresh() },
      },
    )
  }

  return (
    <div style={{ marginLeft: level * 20 }}>
      <div className="flex items-center gap-2 py-1.5 group">
        {category.children?.length > 0 && (
          <button onClick={() => setExpanded(!expanded)} className="text-gray-400 w-4 text-xs">
            {expanded ? '▼' : '▶'}
          </button>
        )}
        {editing ? (
          <>
            <input value={nom} onChange={(e) => setNom(e.target.value)} className="text-sm" />
            <Button variant="secondary" className="text-xs px-2 py-1" onClick={handleUpdate} disabled={submitting}>OK</Button>
          </>
        ) : (
          <>
            <span className="text-sm font-medium">{category.nom}</span>
            <div className="opacity-0 group-hover:opacity-100 flex gap-1">
              <button onClick={() => setEditing(true)} className="text-xs text-gray-400 hover:text-gray-700">Modifier</button>
              <button onClick={() => setAddingChild(true)} className="text-xs text-gray-400 hover:text-gray-700">+ Sous-cat.</button>
              <button onClick={handleDelete} className="text-xs text-red-400 hover:text-red-600">Suppr.</button>
            </div>
          </>
        )}
      </div>
      {addingChild && (
        <div className="flex gap-2 ml-6 mb-2">
          <input placeholder="Nom sous-catégorie" value={childNom} onChange={(e) => setChildNom(e.target.value)} className="text-sm" />
          <Button className="text-xs px-2 py-1" onClick={handleAddChild} disabled={submitting}>Ajouter</Button>
        </div>
      )}
      {expanded && category.children?.map((child) => (
        <CategoryNode key={child.id} category={child} level={level + 1} onRefresh={onRefresh} />
      ))}
    </div>
  )
}

export default function CategoriesPage() {
  const notify = useNotification()
  const { run, submitting } = useAsyncAction()
  const [categories, setCategories] = useState([])
  const [loading, setLoading] = useState(true)
  const [pageError, setPageError] = useState('')
  const [newNom, setNewNom] = useState('')
  const [search, setSearch] = useState('')
  const [searchResults, setSearchResults] = useState(null)

  const load = async () => {
    setLoading(true)
    setPageError('')
    try {
      const data = await categoriesApi.getTree()
      setCategories(data)
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
      notify.error('Le nom de la catégorie est obligatoire.')
      return
    }
    run(
      () => categoriesApi.create({ nom: newNom }),
      {
        successMessage: 'Catégorie créée',
        onSuccess: () => { setNewNom(''); load() },
      },
    )
  }

  const handleSearch = async () => {
    if (!search.trim()) { setSearchResults(null); return }
    try {
      const results = await categoriesApi.search(search)
      setSearchResults(results)
    } catch (error) {
      notify.error(getErrorMessage(error))
    }
  }

  return (
    <>
      <PageHeader title="Catégories" subtitle="Organisation hiérarchique du catalogue" />

      <Alert message={pageError} onDismiss={() => setPageError('')} />

      <Card className="p-5 mb-6">
        <div className="flex gap-3">
          <input placeholder="Nouvelle catégorie racine" value={newNom} onChange={(e) => setNewNom(e.target.value)} className="flex-1" onKeyDown={(e) => e.key === 'Enter' && handleCreate()} />
          <Button onClick={handleCreate} disabled={submitting}>Créer</Button>
        </div>
        <EntitySearchField
          entityType="category"
          value={search}
          onChange={setSearch}
          onSubmit={handleSearch}
          showSearchButton
          className="mt-3"
        />
      </Card>

      {loading ? <Loading /> : pageError ? (
        <Card><p className="text-center py-8 text-sm text-gray-400">Impossible de charger les catégories.</p></Card>
      ) : (
        <Card className="p-5">
          {searchResults ? (
            <div>
              <p className="text-xs text-gray-400 mb-3">Résultats de recherche</p>
              {searchResults.length === 0 ? (
                <p className="text-sm text-gray-400">Aucun résultat</p>
              ) : searchResults.map((c) => (
                <div key={c.id} className="py-1 text-sm">
                  {c.nom} {c.parentNom && <span className="text-gray-400">({c.parentNom})</span>}
                  {search.trim() && <SearchMatchHint match={findEntityMatch(search, c, 'category')} />}
                </div>
              ))}
              <Button variant="ghost" className="mt-3 text-xs" onClick={() => setSearchResults(null)}>Retour à l'arborescence</Button>
            </div>
          ) : categories.length === 0 ? (
            <p className="text-sm text-gray-400 text-center py-8">Aucune catégorie</p>
          ) : (
            categories.map((c) => <CategoryNode key={c.id} category={c} onRefresh={load} />)
          )}
        </Card>
      )}
    </>
  )
}
