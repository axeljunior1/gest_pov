import { getEntitySearchConfig } from '../../config/entitySearchConfig'
import SearchCriteriaHelp from './SearchCriteriaHelp'

const VARIANT_INPUT = {
  default: '',
  pos: 'border border-slate-600 bg-slate-900 text-slate-100',
}

/**
 * Champ de recherche (listes / filtres) avec critères visibles.
 */
export default function EntitySearchField({
  entityType,
  value,
  onChange,
  onSubmit,
  placeholder,
  disabled = false,
  className = '',
  inputClassName = '',
  variant = 'default',
  showCriteriaHelp = true,
  criteriaHelpMode = 'badges',
  showSearchButton = false,
  searchButtonLabel = 'Rechercher',
  id,
  autoFocus = false,
}) {
  const config = getEntitySearchConfig(entityType)
  const baseInput = VARIANT_INPUT[variant] ?? VARIANT_INPUT.default

  return (
    <div className={`space-y-1.5 ${className}`}>
      <div className={showSearchButton ? 'flex gap-2' : undefined}>
        <input
          id={id}
          type="search"
          value={value}
          onChange={(e) => onChange?.(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter') onSubmit?.(value)
          }}
          placeholder={placeholder ?? config.placeholder}
          disabled={disabled}
          autoFocus={autoFocus}
          autoComplete="off"
          aria-describedby={showCriteriaHelp ? `${id ?? entityType}-search-help` : undefined}
          className={`flex-1 w-full ${baseInput} ${inputClassName}`}
        />
        {showSearchButton && (
          <button
            type="button"
            onClick={() => onSubmit?.(value)}
            disabled={disabled}
            className="shrink-0 px-4 py-2 text-sm rounded-lg border border-gray-200 bg-white hover:bg-gray-50 text-gray-700"
          >
            {searchButtonLabel}
          </button>
        )}
      </div>
      {showCriteriaHelp && (
        <SearchCriteriaHelp
          entityType={entityType}
          variant={variant}
          mode={criteriaHelpMode}
          id={`${id ?? entityType}-search-help`}
        />
      )}
    </div>
  )
}
