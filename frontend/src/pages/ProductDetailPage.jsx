import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import {
  productsApi, categoriesApi, suppliersApi, unitsApi, attributesApi, barcodesApi,
} from '../api'
import { PageHeader, Card, Button, Badge, Tabs, Loading, Alert } from '../components/ui'
import { useAsyncAction } from '../hooks/useAsyncAction'
import { useNotification } from '../context/NotificationContext'
import { getErrorMessage } from '../utils/errors'
import { buildProductPayload, buildVariantPayload } from '../utils/payload'
import {
  PRODUCT_STATUS, LIFECYCLE_STATUS, PRICE_TYPES, BARCODE_TYPES, DOCUMENT_TYPES,
  formatPrice, formatDate, lifecycleLabel, statusLabel,
} from '../utils/constants'

const emptyVariant = () => ({
  couleur: '', taille: '', sku: '', prix: '', stock: 0,
  generateBarcode: true, barcodeType: 'CODE128',
})

const initialForm = () => ({
  nom: '', sku: '', description: '', marque: '',
  categorieId: '', fournisseurPrincipalId: '', unitId: '',
  prixAchat: '', prixVente: '', prixPromotionnel: '',
  statut: 'ACTIF', cycleVie: 'BROUILLON',
  variantes: [emptyVariant()],
  attributs: {},
})

