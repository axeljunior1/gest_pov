import { formatPosMoney } from '../../utils/posMoney'
import PosSaleLineLabel from './PosSaleLineLabel'

/** Ligne panier POS : libellé, quantité, prix unitaire, remise, total. */
export default function PosCartLineRow({
  line,
  currency,
  onDecrement,
  onIncrement,
  onQtyBlur,
  onRemove,
}) {
  const hasDiscount = Number(line.discountAmount) > 0

  return (
    <li className={`p-3 text-sm ${line.stockInsufficient ? 'bg-red-950/30 border-l-2 border-red-500' : ''}`}>
      <div className="flex justify-between gap-2 items-start">
        <PosSaleLineLabel line={line} variant="cart" className="flex-1 min-w-0" />
        <span className="shrink-0 font-semibold tabular-nums text-emerald-300">
          {formatPosMoney(line.lineTotal, currency)}
        </span>
      </div>
      {line.stockInsufficient && (
        <p className="text-xs text-red-400 mt-1" role="alert">
          Stock insuffisant — {Number(line.quantityInput)} demandé(s), {Number(line.stockAvailable ?? 0)} disponible(s)
        </p>
      )}
      <div className="flex flex-wrap items-center gap-x-3 gap-y-1 mt-2 text-xs text-slate-400">
        <span className="tabular-nums">
          {formatPosMoney(line.unitPrice, currency)} / unité
        </span>
        {hasDiscount && (
          <span className="text-amber-400 tabular-nums">
            Remise −{formatPosMoney(line.discountAmount, currency)}
          </span>
        )}
      </div>
      <div className="flex items-center gap-2 mt-2 text-slate-400">
        <button
          type="button"
          className="w-7 h-7 bg-slate-800 rounded hover:bg-slate-700"
          aria-label="Diminuer quantité"
          onClick={onDecrement}
        >
          −
        </button>
        <input
          type="text"
          inputMode="decimal"
          aria-label={`Quantité ${line.productNom}`}
          key={`${line.id}-${line.quantityInput}`}
          defaultValue={line.quantityInput}
          className="w-14 h-7 text-center bg-slate-800 border border-slate-600 rounded text-sm text-white tabular-nums focus:border-emerald-500 focus:outline-none"
          onBlur={onQtyBlur}
          onKeyDown={(e) => {
            if (e.key === 'Enter') {
              e.preventDefault()
              e.currentTarget.blur()
            }
          }}
        />
        <button
          type="button"
          className="w-7 h-7 bg-slate-800 rounded hover:bg-slate-700"
          aria-label="Augmenter quantité"
          onClick={onIncrement}
        >
          +
        </button>
        <button
          type="button"
          className="text-red-400 text-xs hover:text-red-300 px-1 ml-auto"
          onClick={onRemove}
        >
          Retirer
        </button>
      </div>
    </li>
  )
}
