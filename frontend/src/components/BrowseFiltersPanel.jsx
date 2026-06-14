import { Button } from './ui'

/**
 * Panneau filtres partagé ventes / retours.
 */
export default function BrowseFiltersPanel({
  draft,
  updateDraft,
  onApply,
  onReset,
  statusOptions,
  extraFields,
  exportButton,
}) {
  return (
    <div className="flex flex-wrap gap-3 items-end">
      <div className="flex-1 min-w-[180px]">
        <label className="text-xs text-gray-500 block mb-1">Recherche</label>
        <input
          type="search"
          value={draft.q}
          onChange={(e) => updateDraft({ q: e.target.value })}
          onKeyDown={(e) => e.key === 'Enter' && onApply()}
          placeholder="N° vente, client…"
          className="w-full rounded-lg border px-3 py-2 text-sm"
        />
      </div>
      {extraFields}
      <div>
        <label className="text-xs text-gray-500 block mb-1">Statut</label>
        <select
          value={draft.status}
          onChange={(e) => updateDraft({ status: e.target.value })}
          className="rounded-lg border px-3 py-2 text-sm"
        >
          {statusOptions.map((o) => (
            <option key={o.value || 'all'} value={o.value}>{o.label}</option>
          ))}
        </select>
      </div>
      <div>
        <label className="text-xs text-gray-500 block mb-1">Du</label>
        <input
          type="date"
          value={draft.dateFrom}
          onChange={(e) => updateDraft({ dateFrom: e.target.value })}
          className="rounded-lg border px-3 py-2 text-sm"
        />
      </div>
      <div>
        <label className="text-xs text-gray-500 block mb-1">Au</label>
        <input
          type="date"
          value={draft.dateTo}
          onChange={(e) => updateDraft({ dateTo: e.target.value })}
          className="rounded-lg border px-3 py-2 text-sm"
        />
      </div>
      <Button type="button" onClick={onApply}>Filtrer</Button>
      {onReset && (
        <Button type="button" variant="secondary" onClick={onReset}>Réinitialiser</Button>
      )}
      {exportButton}
    </div>
  )
}
