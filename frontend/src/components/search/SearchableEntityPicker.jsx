import { useCallback, useEffect, useId, useRef, useState } from 'react'
import { getEntitySearchConfig } from '../../config/entitySearchConfig'
import { findEntityMatch } from '../../utils/entitySearchMatch'
import SearchCriteriaHelp, { SearchMatchHint } from './SearchCriteriaHelp'

const VARIANT = {
  default: {
    input: '',
    dropdown: 'absolute z-50 mt-1 w-full bg-white border border-gray-200 rounded-lg shadow-lg max-h-56 overflow-auto',
    option: 'w-full text-left px-3 py-2.5 text-sm hover:bg-gray-50 border-b border-gray-50 last:border-0',
    optionActive: 'bg-gray-100',
    selected: 'text-sm text-gray-700 truncate',
    clear: 'text-gray-400 hover:text-gray-700 text-xs shrink-0',
  },
  pos: {
    input: 'border border-slate-600',
    dropdown: 'absolute z-50 mt-1 w-full bg-slate-800 border border-slate-700 rounded-lg shadow-lg max-h-40 overflow-auto',
    option: 'w-full text-left px-3 py-2 text-sm hover:bg-slate-700 border-b border-slate-700/80 last:border-0',
    optionActive: 'bg-slate-700/80',
    selected: 'text-sm text-slate-200 truncate',
    clear: 'text-slate-500 hover:text-white text-xs shrink-0',
  },
  compact: {
    input: 'text-sm py-1.5',
    dropdown: 'absolute z-50 mt-1 w-full bg-white border border-gray-200 rounded-lg shadow-md max-h-48 overflow-auto',
    option: 'w-full text-left px-2.5 py-2 text-sm hover:bg-gray-50',
    optionActive: 'bg-gray-100',
    selected: 'text-sm truncate',
    clear: 'text-gray-400 hover:text-gray-700 text-xs',
  },
}

function defaultGetLabel(entity, entityType) {
  switch (entityType) {
    case 'product':
      return entity.nom ?? entity.label
    case 'customer':
      return entity.fullName ?? `${entity.firstName ?? ''} ${entity.lastName ?? ''}`.trim()
    case 'supplier':
      return entity.nom
    case 'user':
      return `${entity.firstName ?? ''} ${entity.lastName ?? ''}`.trim() || entity.email
    case 'category':
      return entity.parentNom ? `${entity.nom} (${entity.parentNom})` : entity.nom
    case 'brand':
      return entity.nom
    default:
      return entity.nom ?? entity.name ?? String(entity.id ?? '')
  }
}

function defaultGetValue(entity) {
  return entity?.id != null ? String(entity.id) : ''
}

/**
 * Sélecteur avec recherche, critères visibles et surlignage du champ correspondant.
 */
