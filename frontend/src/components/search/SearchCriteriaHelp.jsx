import { getEntitySearchConfig } from '../../config/entitySearchConfig'

const VARIANT_STYLES = {
  default: {
    wrap: 'text-xs text-gray-500',
    badge: 'inline-flex items-center rounded-md bg-gray-100 text-gray-600 px-2 py-0.5 font-medium',
    prefix: 'Recherche possible par :',
    inline: 'text-gray-400',
  },
  pos: {
    wrap: 'text-[11px] text-slate-400',
    badge: 'inline-flex items-center rounded-md bg-slate-800 text-slate-300 px-2 py-0.5 border border-slate-700',
    prefix: 'Recherche par :',
    inline: 'text-slate-500',
  },
  compact: {
    wrap: 'text-[11px] text-gray-500',
    badge: 'inline-flex items-center rounded bg-gray-50 text-gray-600 px-1.5 py-px border border-gray-200',
    prefix: null,
    inline: 'text-gray-400',
  },
}

/**
 * Aide visuelle : critères de recherche (badges + texte court).
 */
export default function SearchCriteriaHelp({
  entityType,
  variant = 'default',
  mode = 'badges',
  className = '',
}) {
  const config = getEntitySearchConfig(entityType)
  const styles = VARIANT_STYLES[variant] ?? VARIANT_STYLES.default

  if (!config.criteria?.length) return null

  if (mode === 'inline') {
    return (
      <p className={`${styles.wrap} ${className}`}>
        <span className={styles.inline}>Vous pouvez rechercher par : </span>
        {config.criteria.join(' • ')}
      </p>
    )
  }

  return (
    <div className={`${styles.wrap} space-y-1.5 ${className}`} role="note">
      {styles.prefix && <p>{styles.prefix}</p>}
      <div className="flex flex-wrap gap-1.5">
        {config.criteria.map((criterion) => (
          <span key={criterion} className={styles.badge}>
            {criterion}
          </span>
        ))}
      </div>
    </div>
  )
}

export function SearchMatchHint({ match, variant = 'default', className = '' }) {
  if (!match?.label || match.value == null || match.value === '') return null
  const isPos = variant === 'pos'
  return (
    <p className={`text-xs mt-0.5 ${isPos ? 'text-emerald-400/90' : 'text-emerald-700'} ${className}`}>
      <span className={isPos ? 'text-slate-500' : 'text-gray-500'}>{match.label} correspondant : </span>
      <span className="font-medium">{match.value}</span>
    </p>
  )
}
