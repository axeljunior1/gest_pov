import { formatPosMoney } from '../../utils/posMoney'
import PosSaleLineLabel from './PosSaleLineLabel'

/** Récapitulatif des lignes (panier / encaissement). */
export default function PosSaleLinesSummary({ lines, currency, className = '' }) {
  if (!lines?.length) return null
  return (
    <ul className={`text-sm space-y-2 mb-4 max-h-40 overflow-y-auto border border-slate-700 rounded-lg p-3 bg-slate-800/50 ${className}`}>
      {lines.map((l) => (
        <li key={l.id} className="flex justify-between gap-3 items-start">
          <div className="min-w-0 flex-1">
            <PosSaleLineLabel line={l} variant="cart" />
            <span className="text-xs text-slate-500 mt-0.5 block">× {Number(l.quantityInput)}</span>
          </div>
          <span className="shrink-0 text-slate-300 tabular-nums">{formatPosMoney(l.lineTotal, currency)}</span>
        </li>
      ))}
    </ul>
  )
}
