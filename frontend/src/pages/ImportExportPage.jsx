import { useEffect, useState } from 'react'
import { exportApi, importApi } from '../api'
import { useAuth } from '../context/AuthContext'
import { PageHeader, Card, Button, Loading, Badge, EmptyState } from '../components/ui'
import { useNotification } from '../context/NotificationContext'
import { getErrorMessage } from '../utils/errors'

const EXPORT_TYPES = [
  { key: 'products', label: 'Produits', fn: exportApi.products },
  { key: 'brands', label: 'Marques', fn: exportApi.brands },
  { key: 'categories', label: 'Catégories', fn: exportApi.categories },
  { key: 'suppliers', label: 'Fournisseurs', fn: exportApi.suppliers },
  { key: 'units', label: 'Unités', fn: exportApi.units },
  { key: 'warehouses', label: 'Entrepôts', fn: exportApi.warehouses },
  { key: 'stock', label: 'Stock actuel', fn: exportApi.stock },
  { key: 'movements', label: 'Mouvements', fn: exportApi.movements },
  { key: 'entries', label: 'Entrées', fn: exportApi.entries },
  { key: 'exits', label: 'Sorties', fn: exportApi.exits },
  { key: 'alerts', label: 'Alertes', fn: exportApi.alerts },
  { key: 'inventories', label: 'Inventaires', fn: exportApi.inventories },
]

const IMPORT_TYPES_WITH_DUPLICATE = new Set([
  'products', 'brands', 'categories', 'suppliers', 'units', 'warehouses',
])

const IMPORT_TYPE_OPTIONS = [
  { value: 'products', label: 'Produits', template: 'products' },
  { value: 'brands', label: 'Marques', template: 'brands' },
  { value: 'categories', label: 'Catégories', template: 'categories' },
  { value: 'suppliers', label: 'Fournisseurs', template: 'suppliers' },
  { value: 'units', label: 'Unités', template: 'units' },
  { value: 'warehouses', label: 'Entrepôts', template: 'warehouses' },
  { value: 'packagings', label: 'Conditionnements', template: 'packagings' },
  { value: 'initial-stock', label: 'Stock initial', template: 'initial-stock' },
]

