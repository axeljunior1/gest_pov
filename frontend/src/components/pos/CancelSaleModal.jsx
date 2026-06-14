import { useEffect, useState } from 'react'
import { saleCancellationsApi } from '../../api'
import ModalOverlay from '../ui/ModalOverlay'

export default function CancelSaleModal({ saleNumber, loading, onClose, onConfirm }) {
  const [reasons, setReasons] = useState([])
  const [reason, setReason] = useState('')
  const [comment, setComment] = useState('')

  useEffect(() => {
    saleCancellationsApi.reasons()
      .then((list) => {
        setReasons(list)
        if (list.length > 0) setReason(list[0].code)
      })
      .catch(() => {})
  }, [])

  const submit = (e) => {
    e.preventDefault()
    if (!reason) return
    onConfirm({ reason, comment: comment.trim() || undefined })
  }

  return (
    <ModalOverlay open onClose={onClose}>
      <form
        onSubmit={submit}
        className="bg-slate-900 border border-slate-700 rounded-xl w-full max-w-md p-6"
        onMouseDown={(e) => e.stopPropagation()}
      >
        <h3 className="text-lg font-semibold mb-1">Annuler la vente</h3>
        {saleNumber && <p className="text-sm text-slate-400 mb-4">{saleNumber}</p>}

        <label className="text-xs text-slate-400 block mb-1">Motif d&apos;annulation *</label>
        <select
          required
          value={reason}
          onChange={(e) => setReason(e.target.value)}
          className="w-full rounded-lg px-3 py-2 text-sm border border-slate-600 bg-slate-800 mb-4"
        >
          {reasons.map((r) => (
            <option key={r.code} value={r.code}>{r.label}</option>
          ))}
        </select>

        <label className="text-xs text-slate-400 block mb-1">Commentaire (optionnel)</label>
        <textarea
          value={comment}
          onChange={(e) => setComment(e.target.value)}
          rows={3}
          className="w-full rounded-lg px-3 py-2 text-sm border border-slate-600 bg-slate-800 mb-4 resize-none"
          placeholder="Précisions sur l'annulation…"
        />

        <div className="flex gap-2 justify-end">
          <button type="button" onClick={onClose} disabled={loading}
            className="px-4 py-2 rounded-lg bg-slate-700 text-sm disabled:opacity-50">
            Retour
          </button>
          <button type="submit" disabled={loading || !reason}
            className="px-4 py-2 rounded-lg bg-red-700 hover:bg-red-600 text-sm font-medium disabled:opacity-50">
            {loading ? 'Annulation…' : 'Confirmer l\'annulation'}
          </button>
        </div>
      </form>
    </ModalOverlay>
  )
}
