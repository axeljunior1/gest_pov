/**
 * Vérifie l'accès permission (utilisable hors React / tests).
 * @param {{ permission?: string, anyOf?: string[], hasPermission: (c: string) => boolean, hasAnyPermission: (...c: string[]) => boolean, isSuperAdmin?: boolean }} ctx
 */
export function resolvePermissionAccess(ctx) {
  const {
    permission,
    anyOf = [],
    hasPermission,
    hasAnyPermission,
    isSuperAdmin = false,
  } = ctx

  if (isSuperAdmin) return { allowed: true, requiredLabel: null }

  if (anyOf.length > 0) {
    return {
      allowed: hasAnyPermission(...anyOf),
      requiredLabel: anyOf.join(' | '),
    }
  }

  return {
    allowed: !permission || hasPermission(permission),
    requiredLabel: permission || null,
  }
}
