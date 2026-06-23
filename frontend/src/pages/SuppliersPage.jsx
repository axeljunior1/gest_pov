import { useEffect, useState } from 'react'
import { suppliersApi } from '../api'
import { PageHeader, Card, Button, Loading, Alert } from '../components/ui'
import EntitySearchField from '../components/search/EntitySearchField'
import { filterEntities } from '../utils/entitySearchMatch'
import { useAsyncAction } from '../hooks/useAsyncAction'
import { useNotification } from '../context/NotificationContext'
import { getErrorMessage } from '../utils/errors'

export default function SuppliersPage() {
  const notify = useNotification()
  const { run, submitting } = useAsyncAction()
  const [suppliers, setSuppliers] = useState([])
  const [loading, setLoading] = useState(true)
  const [pageError, setPageError] = useState('')
  const [form, setForm] = useState({ nom: '', email: '', telephone: '', adresse: '' })
  const [editingId, setEditingId] = useState(null)
  const [search, setSearch] = useState('')

  const displayedSuppliers = search.trim()
    ? filterEntities(suppliers, search, 'supplier')
    : suppliers

  const load = async () => {
    setLoading(true)
    setPageError('')
    try {
      const data = await suppliersApi.getAll()
      setSuppliers(data)
    } catch (error) {
      const message = getErrorMessage(error)
      setPageError(message)
      notify.error(message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [])

  const resetForm = () => {
    setForm({ nom: '', email: '', telephone: '', adresse: '' })
    setEditingId(null)
  }

  const handleSubmit = () => {
    if (!form.nom.trim()) {
      notify.error('Le nom du fournisseur est obligatoire.')
      return
    }
    run(
      () => editingId ? suppliersApi.update(editingId, form) : suppliersApi.create(form),
      {
        successMessage: editingId ? 'Fournisseur mis à jour' : 'Fournisseur créé',
        onSuccess: () => { resetForm(); load() },
      },
    )
  }

  const handleEdit = (s) => {
    setEditingId(s.id)
    setForm({ nom: s.nom, email: s.email || '', telephone: s.telephone || '', adresse: s.adresse || '' })
  }

  const handleDelete = (id) => {
    if (!confirm('Supprimer ce fournisseur ?')) return
    run(
      () => suppliersApi.delete(id),
      { successMessage: 'Fournisseur supprimé', onSuccess: load },
    )
  }

  return (
    <>
      <PageHeader title="Fournisseurs" subtitle="Gestion des partenaires" />

      <Alert message={pageError} onDismiss={() => setPageError('')} />

      <Card className="p-5 mb-6">
        <h3 className="text-sm font-medium mb-3">{editingId ? 'Modifier' : 'Nouveau fournisseur'}</h3>
        <div className="grid grid-cols-2 gap-3">
          <input placeholder="Nom *" value={form.nom} onChange={(e) => setForm({ ...form, nom: e.target.value })} />
          <input placeholder="Email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
          <input placeholder="Téléphone" value={form.telephone} onChange={(e) => setForm({ ...form, telephone: e.target.value })} />
          <input placeholder="Adresse" value={form.adresse} onChange={(e) => setForm({ ...form, adresse: e.target.value })} />
        </div>
        <div className="flex gap-2 mt-3">
          <Button onClick={handleSubmit} disabled={submitting}>{editingId ? 'Mettre à jour' : 'Créer'}</Button>
          {editingId && <Button variant="secondary" onClick={resetForm}>Annuler</Button>}
        </div>
      </Card>

      <Card className="p-5 mb-4">
        <EntitySearchField
          entityType="supplier"
          value={search}
          onChange={setSearch}
        />
      </Card>

      {loading ? <Loading /> : pageError ? (
        <Card><p className="text-center py-8 text-sm text-gray-400">Impossible de charger les fournisseurs.</p></Card>
      ) : (
        <Card className="overflow-hidden">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b text-left text-gray-500">
                <th className="px-5 py-3">Nom</th>
                <th className="px-5 py-3">Email</th>
                <th className="px-5 py-3">Téléphone</th>
                <th className="px-5 py-3">Adresse</th>
                <th className="px-5 py-3"></th>
              </tr>
            </thead>
            <tbody>
              {displayedSuppliers.length === 0 ? (
                <tr><td colSpan={5} className="px-5 py-8 text-center text-gray-400">Aucun fournisseur</td></tr>
              ) : displayedSuppliers.map((s) => (
                <tr key={s.id} className="border-b border-gray-50">
                  <td className="px-5 py-3 font-medium">{s.nom}</td>
                  <td className="px-5 py-3 text-gray-600">{s.email || '—'}</td>
                  <td className="px-5 py-3">{s.telephone || '—'}</td>
                  <td className="px-5 py-3 text-gray-600">{s.adresse || '—'}</td>
                  <td className="px-5 py-3">
                    <div className="flex gap-2">
                      <Button variant="ghost" className="text-xs" onClick={() => handleEdit(s)}>Modifier</Button>
                      <Button variant="ghost" className="text-xs text-red-600" onClick={() => handleDelete(s.id)}>Suppr.</Button>
                    </div>
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
