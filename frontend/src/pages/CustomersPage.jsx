import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { customersApi } from '../api'
import { useAuth } from '../context/AuthContext'
import { PageHeader, Card, Button, Loading, Alert } from '../components/ui'
import EntitySearchField from '../components/search/EntitySearchField'
import { SearchMatchHint } from '../components/search/SearchCriteriaHelp'
import { findEntityMatch } from '../utils/entitySearchMatch'
import { useAsyncAction } from '../hooks/useAsyncAction'
import { useNotification } from '../context/NotificationContext'
import { getErrorMessage } from '../utils/errors'

function formatMoney(v) {
  if (v == null) return '—'
  return new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR' }).format(Number(v))
}

export default function CustomersPage() {
  const { id } = useParams()
  const { hasPermission } = useAuth()
  const notify = useNotification()
  const { run, submitting } = useAsyncAction()
  const [customers, setCustomers] = useState([])
  const [history, setHistory] = useState(null)
  const [loading, setLoading] = useState(true)
  const [pageError, setPageError] = useState('')
  const [search, setSearch] = useState('')
  const [form, setForm] = useState({
    firstName: '', lastName: '', phone: '', email: '', companyName: '', address: '', city: '', notes: '',
  })
  const [editingId, setEditingId] = useState(null)
  const [adjustPoints, setAdjustPoints] = useState('')

  const loadList = async () => {
    setLoading(true)
    setPageError('')
    try {
      const data = search.trim()
        ? await customersApi.search(search.trim())
        : await customersApi.list()
      setCustomers(data)
    } catch (e) {
      setPageError(getErrorMessage(e))
    } finally {
      setLoading(false)
    }
  }

  const loadHistory = async (customerId) => {
    try {
      const data = await customersApi.history(customerId)
      setHistory(data)
    } catch (e) {
      notify.error(getErrorMessage(e))
    }
  }

  useEffect(() => { loadList() }, [])

  useEffect(() => {
    if (id) loadHistory(Number(id))
    else setHistory(null)
  }, [id])

  const resetForm = () => {
    setForm({ firstName: '', lastName: '', phone: '', email: '', companyName: '', address: '', city: '', notes: '' })
    setEditingId(null)
  }

  const handleSubmit = () => {
    if (!form.lastName.trim() || !form.firstName.trim()) {
      notify.error('Nom et prénom obligatoires')
      return
    }
    run(
      () => editingId ? customersApi.update(editingId, form) : customersApi.create(form),
      { successMessage: editingId ? 'Client mis à jour' : 'Client créé', onSuccess: () => { resetForm(); loadList() } },
    )
  }

  const handleEdit = (c) => {
    setEditingId(c.id)
    setForm({
      firstName: c.firstName, lastName: c.lastName, phone: c.phone || '', email: c.email || '',
      companyName: c.companyName || '', address: c.address || '', city: c.city || '', notes: c.notes || '',
    })
  }

  const handleAdjust = () => {
    const pts = Number(adjustPoints)
    if (!pts) return
    run(
      () => customersApi.adjustPoints(Number(id), { points: pts, reason: 'Ajustement manuel' }),
      {
        successMessage: 'Points ajustés',
        onSuccess: () => { setAdjustPoints(''); loadHistory(Number(id)); loadList() },
      },
    )
  }

  if (id && history) {
    return (
      <>
        <PageHeader
          title={history.fullName}
          subtitle={`${history.customerNumber} · ${history.loyaltyTier} · ${history.loyaltyPoints} points`}
          action={<Link to="/customers"><Button variant="secondary">← Liste</Button></Link>}
        />
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
          <Card className="p-4"><p className="text-xs text-gray-500">Achats</p><p className="text-xl font-semibold">{history.purchaseCount}</p></Card>
          <Card className="p-4"><p className="text-xs text-gray-500">Total acheté</p><p className="text-xl font-semibold">{formatMoney(history.totalSpent)}</p></Card>
          <Card className="p-4"><p className="text-xs text-gray-500">Panier moyen</p><p className="text-xl font-semibold">{formatMoney(history.averageBasket)}</p></Card>
          <Card className="p-4"><p className="text-xs text-gray-500">Points gagnés / utilisés</p><p className="text-xl font-semibold">{history.totalPointsEarned} / {history.totalPointsRedeemed}</p></Card>
        </div>
        {history.topProducts?.length > 0 && (
          <Card className="p-5 mb-6">
            <h3 className="text-sm font-medium mb-3">Produits les plus achetés</h3>
            <ul className="text-sm space-y-1">
              {history.topProducts.map((p) => (
                <li key={p.productId} className="flex justify-between">
                  <span>{p.productNom}</span>
                  <span className="text-gray-500">{formatMoney(p.totalAmount)}</span>
                </li>
              ))}
            </ul>
          </Card>
        )}
        {hasPermission('loyalty.manage') && (
          <Card className="p-5 mb-6">
            <h3 className="text-sm font-medium mb-3">Ajustement points (manager)</h3>
            <div className="flex gap-2 max-w-xs">
              <input type="number" placeholder="+/- points" value={adjustPoints}
                onChange={(e) => setAdjustPoints(e.target.value)} />
              <Button onClick={handleAdjust} disabled={submitting}>Appliquer</Button>
            </div>
            <p className="text-xs text-gray-500 mt-2">Valeur négative pour retirer des points</p>
          </Card>
        )}
        <Card className="p-5">
          <h3 className="text-sm font-medium mb-3">Historique fidélité</h3>
          <ul className="text-sm divide-y">
            {history.recentTransactions?.map((t) => (
              <li key={t.id} className="py-2 flex justify-between">
                <span>{t.type} {t.saleNumber ? `· ${t.saleNumber}` : ''}</span>
                <span className={t.points >= 0 ? 'text-emerald-600' : 'text-red-600'}>
                  {t.points > 0 ? '+' : ''}{t.points} → {t.balanceAfter}
                </span>
              </li>
            ))}
          </ul>
        </Card>
      </>
    )
  }

  return (
    <>
      <PageHeader title="Clients" subtitle="Gestion clients & fidélité" />
      <Alert message={pageError} onDismiss={() => setPageError('')} />

      {hasPermission('customer.create') && (
        <Card className="p-5 mb-6">
          <h3 className="text-sm font-medium mb-3">{editingId ? 'Modifier client' : 'Nouveau client'}</h3>
          <div className="grid grid-cols-2 gap-3">
            <input placeholder="Prénom *" value={form.firstName} onChange={(e) => setForm({ ...form, firstName: e.target.value })} />
            <input placeholder="Nom *" value={form.lastName} onChange={(e) => setForm({ ...form, lastName: e.target.value })} />
            <input placeholder="Téléphone" value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} />
            <input placeholder="Email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
            <input placeholder="Entreprise" value={form.companyName} onChange={(e) => setForm({ ...form, companyName: e.target.value })} />
            <input placeholder="Ville" value={form.city} onChange={(e) => setForm({ ...form, city: e.target.value })} />
          </div>
          <div className="flex gap-2 mt-3">
            <Button onClick={handleSubmit} disabled={submitting}>{editingId ? 'Enregistrer' : 'Créer'}</Button>
            {editingId && <Button variant="secondary" onClick={resetForm}>Annuler</Button>}
          </div>
        </Card>
      )}

      <Card className="p-5">
        <EntitySearchField
          entityType="customer"
          value={search}
          onChange={setSearch}
          onSubmit={loadList}
          showSearchButton
          className="mb-4"
        />
        {loading ? <Loading /> : (
          <table className="w-full text-sm">
            <thead>
              <tr className="text-left text-gray-500 border-b">
                <th className="pb-2">N°</th>
                <th className="pb-2">Nom</th>
                <th className="pb-2">Téléphone</th>
                <th className="pb-2">Points</th>
                <th className="pb-2">Niveau</th>
                <th className="pb-2" />
              </tr>
            </thead>
            <tbody>
              {customers.map((c) => (
                <tr key={c.id} className="border-b border-gray-50">
                  <td className="py-2 font-mono text-xs">{c.customerNumber}</td>
                  <td className="py-2">
                    {c.fullName}
                    {search.trim() && (
                      <SearchMatchHint match={findEntityMatch(search, c, 'customer')} className="block" />
                    )}
                  </td>
                  <td className="py-2">{c.phone || '—'}</td>
                  <td className="py-2">{c.loyaltyPoints}</td>
                  <td className="py-2">{c.loyaltyTier}</td>
                  <td className="py-2 text-right space-x-2">
                    <Link to={`/customers/${c.id}`} className="text-gray-600 hover:text-gray-900 underline text-xs">Fiche</Link>
                    {hasPermission('customer.update') && (
                      <button type="button" onClick={() => handleEdit(c)} className="text-xs text-gray-600 hover:text-gray-900">Modifier</button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </Card>
    </>
  )
}
