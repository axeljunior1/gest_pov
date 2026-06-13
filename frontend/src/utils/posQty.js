export function parseQty(value, fallback = 1) {
  if (value == null || String(value).trim() === '') return fallback
  const n = parseFloat(String(value).replace(',', '.'))
  if (!Number.isFinite(n) || n <= 0) return fallback
  return n
}

/** Ex. "5*1234567890" ou "2,5 x café" → { qty, term } */
export function parseSearchWithQty(raw) {
  const trimmed = raw?.trim() ?? ''
  const match = trimmed.match(/^(\d+(?:[.,]\d+)?)\s*[*xX×]\s*(.*)$/)
  if (!match) return { qty: null, term: trimmed }
  const qty = parseFloat(match[1].replace(',', '.'))
  if (!Number.isFinite(qty) || qty <= 0) return { qty: null, term: trimmed }
  return { qty, term: match[2].trim() }
}
