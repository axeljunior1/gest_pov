import api from './client'
import type { BrowsePageResponse, ReturnBrowseParams, SaleBrowseParams } from '../types/browse'
import type { SaleDetail, SaleRefundSummary, SaleSummary } from '../types/sales'

function downloadBlob(data: Blob, filename: string, mime?: string) {
  const blob = data instanceof Blob ? data : new Blob([data], { type: mime })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}

export const salesBrowseApiTyped = {
  list: (params: SaleBrowseParams) =>
    api.get<BrowsePageResponse<SaleSummary>>('/sales/browse', { params }).then((r) => r.data),

  detail: (id: number | string) =>
    api.get<SaleDetail>(`/sales/${id}`).then((r) => r.data),

  listReturns: (params: ReturnBrowseParams) =>
    api.get<BrowsePageResponse<SaleRefundSummary>>('/sales/returns/browse', { params }).then((r) => r.data),

  returnDetail: (id: number | string) =>
    api.get<Record<string, unknown>>(`/sales/returns/${id}`).then((r) => r.data),

  exportSales: (params: SaleBrowseParams) =>
    api.get('/sales/browse/export', { params, responseType: 'blob' }).then((r) => {
      downloadBlob(r.data as Blob, 'ventes.csv', r.headers['content-type'])
    }),

  exportReturns: (params: ReturnBrowseParams) =>
    api.get('/sales/returns/browse/export', { params, responseType: 'blob' }).then((r) => {
      downloadBlob(r.data as Blob, 'retours.csv', r.headers['content-type'])
    }),
}