export default function ProductDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const notify = useNotification()
  const { run, submitting } = useAsyncAction()
  const isNew = id === 'new'

  const [tab, setTab] = useState('general')
  const [loading, setLoading] = useState(!isNew)
  const [pageError, setPageError] = useState('')
  const [product, setProduct] = useState(null)
  const [categories, setCategories] = useState([])
  const [suppliers, setSuppliers] = useState([])
  const [units, setUnits] = useState([])
  const [attrDefs, setAttrDefs] = useState([])
  const [priceHistory, setPriceHistory] = useState([])
  const [audit, setAudit] = useState([])

  const [form, setForm] = useState(initialForm)

  const [newVariant, setNewVariant] = useState(emptyVariant())
  const [newSupplier, setNewSupplier] = useState({ supplierId: '', principal: false, referenceFournisseur: '', delaiLivraisonJours: '', prixNegocie: '' })
  const [priceForm, setPriceForm] = useState({ type: 'VENTE', nouveauPrix: '' })
  const [lifecycleForm, setLifecycleForm] = useState({ cycleVie: 'BROUILLON' })
  const [barcodePreview, setBarcodePreview] = useState(null)
  const [packagingForm, setPackagingForm] = useState({
    nom: '', symbole: '', quantiteBase: '', prixVente: '', defaultVente: false, principal: false,
  })
  const [packagingConvert, setPackagingConvert] = useState({ packagingId: '', quantity: '' })
  const [packagingPreview, setPackagingPreview] = useState(null)

  useEffect(() => {
    Promise.all([
      categoriesApi.getTree(),
      suppliersApi.getAll(),
      unitsApi.getAll(),
      attributesApi.getAll(),
    ]).then(([cats, sups, uns, attrs]) => {
      setCategories(cats)
      setSuppliers(sups)
      setUnits(uns)
      setAttrDefs(attrs)
    }).catch((error) => notify.error(getErrorMessage(error)))
  }, [notify])

  useEffect(() => {
    if (isNew) {
      setForm(initialForm())
      setProduct(null)
      setPageError('')
      setTab('general')
      setLoading(false)
      return
    }
    if (!id) return
    loadProduct()
  }, [id, isNew])

  const loadProduct = async () => {
    setLoading(true)
    setPageError('')
    try {
      const data = await productsApi.getById(id)
      setProduct(data)
      setForm({
        nom: data.nom, sku: data.sku, description: data.description || '',
        marque: data.marque || '', categorieId: data.categorieId || '',
        fournisseurPrincipalId: data.fournisseurPrincipalId || '',
        unitId: data.unitId || '', prixAchat: data.prixAchat || '',
        prixVente: data.prixVente || '', prixPromotionnel: data.prixPromotionnel || '',
        statut: data.statut, cycleVie: data.cycleVie,
        variantes: data.variantes?.length ? data.variantes : [emptyVariant()],
        attributs: data.attributs || {},
      })
      setLifecycleForm({ cycleVie: data.cycleVie })
      const [history, auditLog] = await Promise.all([
        productsApi.getPriceHistory(id),
        productsApi.getAudit(id),
      ])
      setPriceHistory(history)
      setAudit(auditLog)
    } catch (error) {
      const message = getErrorMessage(error)
      setPageError(message)
      notify.error(message)
    } finally {
      setLoading(false)
    }
  }

  const flatCategories = (cats, prefix = '') =>
    cats.flatMap((c) => [
      { id: c.id, label: prefix + c.nom },
      ...(c.children ? flatCategories(c.children, prefix + c.nom + ' > ') : []),
    ])

  const buildPayload = () => buildProductPayload(form, { isNew })

  const handleSave = () => {
    if (!form.nom.trim() || !form.sku.trim()) {
      notify.error('Le nom et le SKU sont obligatoires.')
      return
    }
    const payload = buildPayload()
    if (isNew) {
      run(
        () => productsApi.create(payload),
        {
          successMessage: 'Produit créé',
          onSuccess: (created) => navigate(`/products/${created.id}`),
        },
      )
    } else {
      run(
        () => productsApi.update(id, payload),
        { successMessage: 'Produit enregistré', onSuccess: loadProduct },
      )
    }
  }

  const handleAddVariant = () => {
    if (!newVariant.sku.trim()) {
      notify.error('Le SKU de la variante est obligatoire.')
      return
    }
    run(
      () => productsApi.addVariant(id, buildVariantPayload(newVariant)),
      {
        successMessage: 'Variante ajoutée',
        onSuccess: () => { setNewVariant(emptyVariant()); loadProduct() },
      },
    )
  }

  const handleAddSupplier = () => {
    if (!newSupplier.supplierId) {
      notify.error('Sélectionnez un fournisseur.')
      return
    }
    run(
      () => productsApi.addSupplier(id, {
        ...newSupplier,
        supplierId: Number(newSupplier.supplierId),
        delaiLivraisonJours: newSupplier.delaiLivraisonJours ? Number(newSupplier.delaiLivraisonJours) : null,
        prixNegocie: newSupplier.prixNegocie ? Number(newSupplier.prixNegocie) : null,
      }),
      {
        successMessage: 'Fournisseur lié',
        onSuccess: () => {
          setNewSupplier({ supplierId: '', principal: false, referenceFournisseur: '', delaiLivraisonJours: '', prixNegocie: '' })
          loadProduct()
        },
      },
    )
  }

  const handleUpdatePrice = () => {
    if (!priceForm.nouveauPrix) {
      notify.error('Indiquez le nouveau prix.')
      return
    }
    run(
      () => productsApi.updatePrice(id, {
        ...priceForm,
        nouveauPrix: Number(priceForm.nouveauPrix),
      }),
      { successMessage: 'Prix mis à jour', onSuccess: loadProduct },
    )
  }

  const handleUpdateLifecycle = () => {
    run(
      () => productsApi.updateLifecycle(id, lifecycleForm),
      { successMessage: 'Cycle de vie mis à jour', onSuccess: loadProduct },
    )
  }

  const handleGenerateBarcode = async (content, type) => {
    try {
      const result = await barcodesApi.generate({ content, type })
      setBarcodePreview(result)
    } catch (error) {
      notify.error(getErrorMessage(error))
    }
  }

  const handleImageUpload = async (e) => {
    const file = e.target.files?.[0]
    if (!file) return
    const result = await run(
      () => productsApi.uploadImage(id, file, product?.images?.length === 0),
      { successMessage: 'Image ajoutée', onSuccess: loadProduct },
    )
    if (!result.ok) e.target.value = ''
  }

  const handleDocUpload = async (e, type) => {
    const file = e.target.files?.[0]
    if (!file) return
    const result = await run(
      () => productsApi.uploadDocument(id, file, type),
      { successMessage: 'Document ajouté', onSuccess: loadProduct },
    )
    if (!result.ok) e.target.value = ''
  }

  const handleDeleteProduct = () => {
    if (!confirm(`Supprimer « ${product?.nom} » ? Toutes les données associées seront effacées.`)) return
    run(
      () => productsApi.delete(id),
      {
        successMessage: 'Produit supprimé',
        onSuccess: () => navigate('/'),
      },
    )
  }

  const handleDeleteImage = (imageId) => {
    if (!confirm('Supprimer cette image ?')) return
    run(
      () => productsApi.deleteImage(id, imageId),
      { successMessage: 'Image supprimée', onSuccess: loadProduct },
    )
  }

  const handleDeleteDocument = (documentId) => {
    if (!confirm('Supprimer ce document ?')) return
    run(
      () => productsApi.deleteDocument(id, documentId),
      { successMessage: 'Document supprimé', onSuccess: loadProduct },
    )
  }

  const handleDeleteVariant = (variantId) => {
    if (!confirm('Supprimer cette variante ?')) return
    run(
      () => productsApi.deleteVariant(id, variantId),
      { successMessage: 'Variante supprimée', onSuccess: loadProduct },
    )
  }

  const handleRemoveSupplier = (productSupplierId) => {
    if (!confirm('Retirer ce fournisseur ?')) return
    run(
      () => productsApi.removeSupplier(id, productSupplierId),
      { successMessage: 'Fournisseur retiré', onSuccess: loadProduct },
    )
  }

  const handleAddPackaging = () => {
    if (!packagingForm.nom.trim()) {
      notify.error('Le nom du conditionnement est obligatoire.')
      return
    }
    const quantiteBase = parseFloat(String(packagingForm.quantiteBase).replace(',', '.'))
    if (!packagingForm.quantiteBase || !Number.isFinite(quantiteBase) || quantiteBase <= 0) {
      notify.error('Indiquez combien d\'unités de base contient 1 conditionnement (ex: 12).')
      return
    }
    let prixVente = parseFloat(String(packagingForm.prixVente).replace(',', '.'))
    if (!packagingForm.prixVente || !Number.isFinite(prixVente)) {
      const unitPrice = Number(form.prixVente) || 0
      prixVente = unitPrice * quantiteBase
    }
    run(
      () => productsApi.addPackaging(id, {
        nom: packagingForm.nom.trim(),
        symbole: packagingForm.symbole || null,
        quantiteBase,
        prixVente,
        defaultVente: packagingForm.defaultVente,
        principal: packagingForm.principal,
      }),
      {
        successMessage: 'Conditionnement ajouté',
        onSuccess: () => {
          setPackagingForm({
            nom: '', symbole: '', quantiteBase: '', prixVente: '', defaultVente: false, principal: false,
          })
          loadProduct()
        },
      },
    )
  }

  const handleDeletePackaging = (packagingId) => {
    if (!confirm('Supprimer ce conditionnement ?')) return
    run(
      () => productsApi.deletePackaging(id, packagingId),
      { successMessage: 'Conditionnement supprimé', onSuccess: loadProduct },
    )
  }

  const handlePackagingConvertPreview = async () => {
    if (!packagingConvert.packagingId) {
      notify.error('Sélectionnez un conditionnement.')
      return
    }
    const quantity = parseFloat(String(packagingConvert.quantity).replace(',', '.'))
    if (!packagingConvert.quantity || !Number.isFinite(quantity) || quantity <= 0) {
      notify.error('Indiquez une quantité valide.')
      return
    }
    try {
      const result = await productsApi.convertPackagingToBase(id, {
        packagingId: Number(packagingConvert.packagingId),
        quantity,
      })
      setPackagingPreview(result)
    } catch (error) {
      setPackagingPreview(null)
      notify.error(getErrorMessage(error))
    }
  }

  if (loading) return <Loading />

  if (!isNew && pageError && !product) {
    return (
      <>
        <PageHeader title="Produit" subtitle="Fiche produit" action={<Button variant="secondary" onClick={() => navigate('/')}>Retour</Button>} />
        <Alert message={pageError} />
      </>
    )
  }

  const tabs = [
    { id: 'general', label: 'Général' },
    ...(!isNew ? [{ id: 'packagings', label: 'Conditionnements' }] : []),
    { id: 'variants', label: 'Variantes' },
    { id: 'suppliers', label: 'Fournisseurs' },
    { id: 'prices', label: 'Prix' },
    { id: 'media', label: 'Images & Docs' },
    { id: 'attributes', label: 'Attributs' },
    { id: 'lifecycle', label: 'Cycle de vie' },
    { id: 'audit', label: 'Historique' },
  ]

  const setField = (key, value) => setForm({ ...form, [key]: value })

  return (
    <>
      <PageHeader
        title={isNew ? 'Nouveau produit' : product?.nom}
        subtitle={isNew ? 'Créer une fiche produit' : product?.sku}
        action={
          <div className="flex gap-2">
            <Button variant="secondary" onClick={() => navigate('/')}>Retour</Button>
            {!isNew && (
              <Button variant="ghost" className="text-red-600 border border-red-200" onClick={handleDeleteProduct} disabled={submitting}>
                Supprimer
              </Button>
            )}
            <Button onClick={handleSave} disabled={submitting}>{isNew ? 'Créer' : 'Enregistrer'}</Button>
          </div>
        }
      />

      <Alert message={pageError} onDismiss={() => setPageError('')} />

      {!isNew && product && (
        <div className="flex gap-2 mb-6">
          <Badge tone={product.statut === 'ACTIF' ? 'success' : 'default'}>{statusLabel[product.statut]}</Badge>
          <Badge tone="info">{lifecycleLabel[product.cycleVie]}</Badge>
          <Badge>Stock: {product.stockTotal ?? 0} {product.baseUnitSymbole || product.unitSymbole || ''}</Badge>
        </div>
      )}

      <Tabs tabs={tabs} active={tab} onChange={setTab} />

      {tab === 'general' && (
        <Card className="p-6 space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-medium text-gray-500 mb-1">Nom</label>
              <input value={form.nom} onChange={(e) => setField('nom', e.target.value)} className="w-full" />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-500 mb-1">SKU</label>
              <input value={form.sku} onChange={(e) => setField('sku', e.target.value)} className="w-full font-mono" />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-500 mb-1">Marque</label>
              <input value={form.marque} onChange={(e) => setField('marque', e.target.value)} className="w-full" />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-500 mb-1">Catégorie</label>
              <select value={form.categorieId} onChange={(e) => setField('categorieId', e.target.value)} className="w-full">
                <option value="">—</option>
                {flatCategories(categories).map((c) => (
                  <option key={c.id} value={c.id}>{c.label}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-500 mb-1">Fournisseur principal</label>
              <select value={form.fournisseurPrincipalId} onChange={(e) => setField('fournisseurPrincipalId', e.target.value)} className="w-full">
                <option value="">—</option>
                {suppliers.map((s) => <option key={s.id} value={s.id}>{s.nom}</option>)}
              </select>
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-500 mb-1">Unité de base (stock)</label>
              <select value={form.unitId} onChange={(e) => setField('unitId', e.target.value)} className="w-full">
                <option value="">—</option>
                {units.map((u) => <option key={u.id} value={u.id}>{u.nom} ({u.symbole})</option>)}
              </select>
              <p className="text-xs text-gray-400 mt-1">Le stock est toujours exprimé dans cette unité.</p>
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-500 mb-1">Prix achat</label>
              <input type="number" step="0.01" value={form.prixAchat} onChange={(e) => setField('prixAchat', e.target.value)} className="w-full" />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-500 mb-1">Prix vente</label>
              <input type="number" step="0.01" value={form.prixVente} onChange={(e) => setField('prixVente', e.target.value)} className="w-full" />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-500 mb-1">Statut</label>
              <select value={form.statut} onChange={(e) => setField('statut', e.target.value)} className="w-full">
                {PRODUCT_STATUS.map((s) => <option key={s} value={s}>{statusLabel[s]}</option>)}
              </select>
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-500 mb-1">Cycle de vie</label>
              <select value={form.cycleVie} onChange={(e) => setField('cycleVie', e.target.value)} className="w-full">
                {LIFECYCLE_STATUS.map((s) => <option key={s} value={s}>{lifecycleLabel[s]}</option>)}
              </select>
            </div>
          </div>
          <div>
            <label className="block text-xs font-medium text-gray-500 mb-1">Description</label>
            <textarea value={form.description} onChange={(e) => setField('description', e.target.value)} rows={3} className="w-full" />
          </div>

          {isNew && (
            <div className="pt-4 border-t border-gray-100">
              <h3 className="text-sm font-medium mb-3">Variantes initiales</h3>
              {form.variantes.map((v, i) => (
                <div key={i} className="grid grid-cols-5 gap-2 mb-2">
                  <input placeholder="Couleur" value={v.couleur} onChange={(e) => {
                    const variantes = [...form.variantes]
                    variantes[i].couleur = e.target.value
                    setField('variantes', variantes)
                  }} />
                  <input placeholder="Taille" value={v.taille} onChange={(e) => {
                    const variantes = [...form.variantes]
                    variantes[i].taille = e.target.value
                    setField('variantes', variantes)
                  }} />
                  <input placeholder="SKU" value={v.sku} onChange={(e) => {
                    const variantes = [...form.variantes]
                    variantes[i].sku = e.target.value
                    setField('variantes', variantes)
                  }} />
                  <input placeholder="Stock" type="number" value={v.stock} onChange={(e) => {
                    const variantes = [...form.variantes]
                    variantes[i].stock = e.target.value
                    setField('variantes', variantes)
                  }} />
                  <input placeholder="Prix" type="number" step="0.01" value={v.prix} onChange={(e) => {
                    const variantes = [...form.variantes]
                    variantes[i].prix = e.target.value
                    setField('variantes', variantes)
                  }} />
                </div>
              ))}
              <Button variant="secondary" onClick={() => setField('variantes', [...form.variantes, emptyVariant()])}>
                + Variante
              </Button>
            </div>
          )}
        </Card>
      )}

      {tab === 'packagings' && !isNew && (
        <div className="space-y-4">
          <Card className="p-4 bg-amber-50 border-amber-100 text-sm text-amber-900">
            Les conditionnements sont propres à ce produit. Ex: Eau → 1 carton = 12 bouteilles ;
            Soda → 1 carton = 24 canettes. À la réception, le système convertit en unité de base.
          </Card>

          {!form.unitId && (
            <Alert type="warning" message="Définissez d'abord l'unité de base du produit dans l'onglet Général." />
          )}

          <Card className="overflow-hidden">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b text-left text-gray-500">
                  <th className="px-5 py-3">Nom</th>
                  <th className="px-5 py-3">Symbole</th>
                  <th className="px-5 py-3">Contenu (unité de base)</th>
                  <th className="px-5 py-3">Prix vente</th>
                  <th className="px-5 py-3">Vente défaut</th>
                  <th className="px-5 py-3">Principal</th>
                  <th className="px-5 py-3"></th>
                </tr>
              </thead>
              <tbody>
                {product?.conditionnements?.length ? product.conditionnements.map((p) => (
                  <tr key={p.id} className="border-b border-gray-50">
                    <td className="px-5 py-3 font-medium">{p.nom}</td>
                    <td className="px-5 py-3 font-mono text-xs">{p.symbole || '—'}</td>
                    <td className="px-5 py-3">
                      1 {p.nom} = {p.quantiteBase} {p.baseUnitSymbole || product?.baseUnitSymbole}
                    </td>
                    <td className="px-5 py-3 font-medium">{p.prixVente != null ? Number(p.prixVente).toLocaleString('fr-FR') : '—'}</td>
                    <td className="px-5 py-3">{p.defaultVente ? '✓' : ''}</td>
                    <td className="px-5 py-3">{p.principal ? '✓' : ''}</td>
                    <td className="px-5 py-3">
                      <Button variant="ghost" className="text-xs text-red-600" onClick={() => handleDeletePackaging(p.id)}>
                        Suppr.
                      </Button>
                    </td>
                  </tr>
                )) : (
                  <tr><td colSpan={7} className="px-5 py-8 text-center text-gray-400">Aucun conditionnement</td></tr>
                )}
              </tbody>
            </table>
          </Card>

          <Card className="p-5">
            <h3 className="text-sm font-medium mb-3">Ajouter un conditionnement</h3>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
              <input placeholder="Nom (ex: Carton)" value={packagingForm.nom} onChange={(e) => setPackagingForm({ ...packagingForm, nom: e.target.value })} />
              <input placeholder="Symbole (ex: ctn)" value={packagingForm.symbole} onChange={(e) => setPackagingForm({ ...packagingForm, symbole: e.target.value })} />
              <input placeholder="Qté unité de base (ex: 12)" type="number" min="0.000001" step="any" value={packagingForm.quantiteBase} onChange={(e) => {
                const quantiteBase = e.target.value
                const unitPrice = Number(form.prixVente) || 0
                const q = parseFloat(String(quantiteBase).replace(',', '.'))
                const suggested = Number.isFinite(q) && q > 0 ? String(unitPrice * q) : packagingForm.prixVente
                setPackagingForm({ ...packagingForm, quantiteBase, prixVente: suggested })
              }} />
              <input placeholder="Prix vente condi." type="number" min="0" step="any" value={packagingForm.prixVente} onChange={(e) => setPackagingForm({ ...packagingForm, prixVente: e.target.value })} />
              <label className="flex items-center gap-2 text-sm">
                <input type="checkbox" checked={packagingForm.defaultVente} onChange={(e) => setPackagingForm({ ...packagingForm, defaultVente: e.target.checked })} />
                Vente par défaut (POS)
              </label>
              <label className="flex items-center gap-2 text-sm">
                <input type="checkbox" checked={packagingForm.principal} onChange={(e) => setPackagingForm({ ...packagingForm, principal: e.target.checked })} />
                Achat principal
              </label>
            </div>
            <Button className="mt-3" onClick={handleAddPackaging} disabled={submitting || !form.unitId}>Ajouter</Button>
          </Card>

          <Card className="p-5">
            <h3 className="text-sm font-medium mb-3">Simuler une réception (→ stock en unité de base)</h3>
            <div className="flex gap-3 items-end flex-wrap">
              <div>
                <label className="block text-xs text-gray-500 mb-1">Conditionnement</label>
                <select value={packagingConvert.packagingId} onChange={(e) => setPackagingConvert({ ...packagingConvert, packagingId: e.target.value })} className="min-w-40">
                  <option value="">—</option>
                  {product?.conditionnements?.map((p) => (
                    <option key={p.id} value={p.id}>{p.nom} (1 = {p.quantiteBase} {p.baseUnitSymbole})</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-xs text-gray-500 mb-1">Quantité reçue</label>
                <input type="number" min="0.000001" step="any" value={packagingConvert.quantity} onChange={(e) => setPackagingConvert({ ...packagingConvert, quantity: e.target.value })} placeholder="3" />
              </div>
              <Button variant="secondary" onClick={handlePackagingConvertPreview}>Calculer</Button>
            </div>
            {packagingPreview && (
              <div className="mt-4 p-4 bg-emerald-50 border border-emerald-100 rounded-lg text-sm">
                <p className="font-medium text-emerald-900">
                  Stock à enregistrer : {packagingPreview.quantiteBase} {packagingPreview.baseUnitSymbole}
                </p>
                <p className="text-emerald-700 mt-1">{packagingPreview.explanation}</p>
              </div>
            )}
          </Card>
        </div>
      )}

      {tab === 'variants' && !isNew && (
        <div className="space-y-4">
          <Card className="overflow-hidden">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b text-left text-gray-500">
                  <th className="px-5 py-3">Couleur</th>
                  <th className="px-5 py-3">Taille</th>
                  <th className="px-5 py-3">SKU</th>
                  <th className="px-5 py-3">Prix</th>
                  <th className="px-5 py-3">Stock</th>
                  <th className="px-5 py-3">Code-barres</th>
                  <th className="px-5 py-3"></th>
                </tr>
              </thead>
              <tbody>
                {product?.variantes?.map((v) => (
                  <tr key={v.id} className="border-b border-gray-50">
                    <td className="px-5 py-3">{v.couleur || '—'}</td>
                    <td className="px-5 py-3">{v.taille || '—'}</td>
                    <td className="px-5 py-3 font-mono text-xs">{v.sku}</td>
                    <td className="px-5 py-3">{formatPrice(v.prix)}</td>
                    <td className="px-5 py-3">{v.stock}</td>
                    <td className="px-5 py-3">
                      {v.codeBarre ? (
                        <button className="text-xs text-blue-600 hover:underline" onClick={() => handleGenerateBarcode(v.codeBarre, v.barcodeType || 'CODE128')}>
                          {v.codeBarre}
                        </button>
                      ) : '—'}
                    </td>
                    <td className="px-5 py-3">
                      <Button variant="ghost" className="text-red-600 text-xs" onClick={() => handleDeleteVariant(v.id)}>
                        Supprimer
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </Card>

          {barcodePreview && (
            <Card className="p-4 inline-block">
              <img src={`data:image/png;base64,${barcodePreview.imageBase64}`} alt="Code-barres" className="max-h-32" />
            </Card>
          )}

          <Card className="p-5">
            <h3 className="text-sm font-medium mb-3">Ajouter une variante</h3>
            <div className="grid grid-cols-4 gap-3">
              <input placeholder="Couleur" value={newVariant.couleur} onChange={(e) => setNewVariant({ ...newVariant, couleur: e.target.value })} />
              <input placeholder="Taille" value={newVariant.taille} onChange={(e) => setNewVariant({ ...newVariant, taille: e.target.value })} />
              <input placeholder="SKU *" value={newVariant.sku} onChange={(e) => setNewVariant({ ...newVariant, sku: e.target.value })} />
              <input placeholder="Stock" type="number" value={newVariant.stock} onChange={(e) => setNewVariant({ ...newVariant, stock: e.target.value })} />
              <input placeholder="Prix" type="number" step="0.01" value={newVariant.prix} onChange={(e) => setNewVariant({ ...newVariant, prix: e.target.value })} />
              <select value={newVariant.barcodeType} onChange={(e) => setNewVariant({ ...newVariant, barcodeType: e.target.value })}>
                {BARCODE_TYPES.map((t) => <option key={t} value={t}>{t}</option>)}
              </select>
            </div>
            <Button className="mt-3" onClick={handleAddVariant} disabled={submitting}>Ajouter</Button>
          </Card>
        </div>
      )}

      {tab === 'suppliers' && !isNew && (
        <div className="space-y-4">
          <Card className="overflow-hidden">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b text-left text-gray-500">
                  <th className="px-5 py-3">Fournisseur</th>
                  <th className="px-5 py-3">Réf.</th>
                  <th className="px-5 py-3">Délai (j)</th>
                  <th className="px-5 py-3">Prix négocié</th>
                  <th className="px-5 py-3">Principal</th>
                  <th className="px-5 py-3"></th>
                </tr>
              </thead>
              <tbody>
                {product?.fournisseurs?.map((f) => (
                  <tr key={f.id} className="border-b border-gray-50">
                    <td className="px-5 py-3 font-medium">{f.supplierNom}</td>
                    <td className="px-5 py-3">{f.referenceFournisseur || '—'}</td>
                    <td className="px-5 py-3">{f.delaiLivraisonJours ?? '—'}</td>
                    <td className="px-5 py-3">{formatPrice(f.prixNegocie)}</td>
                    <td className="px-5 py-3">{f.principal ? '✓' : ''}</td>
                    <td className="px-5 py-3">
                      <Button variant="ghost" className="text-red-600 text-xs" onClick={() => handleRemoveSupplier(f.id)}>
                        Retirer
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </Card>
          <Card className="p-5">
            <h3 className="text-sm font-medium mb-3">Lier un fournisseur</h3>
            <div className="grid grid-cols-3 gap-3">
              <select value={newSupplier.supplierId} onChange={(e) => setNewSupplier({ ...newSupplier, supplierId: e.target.value })}>
                <option value="">Fournisseur</option>
                {suppliers.map((s) => <option key={s.id} value={s.id}>{s.nom}</option>)}
              </select>
              <input placeholder="Réf. fournisseur" value={newSupplier.referenceFournisseur} onChange={(e) => setNewSupplier({ ...newSupplier, referenceFournisseur: e.target.value })} />
              <input placeholder="Prix négocié" type="number" step="0.01" value={newSupplier.prixNegocie} onChange={(e) => setNewSupplier({ ...newSupplier, prixNegocie: e.target.value })} />
            </div>
            <label className="flex items-center gap-2 mt-3 text-sm">
              <input type="checkbox" checked={newSupplier.principal} onChange={(e) => setNewSupplier({ ...newSupplier, principal: e.target.checked })} />
              Fournisseur principal
            </label>
            <Button className="mt-3" onClick={handleAddSupplier} disabled={submitting}>Ajouter</Button>
          </Card>
        </div>
      )}

      {tab === 'prices' && !isNew && (
        <div className="space-y-4">
          <Card className="p-5">
            <h3 className="text-sm font-medium mb-3">Modifier un prix</h3>
            <div className="flex gap-3 items-end">
              <div>
                <label className="block text-xs text-gray-500 mb-1">Type</label>
                <select value={priceForm.type} onChange={(e) => setPriceForm({ ...priceForm, type: e.target.value })}>
                  {PRICE_TYPES.map((t) => <option key={t} value={t}>{t}</option>)}
                </select>
              </div>
              <div>
                <label className="block text-xs text-gray-500 mb-1">Nouveau prix</label>
                <input type="number" step="0.01" value={priceForm.nouveauPrix} onChange={(e) => setPriceForm({ ...priceForm, nouveauPrix: e.target.value })} />
              </div>
              <Button onClick={handleUpdatePrice} disabled={submitting}>Appliquer</Button>
            </div>
          </Card>
          <Card className="overflow-hidden">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b text-left text-gray-500">
                  <th className="px-5 py-3">Date</th>
                  <th className="px-5 py-3">Type</th>
                  <th className="px-5 py-3">Ancien</th>
                  <th className="px-5 py-3">Nouveau</th>
                  <th className="px-5 py-3">Utilisateur</th>
                </tr>
              </thead>
              <tbody>
                {priceHistory.map((h) => (
                  <tr key={h.id} className="border-b border-gray-50">
                    <td className="px-5 py-3">{formatDate(h.dateModification)}</td>
                    <td className="px-5 py-3">{h.type}</td>
                    <td className="px-5 py-3">{formatPrice(h.ancienPrix)}</td>
                    <td className="px-5 py-3 font-medium">{formatPrice(h.nouveauPrix)}</td>
                    <td className="px-5 py-3">{h.utilisateur}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </Card>
        </div>
      )}

      {tab === 'media' && !isNew && (
        <div className="grid grid-cols-2 gap-6">
          <Card className="p-5">
            <h3 className="text-sm font-medium mb-3">Images</h3>
            <div className="grid grid-cols-3 gap-3 mb-4">
              {product?.images?.map((img) => (
                <div key={img.id} className="relative aspect-square bg-gray-50 rounded-lg overflow-hidden border group">
                  <img src={img.url} alt={img.fileName} className="w-full h-full object-cover" />
                  {img.principale && <span className="absolute top-1 left-1"><Badge tone="info">Principale</Badge></span>}
                  <button
                    type="button"
                    onClick={() => handleDeleteImage(img.id)}
                    className="absolute top-1 right-1 opacity-0 group-hover:opacity-100 bg-red-600 text-white text-xs px-1.5 py-0.5 rounded"
                  >
                    ×
                  </button>
                </div>
              ))}
            </div>
            <input type="file" accept="image/*" onChange={handleImageUpload} className="text-sm" />
          </Card>
          <Card className="p-5">
            <h3 className="text-sm font-medium mb-3">Documents</h3>
            <ul className="space-y-2 mb-4">
              {product?.documents?.map((doc) => (
                <li key={doc.id} className="flex justify-between items-center text-sm gap-2">
                  <a href={doc.url} target="_blank" rel="noreferrer" className="text-blue-600 hover:underline truncate">{doc.fileName}</a>
                  <div className="flex items-center gap-2 shrink-0">
                    <Badge>{doc.type}</Badge>
                    <Button variant="ghost" className="text-xs text-red-600 px-1" onClick={() => handleDeleteDocument(doc.id)}>
                      Suppr.
                    </Button>
                  </div>
                </li>
              ))}
            </ul>
            {DOCUMENT_TYPES.map((type) => (
              <label key={type} className="block text-xs text-gray-500 mb-2">
                {type.replace(/_/g, ' ')}
                <input type="file" className="mt-1 text-sm" onChange={(e) => handleDocUpload(e, type)} />
              </label>
            ))}
          </Card>
        </div>
      )}

      {tab === 'attributes' && (
        <Card className="p-6 space-y-3">
          {attrDefs.length === 0 ? (
            <p className="text-sm text-gray-400">Aucun attribut défini. Créez-en dans la section Attributs.</p>
          ) : (
            attrDefs.map((def) => (
              <div key={def.id}>
                <label className="block text-xs font-medium text-gray-500 mb-1">{def.label}</label>
                <input
                  value={form.attributs[def.code] || ''}
                  onChange={(e) => setForm({ ...form, attributs: { ...form.attributs, [def.code]: e.target.value } })}
                  className="w-full"
                />
              </div>
            ))
          )}
        </Card>
      )}

      {tab === 'lifecycle' && !isNew && (
        <Card className="p-5">
          <div className="flex items-end gap-4">
            <div>
              <label className="block text-xs text-gray-500 mb-1">État du cycle de vie</label>
              <select value={lifecycleForm.cycleVie} onChange={(e) => setLifecycleForm({ ...lifecycleForm, cycleVie: e.target.value })} className="min-w-48">
                {LIFECYCLE_STATUS.map((s) => <option key={s} value={s}>{lifecycleLabel[s]}</option>)}
              </select>
            </div>
            <Button onClick={handleUpdateLifecycle} disabled={submitting}>Mettre à jour</Button>
          </div>
          <div className="mt-6 flex items-center gap-2 text-xs text-gray-400">
            {LIFECYCLE_STATUS.map((s, i) => (
              <span key={s} className="flex items-center gap-2">
                {i > 0 && '→'}
                <span className={lifecycleForm.cycleVie === s ? 'text-gray-900 font-medium' : ''}>{lifecycleLabel[s]}</span>
              </span>
            ))}
          </div>
        </Card>
      )}

      {tab === 'audit' && !isNew && (
        <Card className="overflow-hidden">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b text-left text-gray-500">
                <th className="px-5 py-3">Date</th>
                <th className="px-5 py-3">Action</th>
                <th className="px-5 py-3">Détails</th>
                <th className="px-5 py-3">Utilisateur</th>
              </tr>
            </thead>
            <tbody>
              {audit.map((a) => (
                <tr key={a.id} className="border-b border-gray-50">
                  <td className="px-5 py-3">{formatDate(a.dateAction)}</td>
                  <td className="px-5 py-3"><Badge>{a.action}</Badge></td>
                  <td className="px-5 py-3 text-gray-600">{a.details}</td>
                  <td className="px-5 py-3">{a.utilisateur}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </Card>
      )}
    </>
  )
}
