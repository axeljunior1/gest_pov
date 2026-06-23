import SearchableEntityPicker from './SearchableEntityPicker'

/**
 * Sélecteur métier prêt à l'emploi — délègue à SearchableEntityPicker avec le type d'entité.
 */
export default function SmartEntitySelector({
  entityType,
  ...props
}) {
  if (!entityType) {
    throw new Error('SmartEntitySelector requires entityType')
  }
  return <SearchableEntityPicker entityType={entityType} {...props} />
}

export { SearchableEntityPicker }