export default function ImportExportPage() {
  const notify = useNotification()
  const { hasPermission } = useAuth()
  const canExport = hasPermission('export.read')
  const canImport = hasPermission('import.read')
  const canValidate = hasPermission('import.create')

  const [format, setFormat] = useState('CSV')
  const [duplicateMode, setDuplicateMode] = useState('REJECT')
  const [importType, setImportType] = useState('products')
  const [file, setFile] = useState(null)
  const [preview, setPreview] = useState(null)
  const [history, setHistory] = useState([])
  const [loading, setLoading] = useState(false)
  const [exporting, setExporting] = useState(false)

  useEffect(() => {
    if (canImport) {
      importApi.history().then(setHistory).catch(() => {})
    }
  }, [canImport])

  const runExport = async (exp) => {
    setExporting(true)
    try {
      await exp.fn(format)
      notify.success(`Export ${exp.label} téléchargé`)
    } catch (e) {
      notify.error(getErrorMessage(e, { module: 'import' }))
    } finally {
      setExporting(false)
    }
  }

  const runPreview = async () => {
    if (!file) {
      notify.error('Sélectionnez un fichier')
      return
    }
    setLoading(true)
    try {
      const result = await runImportAction('preview')
      setPreview(result)
    } catch (e) {
      notify.error(getErrorMessage(e, { module: 'import' }))
    } finally {
      setLoading(false)
    }
  }

  const runValidate = async () => {
    if (!file || !canValidate) return
    setLoading(true)
    try {
      const result = await runImportAction('validate')
      setPreview(result)
      notify.success(`Import terminé — ${result.job.successRows} ligne(s) OK`)
      importApi.history().then(setHistory).catch(() => {})
    } catch (e) {
      notify.error(getErrorMessage(e, { module: 'import' }))
    } finally {
      setLoading(false)
    }
  }

  const runImportAction = async (action) => {
    if (action === 'preview') {
      if (importType === 'products') return importApi.previewProducts(file, duplicateMode)
      if (importType === 'brands') return importApi.previewBrands(file, duplicateMode)
      if (importType === 'categories') return importApi.previewCategories(file, duplicateMode)
      if (importType === 'suppliers') return importApi.previewSuppliers(file, duplicateMode)
      if (importType === 'units') return importApi.previewUnits(file, duplicateMode)
      if (importType === 'warehouses') return importApi.previewWarehouses(file, duplicateMode)
      if (importType === 'packagings') return importApi.previewPackagings(file)
      return importApi.previewInitialStock(file)
    }
    if (importType === 'products') return importApi.validateProducts(file, duplicateMode)
    if (importType === 'brands') return importApi.validateBrands(file, duplicateMode)
    if (importType === 'categories') return importApi.validateCategories(file, duplicateMode)
    if (importType === 'suppliers') return importApi.validateSuppliers(file, duplicateMode)
    if (importType === 'units') return importApi.validateUnits(file, duplicateMode)
    if (importType === 'warehouses') return importApi.validateWarehouses(file, duplicateMode)
    if (importType === 'packagings') return importApi.validatePackagings(file)
    return importApi.validateInitialStock(file)
  }

  const downloadTemplate = async (type) => {
    try {
      await importApi.downloadTemplate(type, format)
      notify.success('Template téléchargé')
    } catch (e) {
      // 403/404 : message clair, pas de redirect login (géré côté client.js + wrapper)
      const status = e?.response?.status
      if (status === 404) {
        notify.error("Template d'import indisponible. Redémarrez ou mettez à jour le serveur Gest POV.")
        return
      }
      notify.error(getErrorMessage(e, { module: 'import' }))
    }
  }

  if (!canExport && !canImport) {
    return (
      <EmptyState message="Vous n'avez pas les permissions import/export." />
    )
  }

  return (
    <div>
      <PageHeader
        title="Import / Export"
        subtitle="Échange de données CSV ou Excel avec l'ERP"
      />

      <div className="flex gap-3 mb-6">
        <select className="border border-gray-200 rounded-lg px-3 py-2 text-sm" value={format} onChange={(e) => setFormat(e.target.value)}>
          <option value="CSV">CSV</option>
          <option value="XLSX">Excel (XLSX)</option>
        </select>
      </div>

      {canExport && (
        <Card className="p-6 mb-8">
          <h3 className="text-sm font-semibold text-gray-900 mb-4">Exports</h3>
          <div className="flex flex-wrap gap-2">
            {EXPORT_TYPES.map((exp) => (
              <Button key={exp.key} variant="secondary" disabled={exporting}
                onClick={() => runExport(exp)}>
                {exp.label}
              </Button>
            ))}
          </div>
        </Card>
      )}

      {canImport && (
        <Card className="p-6 mb-8">
          <h3 className="text-sm font-semibold text-gray-900 mb-4">Import</h3>
          <div className="flex flex-wrap gap-3 mb-4">
            <select className="border border-gray-200 rounded-lg px-3 py-2 text-sm" value={importType} onChange={(e) => { setImportType(e.target.value); setPreview(null); setFile(null) }}>
              {IMPORT_TYPE_OPTIONS.map((opt) => (
                <option key={opt.value} value={opt.value}>{opt.label}</option>
              ))}
            </select>
            {IMPORT_TYPES_WITH_DUPLICATE.has(importType) && (
              <select className="border border-gray-200 rounded-lg px-3 py-2 text-sm" value={duplicateMode} onChange={(e) => setDuplicateMode(e.target.value)}>
                <option value="REJECT">Existant → refuser</option>
                <option value="UPDATE">Existant → mettre à jour</option>
                <option value="SKIP">Existant → ignorer</option>
              </select>
            )}
            <Button variant="secondary" onClick={() => downloadTemplate(
              IMPORT_TYPE_OPTIONS.find((o) => o.value === importType)?.template || importType
            )}>
              Télécharger le template
            </Button>
          </div>
          <input type="file" accept=".csv,.xlsx" className="text-sm mb-4 block"
            onChange={(e) => { setFile(e.target.files?.[0] || null); setPreview(null) }} />
          <div className="flex gap-2">
            <Button onClick={runPreview} disabled={loading || !file}>Prévisualiser</Button>
            {canValidate && (
              <Button variant="secondary" onClick={runValidate} disabled={loading || !file}>Valider l'import</Button>
            )}
          </div>

          {loading && <div className="mt-4"><Loading /></div>}

          {preview && !loading && (
            <div className="mt-6">
              <p className="text-sm text-gray-600 mb-3">
                {preview.validRows ?? preview.job?.successRows} valide(s) · {preview.errorRows ?? preview.job?.errorRows} erreur(s)
              </p>
              <ul className="divide-y divide-gray-100 text-sm max-h-64 overflow-auto">
                {(preview.lines || []).map((line) => (
                  <li key={line.lineNumber} className="py-2 flex gap-2 items-start">
                    <Badge tone={line.status === 'OK' ? 'success' : 'danger'}>{line.status}</Badge>
                    <span>L{line.lineNumber} · {line.identifier || '—'}</span>
                    {line.action && <span className="text-gray-400">({line.action})</span>}
                    {line.message && <span className="text-red-600">{line.message}</span>}
                  </li>
                ))}
              </ul>
            </div>
          )}
        </Card>
      )}

      {canImport && history.length > 0 && (
        <Card className="p-6">
          <h3 className="text-sm font-semibold text-gray-900 mb-4">Historique des imports</h3>
          <table className="w-full text-sm">
            <thead>
              <tr className="text-left text-gray-500 border-b">
                <th className="pb-2">Date</th>
                <th className="pb-2">Type</th>
                <th className="pb-2">Fichier</th>
                <th className="pb-2">Statut</th>
                <th className="pb-2 text-right">OK / Erreurs</th>
              </tr>
            </thead>
            <tbody>
              {history.map((job) => (
                <tr key={job.id} className="border-b border-gray-50">
                  <td className="py-2">{new Date(job.createdAt).toLocaleString('fr-FR')}</td>
                  <td className="py-2">{job.importType}</td>
                  <td className="py-2">{job.fileName}</td>
                  <td className="py-2"><Badge tone={job.status === 'COMPLETED' ? 'success' : job.status === 'PARTIAL' ? 'warning' : 'danger'}>{job.status}</Badge></td>
                  <td className="py-2 text-right">{job.successRows} / {job.errorRows}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </Card>
      )}
    </div>
  )
}
