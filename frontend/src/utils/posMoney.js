export function formatPosMoney(value, currency = 'EUR') {
  if (value == null || value === '') return '—'
  try {
    return new Intl.NumberFormat('fr-FR', { style: 'currency', currency }).format(Number(value))
  } catch {
    return `${Number(value).toFixed(2)} ${currency}`
  }
}

export function formatPosDateTime(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleString('fr-FR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export function differenceSeverityStyle(severity) {
  switch (severity) {
    case 'BALANCED':
      return { bg: 'bg-emerald-950/60 border-emerald-700', text: 'text-emerald-300', label: 'Caisse équilibrée' }
    case 'MINOR':
      return { bg: 'bg-amber-950/60 border-amber-700', text: 'text-amber-300', label: 'Écart modéré' }
    case 'MAJOR':
      return { bg: 'bg-red-950/60 border-red-700', text: 'text-red-300', label: 'Écart important' }
    default:
      return { bg: 'bg-slate-800 border-slate-600', text: 'text-slate-300', label: 'Écart' }
  }
}
