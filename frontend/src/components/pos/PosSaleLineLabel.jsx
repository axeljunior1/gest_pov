import { formatPosSaleLineParts } from '../../utils/posSaleLine'

/**
 * Affichage structuré : nom produit + variante + conditionnement.
 * variant="cart" | "compact" | "print"
 */
export default function PosSaleLineLabel({ line, variant = 'cart', className = '' }) {
  const { productNom, variantName, packagingName } = formatPosSaleLineParts(line)
  if (!productNom && !variantName) return null

  if (variant === 'compact') {
    return (
      <span className={className}>
        {productNom}
        {variantName && <span className="text-slate-400"> — {variantName}</span>}
        {packagingName && <span className="text-slate-500"> ({packagingName})</span>}
      </span>
    )
  }

  if (variant === 'print') {
    return (
      <span className={className}>
        <span className="block">{productNom}</span>
        {variantName && (
          <span className="block text-xs text-gray-600">Variante : {variantName}</span>
        )}
        {packagingName && (
          <span className="block text-xs text-gray-500">{packagingName}</span>
        )}
      </span>
    )
  }

  return (
    <span className={`min-w-0 ${className}`}>
      <span className="font-medium block truncate">{productNom}</span>
      {variantName && (
        <span className="block text-xs text-emerald-400/90 font-normal mt-0.5 truncate">
          Variante : {variantName}
        </span>
      )}
      {packagingName && (
        <span className="block text-xs text-slate-400 font-normal truncate">{packagingName}</span>
      )}
    </span>
  )
}
