import {
  productsApi, customersApi, suppliersApi, usersApi, categoriesApi, brandsApi, posApi,
} from '../api'
import { filterEntities } from '../utils/entitySearchMatch'

let supplierCache = null
let userCache = null

export async function searchEntities(entityType, query, options = {}) {
  const q = query?.trim()
  if (!q) return []

  switch (entityType) {
    case 'product':
      if (options.pos) {
        const data = await posApi.search(q, {
          warehouseId: options.warehouseId,
          categoryId: options.categoryId,
        })
        return data.products ?? []
      }
      return productsApi.search({
        query: q,
        statut: options.statut,
        ...options.searchParams,
      })

    case 'customer':
      return customersApi.search(q)

    case 'supplier': {
      if (!supplierCache || options.refreshCache) {
        supplierCache = await suppliersApi.getAll()
      }
      return filterEntities(supplierCache, q, 'supplier')
    }

    case 'user': {
      if (!userCache || options.refreshCache) {
        userCache = await usersApi.list()
      }
      return filterEntities(userCache, q, 'user')
    }

    case 'category': {
      const apiResults = await categoriesApi.search(q)
      if (apiResults?.length) return apiResults
      const tree = options.categoryTree ?? await categoriesApi.getTree()
      return filterEntities(flattenCategories(tree), q, 'category')
    }

    case 'brand': {
      const apiResults = await brandsApi.search(q)
      if (apiResults?.length) return apiResults
      const all = await brandsApi.getAll()
      return filterEntities(all, q, 'brand')
    }

    default:
      return []
  }
}

function flattenCategories(nodes, parentNom = null) {
  const out = []
  for (const node of nodes ?? []) {
    out.push({ ...node, parentNom: node.parentNom ?? parentNom })
    out.push(...flattenCategories(node.children, node.nom))
  }
  return out
}

export function searchEntitiesLocal(entityType, query, options = []) {
  const q = query?.trim()
  if (!q) return options
  return filterEntities(options, q, entityType)
}

export function invalidateEntitySearchCache(entityType) {
  if (entityType === 'supplier') supplierCache = null
  if (entityType === 'user') userCache = null
}
