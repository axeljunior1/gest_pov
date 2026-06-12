import { useEffect, useMemo, useState } from 'react'
import { rolesApi } from '../api'
import { useAuth } from '../context/AuthContext'
import { PageHeader, Card, Button, Loading, Alert, Badge } from '../components/ui'
import { useAsyncAction } from '../hooks/useAsyncAction'
import { useNotification } from '../context/NotificationContext'
import { getErrorMessage } from '../utils/errors'

const MODULE_LABELS = {
  MODULE_PRODUCTS: 'Produits',
  MODULE_STOCK: 'Stock',
  MODULE_ALERTS: 'Alertes',
  MODULE_USERS: 'Utilisateurs',
  MODULE_ROLES: 'Rôles & permissions',
}

export default function RolesPage() {
  const notify = useNotification()
  const { hasPermission, user, refreshUser } = useAuth()
  const { run, submitting } = useAsyncAction()
  const [roles, setRoles] = useState([])
  const [permissions, setPermissions] = useState([])
  const [selectedRoleId, setSelectedRoleId] = useState(null)
  const [selectedCodes, setSelectedCodes] = useState([])
  const [loading, setLoading] = useState(true)
  const [pageError, setPageError] = useState('')

  const canUpdate = hasPermission('roles.update')

  const selectedRole = roles.find((role) => role.id === selectedRoleId) ?? null
  const isSuperAdmin = selectedRole?.code === 'SUPER_ADMIN'

  const permissionsByModule = useMemo(() => {
    const groups = {}
    for (const permission of permissions) {
      const moduleKey = permission.module || 'AUTRE'
      if (!groups[moduleKey]) groups[moduleKey] = []
      groups[moduleKey].push(permission)
    }
    return groups
  }, [permissions])

  const load = async () => {
    setLoading(true)
    setPageError('')
    try {
      const [rolesData, permissionsData] = await Promise.all([
        rolesApi.list(),
        rolesApi.listPermissions(),
      ])
      setRoles(rolesData)
      setPermissions(permissionsData)
      if (!selectedRoleId && rolesData.length > 0) {
        selectRole(rolesData[0])
      } else if (selectedRoleId) {
        const role = rolesData.find((item) => item.id === selectedRoleId)
        if (role) selectRole(role)
      }
    } catch (error) {
      const message = getErrorMessage(error)
      setPageError(message)
      notify.error(message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, []) // eslint-disable-line react-hooks/exhaustive-deps

  const selectRole = (role) => {
    setSelectedRoleId(role.id)
    setSelectedCodes([...(role.permissions ?? [])])
  }

  const togglePermission = (code) => {
    setSelectedCodes((prev) =>
      prev.includes(code) ? prev.filter((item) => item !== code) : [...prev, code],
    )
  }

  const toggleModule = (moduleKey, checked) => {
    const moduleCodes = (permissionsByModule[moduleKey] ?? []).map((item) => item.code)
    setSelectedCodes((prev) => {
      const withoutModule = prev.filter((code) => !moduleCodes.includes(code))
      return checked ? [...withoutModule, ...moduleCodes] : withoutModule
    })
  }

  const isModuleFullySelected = (moduleKey) => {
    const moduleCodes = (permissionsByModule[moduleKey] ?? []).map((item) => item.code)
    return moduleCodes.length > 0 && moduleCodes.every((code) => selectedCodes.includes(code))
  }

  const handleSave = () => {
    if (!selectedRole) return
    run(
      () => rolesApi.updatePermissions(selectedRole.id, selectedCodes),
      {
        successMessage: 'Permissions mises à jour',
        onSuccess: async () => {
          const roleCode = selectedRole?.code
          await load()
          if (roleCode && user?.roles?.includes(roleCode)) {
            await refreshUser()
          }
        },
      },
    )
  }

  const hasChanges = selectedRole
    ? JSON.stringify([...(selectedRole.permissions ?? [])].sort())
      !== JSON.stringify([...selectedCodes].sort())
    : false

  return (
    <>
      <PageHeader
        title="Rôles & permissions"
        subtitle="Définir les droits d’accès par rôle"
      />

      <Alert message={pageError} onDismiss={() => setPageError('')} />

      {loading ? <Loading /> : (
        <div className="grid grid-cols-12 gap-6">
          <Card className="col-span-4 p-4">
            <h3 className="text-sm font-medium mb-3">Rôles</h3>
            <div className="space-y-1">
              {roles.map((role) => (
                <button
                  key={role.id}
                  type="button"
                  onClick={() => selectRole(role)}
                  className={`w-full text-left px-3 py-2.5 rounded-lg text-sm transition-colors ${
                    selectedRoleId === role.id
                      ? 'bg-gray-900 text-white'
                      : 'text-gray-700 hover:bg-gray-50'
                  }`}
                >
                  <div className="font-medium">{role.name}</div>
                  <div className={`text-xs mt-0.5 ${selectedRoleId === role.id ? 'text-gray-300' : 'text-gray-400'}`}>
                    {role.code}
                  </div>
                </button>
              ))}
            </div>
          </Card>

          <Card className="col-span-8 p-5">
            {!selectedRole ? (
              <p className="text-sm text-gray-400">Sélectionnez un rôle.</p>
            ) : (
              <>
                <div className="flex items-start justify-between gap-4 mb-5">
                  <div>
                    <h3 className="text-lg font-semibold">{selectedRole.name}</h3>
                    <p className="text-sm text-gray-500 mt-1">{selectedRole.description || '—'}</p>
                    <div className="flex gap-2 mt-2">
                      <Badge>{selectedRole.code}</Badge>
                      {selectedRole.isSystem && <Badge tone="info">Système</Badge>}
                      <Badge tone="default">{selectedCodes.length} permission(s)</Badge>
                    </div>
                  </div>
                  {canUpdate && !isSuperAdmin && (
                    <Button
                      onClick={handleSave}
                      disabled={submitting || !hasChanges}
                    >
                      Enregistrer
                    </Button>
                  )}
                </div>

                {isSuperAdmin && (
                  <Alert
                    type="info"
                    message="Le rôle SUPER_ADMIN possède toutes les permissions et ne peut pas être modifié."
                  />
                )}

                <div className="space-y-5">
                  {Object.entries(permissionsByModule).map(([moduleKey, modulePermissions]) => (
                    <div key={moduleKey} className="border border-gray-100 rounded-xl p-4">
                      <div className="flex items-center justify-between mb-3">
                        <h4 className="text-sm font-medium text-gray-900">
                          {MODULE_LABELS[moduleKey] ?? moduleKey}
                        </h4>
                        {canUpdate && !isSuperAdmin && (
                          <label className="flex items-center gap-2 text-xs text-gray-500">
                            <input
                              type="checkbox"
                              checked={isModuleFullySelected(moduleKey)}
                              onChange={(e) => toggleModule(moduleKey, e.target.checked)}
                            />
                            Tout sélectionner
                          </label>
                        )}
                      </div>
                      <div className="grid grid-cols-2 gap-2">
                        {modulePermissions.map((permission) => (
                          <label
                            key={permission.code}
                            className={`flex items-start gap-2 rounded-lg border px-3 py-2 text-sm ${
                              selectedCodes.includes(permission.code)
                                ? 'border-gray-900 bg-gray-50'
                                : 'border-gray-100'
                            }`}
                          >
                            <input
                              type="checkbox"
                              className="mt-0.5"
                              checked={selectedCodes.includes(permission.code)}
                              disabled={!canUpdate || isSuperAdmin}
                              onChange={() => togglePermission(permission.code)}
                            />
                            <span>
                              <span className="font-medium text-gray-900">{permission.name}</span>
                              <span className="block text-xs text-gray-400">{permission.code}</span>
                            </span>
                          </label>
                        ))}
                      </div>
                    </div>
                  ))}
                </div>
              </>
            )}
          </Card>
        </div>
      )}
    </>
  )
}
