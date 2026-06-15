import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { useNotification } from '../context/NotificationContext'
import { getErrorMessage } from '../utils/errors'
import { buildBrowseDateParams } from '../utils/saleDisplay'

export const DEFAULT_BROWSE_PAGE_SIZE = 25

/** Référence stable pour les pages sans paramètre URL additionnel. */
export const EMPTY_BROWSE_EXTRA_KEYS = []

/** Clés URL additionnelles — page retours. */
export const RETURN_BROWSE_EXTRA_KEYS = ['saleId']

/** @typedef {import('../types/browse').BrowseFilters} BrowseFilters */
/** @typedef {import('../types/browse').BrowsePageResponse} BrowsePageResponse */

/**
 * Hook partagé ventes/retours : filtres appliqués au clic « Filtrer », pagination URL.
 * @param {{ fetchFn: (params: object) => Promise<BrowsePageResponse>, extraParamKeys?: string[], pageSize?: number }} options
 */
export function useSalesBrowseFilters({
  fetchFn,
  extraParamKeys = EMPTY_BROWSE_EXTRA_KEYS,
  pageSize = DEFAULT_BROWSE_PAGE_SIZE,
}) {
  const notify = useNotification()
  const fetchFnRef = useRef(fetchFn)
  const notifyRef = useRef(notify)
  fetchFnRef.current = fetchFn
  notifyRef.current = notify

  const [searchParams, setSearchParams] = useSearchParams()
  const [loading, setLoading] = useState(true)
  const [exporting, setExporting] = useState(false)
  const [pageData, setPageData] = useState({
    items: [],
    totalElements: 0,
    page: 0,
    size: pageSize,
    totalPages: 0,
  })

  /** Chaîne stable — évite les re-fetch si le parent passe un nouveau tableau à contenu identique. */
  const extraKeysKey = extraParamKeys.join('\0')

  const readFromUrl = useCallback(() => {
    /** @type {BrowseFilters} */
    const draft = {
      q: searchParams.get('q') || '',
      status: searchParams.get('status') || '',
      dateFrom: searchParams.get('dateFrom') || '',
      dateTo: searchParams.get('dateTo') || '',
      page: Number(searchParams.get('page') || 0),
    }
    for (const key of extraParamKeys) {
      draft[key] = searchParams.get(key) || ''
    }
    return draft
  }, [searchParams, extraKeysKey]) // eslint-disable-line react-hooks/exhaustive-deps

  const [draft, setDraft] = useState(() => readFromUrl())
  const [applied, setApplied] = useState(() => readFromUrl())

  const buildApiParams = useCallback((filters, page) => {
    const params = {
      page,
      limit: pageSize,
      ...buildBrowseDateParams(filters.dateFrom, filters.dateTo),
    }
    if (filters.q?.trim()) params.q = filters.q.trim()
    if (filters.status) params.status = filters.status
    for (const key of extraParamKeys) {
      const value = filters[key]
      if (value != null && String(value).trim() !== '') {
        params[key] = key === 'saleId' ? Number(value) : value
      }
    }
    return params
  }, [extraKeysKey, pageSize]) // eslint-disable-line react-hooks/exhaustive-deps

  const syncUrl = useCallback((filters) => {
    const params = {}
    if (filters.q?.trim()) params.q = filters.q.trim()
    if (filters.status) params.status = filters.status
    if (filters.dateFrom) params.dateFrom = filters.dateFrom
    if (filters.dateTo) params.dateTo = filters.dateTo
    if (filters.page > 0) params.page = String(filters.page)
    for (const key of extraParamKeys) {
      if (filters[key]) params[key] = filters[key]
    }
    setSearchParams(params, { replace: true })
  }, [extraKeysKey, setSearchParams]) // eslint-disable-line react-hooks/exhaustive-deps

  const loadPage = useCallback(async (filters, signal) => {
    setLoading(true)
    try {
      const data = await fetchFnRef.current(buildApiParams(filters, filters.page))
      if (signal?.aborted) return
      setPageData({
        items: data.items ?? [],
        totalElements: data.totalElements ?? 0,
        page: data.page ?? filters.page,
        size: data.size ?? pageSize,
        totalPages: data.totalPages ?? 0,
      })
    } catch (e) {
      if (signal?.aborted) return
      notifyRef.current.error(getErrorMessage(e))
    } finally {
      if (!signal?.aborted) setLoading(false)
    }
  }, [buildApiParams, pageSize])

  const appliedKey = useMemo(
    () => JSON.stringify(applied),
    [applied],
  )

  useEffect(() => {
    const controller = new AbortController()
    loadPage(applied, controller.signal)
    return () => controller.abort()
  }, [appliedKey, loadPage]) // appliedKey remplace applied (objet recréé)

  const applyFilters = useCallback(() => {
    const next = { ...draft, page: 0 }
    setApplied(next)
    syncUrl(next)
  }, [draft, syncUrl])

  const resetFilters = useCallback(() => {
    const empty = { q: '', status: '', dateFrom: '', dateTo: '', page: 0 }
    for (const key of extraParamKeys) empty[key] = ''
    setDraft(empty)
    setApplied(empty)
    setSearchParams({}, { replace: true })
  }, [extraKeysKey, setSearchParams]) // eslint-disable-line react-hooks/exhaustive-deps

  const goToPage = useCallback((page) => {
    setApplied((prev) => {
      const next = { ...prev, page }
      syncUrl(next)
      return next
    })
  }, [syncUrl])

  const updateDraft = useCallback((patch) => {
    setDraft((prev) => ({ ...prev, ...patch }))
  }, [])

  const runExport = useCallback(async (exportFn) => {
    if (!exportFn) return
    setExporting(true)
    try {
      await exportFn(buildApiParams({ ...applied, page: 0 }, 0))
      notifyRef.current.success('Export téléchargé')
    } catch (e) {
      notifyRef.current.error(getErrorMessage(e))
    } finally {
      setExporting(false)
    }
  }, [applied, buildApiParams])

  const pagination = useMemo(() => ({
    page: pageData.page,
    size: pageData.size,
    totalElements: pageData.totalElements,
    totalPages: pageData.totalPages,
    hasPrev: pageData.page > 0,
    hasNext: pageData.page + 1 < pageData.totalPages,
  }), [pageData])

  return {
    items: pageData.items,
    loading,
    exporting,
    draft,
    applied,
    updateDraft,
    applyFilters,
    resetFilters,
    goToPage,
    pagination,
    runExport,
  }
}

/** @deprecated Utiliser useSalesBrowseFilters */
export const useBrowseList = useSalesBrowseFilters
