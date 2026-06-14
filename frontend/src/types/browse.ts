/** Types partagés — consultation ventes / retours */

export interface BrowseFilters {
  q: string
  status: string
  dateFrom: string
  dateTo: string
  page: number
  saleId?: string
  [key: string]: string | number | undefined
}

export interface BrowsePageResponse<T> {
  items: T[]
  totalElements: number
  page: number
  size: number
  totalPages: number
}

export interface SaleBrowseParams {
  q?: string
  status?: string
  dateFrom?: string
  dateTo?: string
  page?: number
  limit?: number
}

export interface ReturnBrowseParams extends SaleBrowseParams {
  saleId?: number
}
