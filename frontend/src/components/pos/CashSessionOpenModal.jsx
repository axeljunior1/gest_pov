import { useState } from 'react'
import { formatPosMoney } from '../../utils/posMoney'

import ModalOverlay from '../ui/ModalOverlay'

export default function CashSessionOpenModal({ currency, onClose, onConfirm, loading }) {
  const [amount, setAmount] = useState('100')

  const submit = (e) => {
    e.preventDefault()
    const value = Number(amount)
    if (!Number.isFinite(value) || value < 0) return
    onConfirm(value)
  }

  return (
    <ModalOverlay open onClose={loading ? undefined : onClose}>
      <form onSubmit={submit} className="bg-slate-900 border border-slate-700 rounded-2xl w-full max-w-md p-6">
        <h2 className="text-lg font-semibold">Ouverture de caisse</h2>
        <p className="text-sm text-slate-400 mt-2">
          Saisissez le fond de caisse (monnaie disponible au démarrage).
        </p>
        <label className="block mt-6">
          <span className="text-xs text-slate-400 uppercase tracking-wide">Montant initial de caisse</span>
          <input
            type="number"
            step="0.01"
            min="0"
            required
            autoFocus
            className="mt-2 w-full rounded-xl px-4 py-3 text-xl font-semibold border border-slate-600 bg-slate-800"
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
          />
          {amount !== '' && Number.isFinite(Number(amount)) && (
            <span className="text-xs text-slate-500 mt-1 block">
              {formatPosMoney(Number(amount), currency)}
            </span>
          )}
        </label>
        <div className="flex gap-3 justify-end mt-6">
          <button type="button" onClick={onClose} disabled={loading}
            className="px-4 py-2 rounded-lg bg-slate-700 text-sm">
            Annuler
          </button>
          <button type="submit" disabled={loading}
            className="px-5 py-2.5 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-sm font-medium disabled:opacity-50">
            {loading ? 'Ouverture…' : 'Ouvrir la caisse'}
          </button>
        </div>
      </form>
    </ModalOverlay>
  )
}