export default function SearchableEntityPicker({
  entityType,
  value,
  onChange,
  onSearch,
  options: staticOptions,
  getOptionLabel = defaultGetLabel,
  getOptionValue = defaultGetValue,
  placeholder,
  disabled = false,
  className = '',
  inputClassName = '',
  variant = 'default',
  showCriteriaHelp = true,
  criteriaHelpMode = 'badges',
  minChars = 0,
  debounceMs = 280,
  clearable = true,
  emptyMessage = 'Aucun résultat',
  id: idProp,
  'aria-label': ariaLabel,
}) {
  const autoId = useId()
  const inputId = idProp ?? `entity-picker-${entityType}-${autoId}`
  const config = getEntitySearchConfig(entityType)
  const styles = VARIANT[variant] ?? VARIANT.default

  const [query, setQuery] = useState('')
  const [results, setResults] = useState(null)
  const [open, setOpen] = useState(false)
  const [loading, setLoading] = useState(false)
  const [selectedLabel, setSelectedLabel] = useState('')
  const [highlight, setHighlight] = useState(0)
  const timerRef = useRef(null)
  const wrapRef = useRef(null)

  const resolveSearch = useCallback(async (q) => {
    if (staticOptions) {
      const { searchEntitiesLocal } = await import('../../services/entitySearchService')
      return searchEntitiesLocal(entityType, q, staticOptions)
    }
    if (onSearch) return onSearch(q)
    const { searchEntities } = await import('../../services/entitySearchService')
    return searchEntities(entityType, q)
  }, [entityType, staticOptions, onSearch])

  useEffect(() => {
    if (!value) {
      setSelectedLabel('')
      return
    }
    if (staticOptions?.length) {
      const hit = staticOptions.find((o) => getOptionValue(o) === String(value))
      if (hit) setSelectedLabel(getOptionLabel(hit, entityType))
    }
  }, [value, staticOptions, getOptionLabel, getOptionValue, entityType])

  useEffect(() => {
    clearTimeout(timerRef.current)
    if (!open) return undefined

    const q = query.trim()
    if (q.length < minChars) {
      setResults(staticOptions ?? [])
      return undefined
    }

    timerRef.current = setTimeout(async () => {
      setLoading(true)
      try {
        const data = await resolveSearch(q)
        setResults(data)
        setHighlight(0)
      } catch {
        setResults([])
      } finally {
        setLoading(false)
      }
    }, debounceMs)

    return () => clearTimeout(timerRef.current)
  }, [query, open, minChars, debounceMs, resolveSearch, staticOptions])

  useEffect(() => {
    const onDocClick = (e) => {
      if (wrapRef.current && !wrapRef.current.contains(e.target)) {
        setOpen(false)
      }
    }
    document.addEventListener('mousedown', onDocClick)
    return () => document.removeEventListener('mousedown', onDocClick)
  }, [])

  const pick = (entity) => {
    const id = getOptionValue(entity)
    setSelectedLabel(getOptionLabel(entity, entityType))
    setQuery('')
    setResults(null)
    setOpen(false)
    onChange?.(id, entity)
  }

  const clear = () => {
    setSelectedLabel('')
    setQuery('')
    setResults(null)
    onChange?.('', null)
  }

  const displayResults = results ?? (open && !query.trim() && staticOptions ? staticOptions : null)

  return (
    <div ref={wrapRef} className={`relative ${className}`}>
      {value && selectedLabel && !open ? (
        <div className={`flex items-center gap-2 rounded-lg border border-gray-200 px-3 py-2 ${variant === 'pos' ? 'bg-slate-900 border-slate-700' : 'bg-gray-50'}`}>
          <span className={`flex-1 min-w-0 ${styles.selected}`}>{selectedLabel}</span>
          {clearable && !disabled && (
            <button type="button" onClick={clear} className={styles.clear} aria-label="Effacer la sélection">
              Effacer
            </button>
          )}
        </div>
      ) : (
        <>
          <input
            id={inputId}
            type="search"
            value={query}
            onChange={(e) => {
              setQuery(e.target.value)
              setOpen(true)
            }}
            onFocus={() => setOpen(true)}
            onKeyDown={(e) => {
              if (!displayResults?.length) return
              if (e.key === 'ArrowDown') {
                e.preventDefault()
                setHighlight((i) => Math.min(i + 1, displayResults.length - 1))
              } else if (e.key === 'ArrowUp') {
                e.preventDefault()
                setHighlight((i) => Math.max(i - 1, 0))
              } else if (e.key === 'Enter' && displayResults[highlight]) {
                e.preventDefault()
                pick(displayResults[highlight])
              } else if (e.key === 'Escape') {
                setOpen(false)
              }
            }}
            placeholder={placeholder ?? config.placeholder}
            disabled={disabled}
            aria-label={ariaLabel ?? `Rechercher un ${config.label}`}
            aria-expanded={open}
            aria-controls={`${inputId}-listbox`}
            autoComplete="off"
            className={`w-full ${styles.input} ${inputClassName}`}
          />
          {showCriteriaHelp && (
            <SearchCriteriaHelp
              entityType={entityType}
              variant={variant}
              mode={criteriaHelpMode}
              className="mt-1.5"
            />
          )}
        </>
      )}

      {open && !value && (
        <div id={`${inputId}-listbox`} role="listbox" className={styles.dropdown}>
          {loading && (
            <p className="px-3 py-2 text-xs text-gray-400">Recherche…</p>
          )}
          {!loading && displayResults?.length === 0 && query.trim() && (
            <p className="px-3 py-2 text-xs text-gray-400">{emptyMessage}</p>
          )}
          {!loading && displayResults?.map((entity, index) => {
            const label = getOptionLabel(entity, entityType)
            const match = query.trim() ? findEntityMatch(query, entity, entityType) : null
            return (
              <button
                key={getOptionValue(entity) || index}
                type="button"
                role="option"
                aria-selected={index === highlight}
                onMouseEnter={() => setHighlight(index)}
                onClick={() => pick(entity)}
                className={`${styles.option} ${index === highlight ? styles.optionActive : ''}`}
              >
                <span className="font-medium block truncate">{label}</span>
                <SearchMatchHint match={match} variant={variant} />
              </button>
            )
          })}
        </div>
      )}
    </div>
  )
}
