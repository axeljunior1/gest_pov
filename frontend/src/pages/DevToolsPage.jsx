import { useEffect, useState } from 'react'
import { adminDevApi } from '../api'
import { PageHeader, Card, Button, Loading, Badge } from '../components/ui'
import { useNotification } from '../context/NotificationContext'
import { getErrorMessage } from '../utils/errors'

const TOKEN_STORAGE_KEY = 'erp_dev_reset_token'

export default function DevToolsPage() {
  const notify = useNotification()
  const [loading, setLoading] = useState(true)
  const [available, setAvailable] = useState(false)
  const [resetEnabled, setResetEnabled] = useState(false)
  const [resetToken, setResetToken] = useState(() => sessionStorage.getItem(TOKEN_STORAGE_KEY) ?? '')
  const [busy, setBusy] = useState(null)
  const [lastResult, setLastResult] = useState(null)

  useEffect(() => {
    adminDevApi.status()
      .then((data) => {
        setAvailable(true)
        setResetEnabled(!!data.resetEnabled)
      })
      .catch(() => {
        setAvailable(false)
        setResetEnabled(false)
      })
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => {
    if (resetToken) {
      sessionStorage.setItem(TOKEN_STORAGE_KEY, resetToken)
    } else {
      sessionStorage.removeItem(TOKEN_STORAGE_KEY)
    }
  }, [resetToken])

  const assertToken = () => {
    if (!resetToken.trim()) {
      notify.error('Saisissez le jeton X-Reset-Token (voir application-dev.yml ou APP_RESET_TOKEN)')
      return false
    }
    return true
  }

  const runAction = async (key, label, action) => {
    if (!assertToken()) return
    if (!window.confirm(`${label}\n\nCette action est irréversible sur les données métier.`)) return
    setBusy(key)
    setLastResult(null)
    try {
      const result = await action(resetToken.trim())
      setLastResult(result)
      notify.success('Opération terminée')
    } catch (e) {
      notify.error(getErrorMessage(e))
    } finally {
      setBusy(null)
    }
  }

  if (loading) return <Loading />

  return (
    <div>
      <PageHeader
        title="Outils développement"
        subtitle="Purge et rechargement du jeu de démo (profil backend dev uniquement)"
      />

      {!available && (
        <Card className="p-6 border-amber-200 bg-amber-50">
          <p className="text-sm text-amber-900 font-medium">Backend non disponible en mode dev</p>
          <p className="text-sm text-amber-800 mt-2">
            Lancez le backend avec <code className="text-xs bg-white px-1 rounded">.\dev.ps1</code>
            {' '}(profil <code className="text-xs bg-white px-1 rounded">dev</code>).
            Le reset complet PostgreSQL (<code className="text-xs bg-white px-1 rounded">reset-db.ps1 -Mode full</code>)
            {' '}reste à exécuter en ligne de commande.
          </p>
        </Card>
      )}

      {available && (
        <div className="space-y-6">
          <Card className="p-6">
            <div className="flex items-center gap-2 mb-4">
              <h3 className="text-sm font-semibold text-gray-900">État</h3>
              <Badge tone={resetEnabled ? 'success' : 'warning'}>
                {resetEnabled ? 'Reset activé' : 'Reset désactivé'}
              </Badge>
            </div>
            <p className="text-sm text-gray-600">
              Équivalent des appels <code className="text-xs bg-gray-100 px-1 rounded">POST /api/admin/reset-demo</code>
              {' '}et <code className="text-xs bg-gray-100 px-1 rounded">seed-demo</code>.
              Les utilisateurs, rôles et paramètres applicatifs sont conservés lors de la purge.
            </p>
          </Card>

          <Card className="p-6">
            <label className="block max-w-md">
              <span className="text-xs font-medium text-gray-500 uppercase tracking-wide">
                Jeton X-Reset-Token
              </span>
              <input
                type="password"
                autoComplete="off"
                className="mt-1 w-full border border-gray-200 rounded-lg px-3 py-2 text-sm font-mono"
                placeholder="dev-reset-token-change-me"
                value={resetToken}
                onChange={(e) => setResetToken(e.target.value)}
              />
              <span className="text-xs text-gray-400 mt-1 block">
                Configurable via <code className="bg-gray-100 px-1 rounded">app.admin.reset-token</code>
                {' '}ou variable <code className="bg-gray-100 px-1 rounded">APP_RESET_TOKEN</code>.
              </span>
            </label>
          </Card>

          <Card className="p-6">
            <h3 className="text-sm font-semibold text-gray-900 mb-4">Actions</h3>
            <div className="flex flex-wrap gap-3">
              <Button
                variant="danger"
                disabled={!resetEnabled || !!busy}
                onClick={() => runAction('reset', 'Purger toutes les données métier ?', adminDevApi.resetDemo)}
              >
                {busy === 'reset' ? 'Purge…' : 'Purger données métier'}
              </Button>
              <Button
                variant="secondary"
                disabled={!resetEnabled || !!busy}
                onClick={() => runAction('seed', 'Charger le produit démo DEMO-EAU-1L ?', adminDevApi.seedDemo)}
              >
                {busy === 'seed' ? 'Chargement…' : 'Charger jeu de démo'}
              </Button>
              <Button
                disabled={!resetEnabled || !!busy}
                onClick={() => runAction(
                  'both',
                  'Purger puis recharger le jeu de démo (équivalent reset-db.ps1 -Mode demo -SeedDemo) ?',
                  adminDevApi.resetAndSeedDemo,
                )}
              >
                {busy === 'both' ? 'Reset + seed…' : 'Purge + jeu de démo'}
              </Button>
            </div>
            <p className="text-xs text-gray-500 mt-4">
              Jeu de démo : produit <strong>DEMO-EAU-1L</strong>, conditionnements unité/carton/palette, stock 500 L.
            </p>
          </Card>

          {lastResult && (
            <Card className="p-6">
              <h3 className="text-sm font-semibold text-gray-900 mb-2">Dernier résultat</h3>
              <pre className="text-xs bg-gray-50 border border-gray-200 rounded-lg p-3 overflow-x-auto">
                {JSON.stringify(lastResult, null, 2)}
              </pre>
            </Card>
          )}
        </div>
      )}
    </div>
  )
}
