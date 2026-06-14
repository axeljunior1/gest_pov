import ModalOverlay from '../ui/ModalOverlay'
import { isCashierRecallHold } from '../../utils/saleStatus'
import { formatPosMoney } from '../../utils/posMoney'

export default function ResumeSalesModal({
  open,
  onClose,
  holdSales,
  draftSales,
  currency,
  isCentralCashier,
  onResumeHold,
  onPickDraft,
}) {
  const total = (holdSales?.length ?? 0) + (draftSales?.length ?? 0)

  return (
    <ModalOverlay open={open} onClose={onClose}>
      <div className="bg-slate-900 border border-slate-600 rounded-xl p-4 w-full max-w-md shadow-xl">
        <div className="flex items-center justify-between mb-3">
          <h3 className="font-semibold text-base">
            Ventes en attente
            {total > 0 && (
              <span className="ml-2 inline-flex items-center justify-center min-w-[1.5rem] h-6 px-1.5 rounded-full bg-amber-500 text-slate-900 text-xs font-bold">
                {total}
              </span>
            )}
          </h3>
          <button
            type="button"
            onClick={onClose}
            className="text-slate-400 hover:text-white text-xl leading-none px-2"
            aria-label="Fermer"
          >
            ×
          </button>
        </div>

        {total === 0 && (
          <p className="text-sm text-slate-400 py-6 text-center">Aucune vente en attente</p>
        )}

        {holdSales?.length > 0 && (
          <div className="mb-3">
            <p className="text-xs text-slate-400 mb-2 uppercase tracking-wide">
              En pause / retour caisse ({holdSales.length})
            </p>
            <ul className="space-y-1 max-h-48 overflow-auto">
              {holdSales.map((h) => (
                <li key={h.id}>
                  <button
                    type="button"
                    onClick={() => onResumeHold(h)}
                    className={`w-full text-left text-sm py-2 px-2 rounded-lg hover:bg-slate-800 ${
                      isCashierRecallHold(h) ? 'text-amber-300' : 'text-slate-200'
                    }`}
                  >
                    {isCashierRecallHold(h) && (
                      <span className="inline-block mr-1 text-[10px] uppercase tracking-wide text-amber-400 font-semibold">
                        Retour caisse
                      </span>
                    )}
                    {h.holdLabel || h.saleNumber} — {formatPosMoney(h.total, currency)}
                  </button>
                </li>
              ))}
            </ul>
          </div>
        )}

        {draftSales?.length > 0 && (
          <div className={holdSales?.length > 0 ? 'pt-3 border-t border-slate-700' : ''}>
            <p className="text-xs text-slate-400 mb-2 uppercase tracking-wide">
              En cours de saisie ({draftSales.length})
              {isCentralCashier ? '' : ''}
            </p>
            <ul className="space-y-1 max-h-48 overflow-auto">
              {draftSales.map((d) => (
                <li key={d.id}>
                  <button
                    type="button"
                    onClick={() => onPickDraft(d)}
                    className="w-full text-left text-sm py-2 px-2 rounded-lg hover:bg-slate-800 text-slate-200 hover:text-amber-300"
                  >
                    {d.saleNumber} — {formatPosMoney(d.total, currency)}
                  </button>
                </li>
              ))}
            </ul>
          </div>
        )}

        <button
          type="button"
          onClick={onClose}
          className="mt-4 w-full py-2 rounded-lg bg-slate-700 text-sm hover:bg-slate-600"
        >
          Fermer
        </button>
      </div>
    </ModalOverlay>
  )
}
