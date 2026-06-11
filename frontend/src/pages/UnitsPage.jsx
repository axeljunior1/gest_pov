import { useEffect, useState } from 'react'
import { unitsApi } from '../api'
import { PageHeader, Card, Button, Loading, Alert } from '../components/ui'
import { useAsyncAction } from '../hooks/useAsyncAction'
import { useNotification } from '../context/NotificationContext'
import { getErrorMessage } from '../utils/errors'

const UNIT_GROUPS = [
  { title: 'Comptage', symboles: ['pcs', 'dz'] },
  { title: 'Masse', symboles: ['kg', 'g', 'mg', 't', 'q'] },
  { title: 'Volume', symboles: ['L', 'mL', 'cL', 'dL', 'hL', 'm³'] },
  { title: 'Longueur', symboles: ['km', 'm', 'cm', 'mm'] },
  { title: 'Surface', symboles: ['m²', 'cm²', 'mm²'] },
]

export default function UnitsPage() {
  const notify = useNotification()
  const { run, submitting } = useAsyncAction()
  const [units, setUnits] = useState([])
  const [conversions, setConversions] = useState([])
  const [loading, setLoading] = useState(true)
  const [pageError, setPageError] = useState('')
  const [convertForm, setConvertForm] = useState({ fromUnitId: '', toUnitId: '', quantity: '' })
  const [convertResult, setConvertResult] = useState(null)

  const load = async () => {
    setLoading(true)
    setPageError('')
    try {
      const [u, c] = await Promise.all([unitsApi.getAll(), unitsApi.getConversions()])
      setUnits(u)
      setConversions(c)
    } catch (error) {
      const message = getErrorMessage(error)
      setPageError(message)
      notify.error(message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [])

  const handleConvert = async () => {
    const quantity = parseFloat(String(convertForm.quantity).replace(',', '.'))
    if (!convertForm.fromUnitId) { notify.error('Sélectionnez l\'unité de départ.'); return }
    if (!convertForm.toUnitId) { notify.error('Sélectionnez l\'unité d\'arrivée.'); return }
    if (convertForm.quantity === '' || !Number.isFinite(quantity)) {
      notify.error('Indiquez une quantité valide.')
      return
    }
    try {
      const result = await unitsApi.convert({
        fromUnitId: convertForm.fromUnitId,
        toUnitId: convertForm.toUnitId,
        quantity,
      })
      setConvertResult(result.result)
    } catch (error) {
      setConvertResult(null)
      notify.error(getErrorMessage(error))
    }
  }

  const unitsBySymbole = Object.fromEntries(units.map((u) => [u.symbole, u]))

  const handleDeleteUnit = (unit) => {
    if (!confirm(`Supprimer l'unité « ${unit.nom} (${unit.symbole}) » ?`)) return
    run(
      () => unitsApi.delete(unit.id),
      { successMessage: 'Unité supprimée', onSuccess: load },
    )
  }

  const handleDeleteConversion = (conversion) => {
    if (!confirm(`Supprimer la conversion ${conversion.fromUnitSymbole} → ${conversion.toUnitSymbole} ?`)) return
    run(
      () => unitsApi.deleteConversion(conversion.id),
      { successMessage: 'Conversion supprimée', onSuccess: load },
    )
  }

  return (
    <>
      <PageHeader
        title="Unités de mesure"
        subtitle="Catalogue SI / international pré-configuré — 20 unités avec conversions globales"
      />

      <Alert message={pageError} onDismiss={() => setPageError('')} />

      <Card className="p-4 mb-6 bg-blue-50 border-blue-100 text-sm text-blue-900 space-y-1">
        <p><strong>Catalogue de référence</strong> — unités et conversions reconnues (kg↔g, L↔mL, m↔cm…).</p>
        <p>Le stock produit reste en <strong>unité de base</strong>. Suppression possible si l'unité n'est pas utilisée par un produit.</p>
      </Card>

      <Card className="p-5 mb-6">
        <h3 className="text-sm font-medium mb-3">Calculateur de conversion</h3>
        <div className="flex gap-3 items-end flex-wrap">
          <input placeholder="Quantité" type="number" step="any" value={convertForm.quantity} onChange={(e) => setConvertForm({ ...convertForm, quantity: e.target.value })} />
          <select value={convertForm.fromUnitId} onChange={(e) => setConvertForm({ ...convertForm, fromUnitId: e.target.value })} className="min-w-36">
            <option value="">De</option>
            {units.map((u) => <option key={u.id} value={String(u.id)}>{u.nom} ({u.symbole})</option>)}
          </select>
          <select value={convertForm.toUnitId} onChange={(e) => setConvertForm({ ...convertForm, toUnitId: e.target.value })} className="min-w-36">
            <option value="">Vers</option>
            {units.map((u) => <option key={u.id} value={String(u.id)}>{u.nom} ({u.symbole})</option>)}
          </select>
          <Button variant="secondary" onClick={handleConvert}>Convertir</Button>
          {convertResult != null && <span className="text-sm font-medium">= {convertResult}</span>}
        </div>
      </Card>

      {loading ? <Loading /> : (
        <div className="grid grid-cols-2 gap-6">
          <Card className="overflow-hidden">
            <div className="px-5 py-3 border-b text-sm font-medium">20 unités de référence</div>
            <div className="p-5 space-y-4">
              {UNIT_GROUPS.map((group) => {
                const groupUnits = group.symboles
                  .map((s) => unitsBySymbole[s])
                  .filter(Boolean)
                if (groupUnits.length === 0) return null
                return (
                  <div key={group.title}>
                    <p className="text-xs font-medium text-gray-500 mb-2">{group.title}</p>
                    <div className="flex flex-wrap gap-2">
                      {groupUnits.map((u) => (
                        <span key={u.id} className="inline-flex items-center gap-1 px-2.5 py-1 bg-gray-100 rounded-md text-xs group">
                          {u.nom} <span className="font-mono text-gray-500">({u.symbole})</span>
                          <button
                            type="button"
                            disabled={submitting}
                            onClick={() => handleDeleteUnit(u)}
                            className="text-red-500 hover:text-red-700 opacity-60 group-hover:opacity-100"
                            title="Supprimer"
                          >
                            ×
                          </button>
                        </span>
                      ))}
                    </div>
                  </div>
                )
              })}
            </div>
          </Card>

          <Card className="overflow-hidden">
            <div className="px-5 py-3 border-b text-sm font-medium">Conversions globales pré-définies</div>
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b text-left text-gray-500">
                  <th className="px-5 py-3">Conversion</th>
                  <th className="px-5 py-3">Facteur</th>
                  <th className="px-5 py-3 w-20"></th>
                </tr>
              </thead>
              <tbody>
                {conversions.length === 0 ? (
                  <tr><td colSpan={3} className="px-5 py-8 text-center text-gray-400">Chargement du catalogue…</td></tr>
                ) : conversions.map((c) => (
                  <tr key={c.id} className="border-b border-gray-50">
                    <td className="px-5 py-3">1 {c.fromUnitSymbole} = {c.factor} {c.toUnitSymbole}</td>
                    <td className="px-5 py-3 font-mono text-xs">{c.factor}</td>
                    <td className="px-5 py-3">
                      <Button variant="ghost" className="text-xs text-red-600" disabled={submitting} onClick={() => handleDeleteConversion(c)}>
                        Suppr.
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </Card>
        </div>
      )}
    </>
  )
}
