export function saleHasStockIssues(sale) {
  if (!sale?.lignes?.length) return false
  if (sale.hasStockIssues === true) return true
  return sale.lignes.some((l) => l.stockInsufficient)
}

export function getSaleStockIssueLines(sale) {
  if (!sale?.lignes?.length) return []
  return sale.lignes.filter((l) => l.stockInsufficient)
}

import { formatPosSaleLineLabel } from './posSaleLine'

export function formatStockIssueLine(line) {
  const label = formatPosSaleLineLabel(line)
  const qty = Number(line.quantityInput)
  const available = line.stockAvailable != null ? Number(line.stockAvailable) : null
  if (available != null) {
    return `${label} : ${qty} en panier, ${available} disponible(s)`
  }
  return `${label} : quantité en panier (${qty}) supérieure au stock`
}
