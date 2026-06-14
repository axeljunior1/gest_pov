import { formatPosMoney } from '../../utils/posMoney'

export function resolvePosVariantId(product, variantId) {
  if (variantId) return variantId
  if (product?.matchedVariantId) return product.matchedVariantId
  if (product?.variants?.length === 1) return product.variants[0].id
  return null
}

export function expandPosSearchResults(results, matchType) {
  if (!results?.length) return []
  if (matchType !== 'TEXT') {
    return results.map((p) => ({
      key: `${p.id}`,
      product: p,
      variantId: resolvePosVariantId(p),
      label: p.nom,
      unitPrice: p.unitPrice,
      stockAvailable: p.stockAvailable,
      outOfStock: p.outOfStock,
      lowStock: p.lowStock,
    }))
  }
  const rows = []
  for (const p of results) {
    if (p.hasVariants && p.variants?.length > 0) {
      for (const v of p.variants) {
        rows.push({
          key: `${p.id}-${v.id}`,
          product: p,
          variantId: v.id,
          label: `${p.nom} — ${v.label || v.sku}`,
          unitPrice: v.unitPrice,
          stockAvailable: v.stockAvailable,
          outOfStock: v.outOfStock,
          lowStock: v.lowStock,
        })
      }
    } else {
      rows.push({
        key: `${p.id}`,
        product: p,
        variantId: resolvePosVariantId(p),
        label: p.nom,
        unitPrice: p.unitPrice,
        stockAvailable: p.stockAvailable,
        outOfStock: p.outOfStock,
        lowStock: p.lowStock,
      })
    }
  }
  return rows
}

export default function PosSearchResults({ results, matchType, highlightIndex, currency, onPick, message }) {
  if (results === null) return null

  const rows = expandPosSearchResults(results, matchType)

  if (rows.length === 0) {
    const text = matchType === 'BARCODE_NOT_FOUND'
      ? (message || 'Aucun produit trouvé pour ce code-barres')
      : matchType === 'BARCODE_AMBIGUOUS'
        ? (message || 'Plusieurs articles correspondent — contactez un administrateur')
        : 'Aucun produit trouvé'
    return <p className="text-red-400 text-sm mt-2 px-1 font-medium">{text}</p>
  }

  const hint = matchType === 'TEXT'
    ? `${rows.length} résultat(s) — cliquez ou Entrée pour ajouter`
    : null

  return (
    <div className="mt-2 bg-slate-800 rounded-xl border border-slate-700 shadow-lg overflow-hidden">
      {hint && (
        <p className="text-xs text-slate-400 px-4 py-2 border-b border-slate-700">{hint}</p>
      )}
      <ul className="max-h-56 overflow-auto">
        {rows.map((row, index) => (
          <li key={row.key}>
            <button
              type="button"
              onClick={() => onPick(row)}
              className={`w-full text-left px-4 py-3 text-sm flex items-center justify-between gap-3 ${
                index === highlightIndex ? 'bg-emerald-600/30 ring-1 ring-inset ring-emerald-500' : 'hover:bg-slate-700'
              }`}
            >
              <span className="min-w-0">
                <span className="font-medium block truncate">{row.label}</span>
                <span className="text-xs text-slate-400">{row.product.sku}{row.product.categoryNom ? ` · ${row.product.categoryNom}` : ''}</span>
              </span>
              <span className="shrink-0 text-right">
                <span className="text-emerald-400 font-medium">{formatPosMoney(row.unitPrice ?? row.product.unitPrice, currency)}</span>
                {row.outOfStock
                  ? <span className="block text-[10px] text-red-400">Rupture</span>
                  : row.lowStock
                    ? <span className="block text-[10px] text-orange-400">Stock faible ({Number(row.stockAvailable).toFixed(0)})</span>
                    : <span className="block text-[10px] text-slate-500">Stock {Number(row.stockAvailable ?? row.product.stockAvailable).toFixed(0)}</span>}
              </span>
            </button>
          </li>
        ))}
      </ul>
    </div>
  )
}
