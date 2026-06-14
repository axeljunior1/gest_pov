/** Détection scan code-barres POS (chiffres uniquement, longueur minimale configurable). */
export function looksLikeBarcode(term, minLength = 6) {
  if (!term) return false
  const trimmed = term.trim()
  return /^\d+$/.test(trimmed) && trimmed.length >= minLength
}

export function barcodeMinLengthFromContext(ctx) {
  const fromConfig = ctx?.barcodeScanConfig?.minLength
  if (typeof fromConfig === 'number' && fromConfig > 0) return fromConfig
  const fromSettings = ctx?.publicSettings?.['pos.barcode_min_length']
  const parsed = parseInt(fromSettings, 10)
  return Number.isFinite(parsed) && parsed > 0 ? parsed : 6
}

export function isBarcodeScanEnabled(ctx) {
  if (ctx?.barcodeScanConfig?.scanEnabled != null) {
    return ctx.barcodeScanConfig.scanEnabled !== false
  }
  const v = ctx?.publicSettings?.['pos.barcode_scan_enabled']
  return v == null || v === 'true' || v === true
}

export function isBarcodeAutoAddEnabled(ctx) {
  if (ctx?.barcodeScanConfig?.autoAddToCart != null) {
    return ctx.barcodeScanConfig.autoAddToCart !== false
  }
  const v = ctx?.publicSettings?.['pos.barcode_auto_add_to_cart']
  return v == null || v === 'true' || v === true
}
