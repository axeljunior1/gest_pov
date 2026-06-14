import { useCallback, useEffect, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { useNotification } from '../context/NotificationContext'
import { getErrorMessage } from '../utils/errors'
import { buildBrowseDateParams } from '../utils/saleDisplay'

export const DEFAULT_BROWSE_PAGE_SIZE = 25

/** @typedef {import('../types/browse').BrowseFilters} BrowseFilters */
/** @typedef {import('../types/browse').BrowsePageResponse} BrowsePageResponse */

/**
 * Hook partagé ventes/retours : filtres appliqués au clic « Filtrer », pagination URL.
 * @param {{ fetchFn: (params: object) => Promise<BrowsePageResponse>, extraParamKeys?: string[], pageSize?: number }} options
 */
export function useSalesBrowseFilters({
  fetchFn,
  extraParamKeys = [],
  pageSize = DEFAULT_BROWSE_PAGE_SIZE,
}) {
  const notify = useNotification()
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
  }, [searchParams, extraParamKeys])

  const [draft, setDraft] = useState(readFromUrl)
  const [applied, setApplied] = useState(readFromUrl)

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
  }, [extraParamKeys, pageSize])

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
  }, [extraParamKeys, setSearchParams])

  const loadPage = useCallback(async (filters) => {
    setLoading(true)
    try {
      const data = await fetchFn(buildApiParams(filters, filters.page))
      setPageData({
        items: data.items ?? [],
        totalElements: data.totalElements ?? 0,
        page: data.page ?? filters.page,
        size: data.size ?? pageSize,
        totalPages: data.totalPages ?? 0,
      })
    } catch (e) {
      notify.error(getErrorMessage(e))
    } finally {
      setLoading(false)
    }
  }, [buildApiParams, fetchFn, notify, pageSize])

  useEffect(() => {
    loadPage(applied)
  }, [applied, loadPage])

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
  }, [extraParamKeys, setSearchParams])

  const goToPage = useCallback((page) => {
    const next = { ...applied, page }
    setApplied(next)
    syncUrl(next)
  }, [applied, syncUrl])

  const updateDraft = useCallback((patch) => {
    setDraft((prev) => ({ ...prev, ...patch }))
  }, [])

  const runExport = useCallback(async (exportFn) => {
    if (!exportFn) return
    setExporting(true)
    try {
      await exportFn(buildApiParams({ ...applied, page: 0 }, 0))
      notify.success('Export téléchargé')
    } catch (e) {
      notify.error(getErrorMessage(e))
    } finally {
      setExporting(false)
    }
  }, [applied, buildApiParams, notify])

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
