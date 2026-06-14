import { Button } from './ui'

export default function BrowsePagination({ pagination, onPageChange, className = '' }) {
  if (!pagination || pagination.totalElements === 0) return null

  const { page, totalPages, totalElements, size, hasPrev, hasNext } = pagination
  const from = page * size + 1
  const to = Math.min((page + 1) * size, totalElements)

  return (
    <div className={`flex flex-wrap items-center justify-between gap-3 px-4 py-3 border-t bg-gray-50 text-sm ${className}`}>
      <p className="text-gray-600">
        {from}–{to} sur {totalElements} · page {page + 1}/{Math.max(totalPages, 1)}
      </p>
      <div className="flex gap-2">
        <Button
          type="button"
          variant="secondary"
          disabled={!hasPrev}
          onClick={() => onPageChange(page - 1)}
        >
          Précédent
        </Button>
        <Button
          type="button"
          variant="secondary"
          disabled={!hasNext}
          onClick={() => onPageChange(page + 1)}
        >
          Suivant
        </Button>
      </div>
    </div>
  )
}
