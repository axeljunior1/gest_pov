import { useEffect, useState } from 'react'
import { attributesApi } from '../api'
import { PageHeader, Card, Button, Loading, Alert } from '../components/ui'
import { useAsyncAction } from '../hooks/useAsyncAction'
import { useNotification } from '../context/NotificationContext'
import { getErrorMessage } from '../utils/errors'

export default function AttributesPage() {
  const notify = useNotification()
  const { run, submitting } = useAsyncAction()
  const [attributes, setAttributes] = useState([])
  const [loading, setLoading] = useState(true)
  const [pageError, setPageError] = useState('')
  const [form, setForm] = useState({ code: '', label: '', type: 'text' })

  const load = async () => {
    setLoading(true)
    setPageError('')
    try {
      const data = await attributesApi.getAll()
      setAttributes(data)
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
    if (!form.code.trim() || !form.label.trim()) {
      notify.error('Le code et le label sont obligatoires.')
      return
    }
    run(
      () => attributesApi.create(form),
      {
        successMessage: 'Attribut créé',
        onSuccess: () => { setForm({ code: '', label: '', type: 'text' }); load() },
      },
    )
  }

  const handleDelete = (id) => {
    if (!confirm('Supprimer cet attribut ?')) return
    run(
      () => attributesApi.delete(id),
      { successMessage: 'Attribut supprimé', onSuccess: load },
    )
  }

  return (
    <>
      <PageHeader
        title="Attributs personnalisés"
        subtitle="Champs dynamiques par secteur (auto, pharma, IT...)"
      />

      <Alert message={pageError} onDismiss={() => setPageError('')} />

      <Card className="p-5 mb-6">
        <div className="grid grid-cols-3 gap-3">
          <input placeholder="Code (ex: ram)" value={form.code} onChange={(e) => setForm({ ...form, code: e.target.value })} />
          <input placeholder="Label (ex: Mémoire RAM)" value={form.label} onChange={(e) => setForm({ ...form, label: e.target.value })} />
          <select value={form.type} onChange={(e) => setForm({ ...form, type: e.target.value })}>
            <option value="text">Texte</option>
            <option value="number">Nombre</option>
            <option value="date">Date</option>
            <option value="boolean">Booléen</option>
          </select>
        </div>
        <Button className="mt-3" onClick={handleCreate} disabled={submitting}>Créer attribut</Button>
      </Card>

      {loading ? <Loading /> : pageError ? (
        <Card><p className="text-center py-8 text-sm text-gray-400">Impossible de charger les attributs.</p></Card>
      ) : (
        <Card className="overflow-hidden">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b text-left text-gray-500">
                <th className="px-5 py-3">Code</th>
                <th className="px-5 py-3">Label</th>
                <th className="px-5 py-3">Type</th>
                <th className="px-5 py-3"></th>
              </tr>
            </thead>
            <tbody>
              {attributes.length === 0 ? (
                <tr><td colSpan={4} className="px-5 py-8 text-center text-gray-400">Aucun attribut</td></tr>
              ) : attributes.map((a) => (
                <tr key={a.id} className="border-b border-gray-50">
                  <td className="px-5 py-3 font-mono text-xs">{a.code}</td>
                  <td className="px-5 py-3">{a.label}</td>
                  <td className="px-5 py-3 text-gray-500">{a.type}</td>
                  <td className="px-5 py-3">
                    <Button variant="ghost" className="text-xs text-red-600" onClick={() => handleDelete(a.id)}>Suppr.</Button>
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
