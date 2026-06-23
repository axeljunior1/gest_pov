import { useEffect, useState } from 'react'
import { rolesApi, usersApi } from '../api'
import { useAuth } from '../context/AuthContext'
import { PageHeader, Card, Button, Loading, Alert, Badge } from '../components/ui'
import EntitySearchField from '../components/search/EntitySearchField'
import { filterEntities } from '../utils/entitySearchMatch'
import { useAsyncAction } from '../hooks/useAsyncAction'
import { useNotification } from '../context/NotificationContext'
import { getErrorMessage } from '../utils/errors'

const emptyForm = () => ({
  firstName: '',
  lastName: '',
  email: '',
  password: '',
  isActive: true,
  roleIds: [],
})

function formatDate(value) {
  if (!value) return '—'
  return new Date(value).toLocaleString('fr-FR')
}

export default function UsersPage() {
  const notify = useNotification()
  const { hasPermission, user, refreshUser } = useAuth()
  const { run, submitting } = useAsyncAction()
  const [users, setUsers] = useState([])
  const [roles, setRoles] = useState([])
  const [loading, setLoading] = useState(true)
  const [pageError, setPageError] = useState('')
  const [form, setForm] = useState(emptyForm())
  const [editingId, setEditingId] = useState(null)
  const [search, setSearch] = useState('')

  const displayedUsers = search.trim() ? filterEntities(users, search, 'user') : users

  const canCreate = hasPermission('users.create')
  const canUpdate = hasPermission('users.update')
  const canDelete = hasPermission('users.delete')
  const canEditRoles = hasPermission('roles.read')

  const load = async () => {
    setLoading(true)
    setPageError('')
    try {
      const requests = [usersApi.list()]
      if (canEditRoles) requests.push(rolesApi.list())
      const results = await Promise.all(requests)
      setUsers(results[0])
      if (results[1]) setRoles(results[1])
    } catch (error) {
      const message = getErrorMessage(error)
      setPageError(message)
      notify.error(message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, []) // eslint-disable-line react-hooks/exhaustive-deps

  const resetForm = () => {
    setForm(emptyForm())
    setEditingId(null)
  }

  const toggleRole = (roleId) => {
    setForm((prev) => ({
      ...prev,
      roleIds: prev.roleIds.includes(roleId)
        ? prev.roleIds.filter((id) => id !== roleId)
        : [...prev.roleIds, roleId],
    }))
  }

  const handleEdit = (user) => {
    const roleIds = roles
      .filter((role) => user.roles?.includes(role.code))
      .map((role) => role.id)
    setEditingId(user.id)
    setForm({
      firstName: user.firstName,
      lastName: user.lastName,
      email: user.email,
      password: '',
      isActive: user.isActive !== false,
      roleIds,
    })
  }

  const handleSubmit = () => {
    if (!form.firstName.trim() || !form.lastName.trim() || !form.email.trim()) {
      notify.error('Prénom, nom et email sont obligatoires.')
      return
    }
    if (!editingId && !form.password.trim()) {
      notify.error('Mot de passe obligatoire à la création.')
      return
    }
    if (form.roleIds.length === 0) {
      notify.error('Sélectionnez au moins un rôle.')
      return
    }

    const payload = {
      firstName: form.firstName.trim(),
      lastName: form.lastName.trim(),
      email: form.email.trim(),
      isActive: form.isActive,
      roleIds: form.roleIds,
    }
    if (form.password.trim()) payload.password = form.password

    run(
      () => (editingId ? usersApi.update(editingId, payload) : usersApi.create(payload)),
      {
        successMessage: editingId ? 'Utilisateur mis à jour' : 'Utilisateur créé',
        onSuccess: async () => {
          const updatedSelf = editingId && user?.id === editingId
          resetForm()
          await load()
          if (updatedSelf) {
            await refreshUser()
          }
        },
      },
    )
  }

  const handleDelete = (user) => {
    if (!confirm(`Supprimer l'utilisateur ${user.email} ?`)) return
    run(
      () => usersApi.delete(user.id),
      { successMessage: 'Utilisateur supprimé', onSuccess: load },
    )
  }

  const roleLabel = (code) => roles.find((r) => r.code === code)?.name ?? code

  return (
    <>
      <PageHeader
        title="Utilisateurs"
        subtitle="Comptes, accès et rôles assignés"
      />

      <Alert message={pageError} onDismiss={() => setPageError('')} />

      {(canCreate || (canUpdate && editingId)) && (
        <Card className="p-5 mb-6">
          <h3 className="text-sm font-medium mb-3">
            {editingId ? 'Modifier l’utilisateur' : 'Nouvel utilisateur'}
          </h3>
          <div className="grid grid-cols-2 gap-3">
            <input
              placeholder="Prénom *"
              value={form.firstName}
              onChange={(e) => setForm({ ...form, firstName: e.target.value })}
            />
            <input
              placeholder="Nom *"
              value={form.lastName}
              onChange={(e) => setForm({ ...form, lastName: e.target.value })}
            />
            <input
              type="email"
              placeholder="Email *"
              value={form.email}
              onChange={(e) => setForm({ ...form, email: e.target.value })}
            />
            <input
              type="password"
              placeholder={editingId ? 'Nouveau mot de passe (optionnel)' : 'Mot de passe *'}
              value={form.password}
              onChange={(e) => setForm({ ...form, password: e.target.value })}
            />
          </div>

          <label className="flex items-center gap-2 mt-3 text-sm text-gray-700">
            <input
              type="checkbox"
              checked={form.isActive}
              onChange={(e) => setForm({ ...form, isActive: e.target.checked })}
            />
            Compte actif
          </label>

          {canEditRoles ? (
            <div className="mt-4">
              <p className="text-sm font-medium text-gray-700 mb-2">Rôles *</p>
              <div className="grid grid-cols-2 gap-2">
                {roles.map((role) => (
                  <label key={role.id} className="flex items-start gap-2 text-sm text-gray-600">
                    <input
                      type="checkbox"
                      className="mt-0.5"
                      checked={form.roleIds.includes(role.id)}
                      onChange={() => toggleRole(role.id)}
                    />
                    <span>
                      <span className="font-medium text-gray-900">{role.name}</span>
                      <span className="block text-xs text-gray-400">{role.code}</span>
                    </span>
                  </label>
                ))}
              </div>
            </div>
          ) : (
            <p className="mt-3 text-sm text-amber-700">
              Droits insuffisants pour charger la liste des rôles.
            </p>
          )}

          <div className="flex gap-2 mt-4">
            <Button onClick={handleSubmit} disabled={submitting || !canEditRoles}>
              {editingId ? 'Mettre à jour' : 'Créer'}
            </Button>
            {editingId && (
              <Button variant="secondary" onClick={resetForm}>Annuler</Button>
            )}
          </div>
        </Card>
      )}

      <Card className="p-5 mb-4">
        <EntitySearchField entityType="user" value={search} onChange={setSearch} />
      </Card>

      {loading ? <Loading /> : (
        <Card className="overflow-hidden">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b text-left text-gray-500">
                <th className="px-5 py-3">Utilisateur</th>
                <th className="px-5 py-3">Email</th>
                <th className="px-5 py-3">Rôles</th>
                <th className="px-5 py-3">Statut</th>
                <th className="px-5 py-3">Dernière connexion</th>
                <th className="px-5 py-3"></th>
              </tr>
            </thead>
            <tbody>
              {displayedUsers.length === 0 ? (
                <tr>
                  <td colSpan={6} className="px-5 py-8 text-center text-gray-400">
                    Aucun utilisateur
                  </td>
                </tr>
              ) : displayedUsers.map((user) => (
                <tr key={user.id} className="border-b border-gray-50">
                  <td className="px-5 py-3 font-medium">
                    {user.firstName} {user.lastName}
                  </td>
                  <td className="px-5 py-3 text-gray-600">{user.email}</td>
                  <td className="px-5 py-3">
                    <div className="flex flex-wrap gap-1">
                      {(user.roles ?? []).map((code) => (
                        <Badge key={code}>{roleLabel(code)}</Badge>
                      ))}
                    </div>
                  </td>
                  <td className="px-5 py-3">
                    <Badge tone={user.isActive !== false ? 'success' : 'default'}>
                      {user.isActive !== false ? 'Actif' : 'Inactif'}
                    </Badge>
                  </td>
                  <td className="px-5 py-3 text-gray-600">{formatDate(user.lastLoginAt)}</td>
                  <td className="px-5 py-3">
                    <div className="flex gap-2">
                      {canUpdate && (
                        <Button variant="ghost" className="text-xs" onClick={() => handleEdit(user)}>
                          Modifier
                        </Button>
                      )}
                      {canDelete && user.email !== 'admin@erp.local' && (
                        <Button
                          variant="ghost"
                          className="text-xs text-red-600"
                          onClick={() => handleDelete(user)}
                        >
                          Suppr.
                        </Button>
                      )}
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
