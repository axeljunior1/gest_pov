import api from './client'

export const authApi = {
  login: (email, password) => api.post('/auth/login', { email, password }).then(r => r.data),
  me: () => api.get('/auth/me').then(r => r.data),
}

export const productsApi = {
  search: (params) => api.get('/products', { params }).then(r => r.data),
  getById: (id) => api.get(`/products/${id}`).then(r => r.data),
  create: (data) => api.post('/products', data).then(r => r.data),
  update: (id, data) => api.put(`/products/${id}`, data).then(r => r.data),
  delete: (id) => api.delete(`/products/${id}`),
  updatePrice: (id, data) => api.patch(`/products/${id}/price`, data).then(r => r.data),
  updateLifecycle: (id, data) => api.patch(`/products/${id}/lifecycle`, data).then(r => r.data),
  moveCategory: (id, data) => api.patch(`/products/${id}/category`, data).then(r => r.data),
  getPriceHistory: (id) => api.get(`/products/${id}/price-history`).then(r => r.data),
  getAudit: (id) => api.get(`/products/${id}/audit`).then(r => r.data),
  addVariant: (id, data) => api.post(`/products/${id}/variants`, data).then(r => r.data),
  deleteVariant: (id, variantId) => api.delete(`/products/${id}/variants/${variantId}`),
  addSupplier: (id, data) => api.post(`/products/${id}/suppliers`, data).then(r => r.data),
  removeSupplier: (id, psId) => api.delete(`/products/${id}/suppliers/${psId}`),
  uploadImage: (id, file, principale = false) => {
    const form = new FormData()
    form.append('file', file)
    form.append('principale', principale)
    return api.post(`/products/${id}/images`, form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }).then(r => r.data)
  },
  uploadDocument: (id, file, type) => {
    const form = new FormData()
    form.append('file', file)
    form.append('type', type)
    return api.post(`/products/${id}/documents`, form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }).then(r => r.data)
  },
  deleteImage: (id, imageId) => api.delete(`/products/${id}/images/${imageId}`),
  deleteDocument: (id, documentId) => api.delete(`/products/${id}/documents/${documentId}`),
  getPackagings: (id) => api.get(`/products/${id}/packagings`).then(r => r.data),
  addPackaging: (id, data) => api.post(`/products/${id}/packagings`, data).then(r => r.data),
  updatePackaging: (id, packagingId, data) => api.put(`/products/${id}/packagings/${packagingId}`, data).then(r => r.data),
  deletePackaging: (id, packagingId) => api.delete(`/products/${id}/packagings/${packagingId}`),
  convertPackagingToBase: (id, data) => api.post(`/products/${id}/packagings/convert-to-base`, data).then(r => r.data),
}

export const categoriesApi = {
  getTree: () => api.get('/categories').then(r => r.data),
  search: (nom) => api.get('/categories/search', { params: { nom } }).then(r => r.data),
  create: (data) => api.post('/categories', data).then(r => r.data),
  update: (id, data) => api.put(`/categories/${id}`, data).then(r => r.data),
  delete: (id) => api.delete(`/categories/${id}`),
}

export const suppliersApi = {
  getAll: () => api.get('/suppliers').then(r => r.data),
  search: (nom) => api.get('/suppliers/search', { params: { nom } }).then(r => r.data),
  create: (data) => api.post('/suppliers', data).then(r => r.data),
  update: (id, data) => api.put(`/suppliers/${id}`, data).then(r => r.data),
  delete: (id) => api.delete(`/suppliers/${id}`),
}

export const unitsApi = {
  getAll: () => api.get('/units').then(r => r.data),
  create: (data) => api.post('/units', data).then(r => r.data),
  delete: (id) => api.delete(`/units/${id}`),
  getConversions: () => api.get('/units/conversions').then(r => r.data),
  createConversion: (data) => api.post('/units/conversions', data).then(r => r.data),
  deleteConversion: (id) => api.delete(`/units/conversions/${id}`),
  convert: (params) => api.get('/units/convert', { params }).then(r => r.data),
}

export const attributesApi = {
  getAll: () => api.get('/attributes').then(r => r.data),
  create: (data) => api.post('/attributes', data).then(r => r.data),
  delete: (id) => api.delete(`/attributes/${id}`),
}

export const barcodesApi = {
  generate: (data) => api.post('/barcodes/generate', data).then(r => r.data),
}

export const stockApi = {
  getItems: (params) => api.get('/stock/items', { params }).then(r => r.data),
  getAvailable: (params) => api.get('/stock/available', { params }).then(r => r.data),
  getMovements: (params) => api.get('/stock/movements', { params }).then(r => r.data),
  receipt: (data) => api.post('/stock/receipt', data).then(r => r.data),
  issue: (data) => api.post('/stock/issue', data).then(r => r.data),
  adjust: (data) => api.post('/stock/adjust', data).then(r => r.data),
  getWarehouses: () => api.get('/warehouses').then(r => r.data),
  createWarehouse: (data) => api.post('/warehouses', data).then(r => r.data),
  getLocations: (warehouseId) => api.get(`/warehouses/${warehouseId}/locations`).then(r => r.data),
  getTransfers: () => api.get('/stock/transfers').then(r => r.data),
  getReservations: () => api.get('/stock/reservations').then(r => r.data),
}

export const stockMovementsApi = {
  list: (params) => api.get('/stock/movements', { params }).then(r => r.data),
  getById: (id) => api.get(`/stock/movements/${id}`).then(r => r.data),
  export: (params) => api.get('/stock/movements/export', {
    params,
    responseType: 'blob',
  }).then(r => r.data),
}

export const inventoryCountsApi = {
  list: (params) => api.get('/stock/inventories', { params }).then(r => r.data),
  getById: (id) => api.get(`/stock/inventories/${id}`).then(r => r.data),
  create: (data) => api.post('/stock/inventories', data).then(r => r.data),
  update: (id, data) => api.put(`/stock/inventories/${id}`, data).then(r => r.data),
  start: (id) => api.post(`/stock/inventories/${id}/start`, {}).then(r => r.data),
  validate: (id) => api.post(`/stock/inventories/${id}/validate`, {}).then(r => r.data),
  cancel: (id) => api.post(`/stock/inventories/${id}/cancel`, {}).then(r => r.data),
  deleteLine: (inventoryId, lineId) => api.delete(`/stock/inventories/${inventoryId}/lines/${lineId}`),
  delete: (id) => api.delete(`/stock/inventories/${id}`),
}

export const stockEntriesApi = {
  list: (params) => api.get('/stock/entries', { params }).then(r => r.data),
  getById: (id) => api.get(`/stock/entries/${id}`).then(r => r.data),
  create: (data) => api.post('/stock/entries', data).then(r => r.data),
  update: (id, data) => api.put(`/stock/entries/${id}`, data).then(r => r.data),
  validate: (id) =>
    api.post(`/stock/entries/${id}/validate`, {}).then(r => r.data),
  cancel: (id) =>
    api.post(`/stock/entries/${id}/cancel`, {}).then(r => r.data),
  deleteLine: (entryId, lineId) => api.delete(`/stock/entries/${entryId}/lines/${lineId}`),
  delete: (id) => api.delete(`/stock/entries/${id}`),
}

export const stockExitsApi = {
  list: (params) => api.get('/stock/exits', { params }).then(r => r.data),
  getById: (id) => api.get(`/stock/exits/${id}`).then(r => r.data),
  create: (data) => api.post('/stock/exits', data).then(r => r.data),
  update: (id, data) => api.put(`/stock/exits/${id}`, data).then(r => r.data),
  validate: (id) =>
    api.post(`/stock/exits/${id}/validate`, {}).then(r => r.data),
  cancel: (id) =>
    api.post(`/stock/exits/${id}/cancel`, {}).then(r => r.data),
  deleteLine: (exitId, lineId) => api.delete(`/stock/exits/${exitId}/lines/${lineId}`),
  delete: (id) => api.delete(`/stock/exits/${id}`),
}

export const dashboardApi = {
  summary: () => api.get('/dashboard/summary').then(r => r.data),
  alerts: () => api.get('/dashboard/alerts').then(r => r.data),
  recentMovements: (params) => api.get('/dashboard/movements/recent', { params }).then(r => r.data),
  recentEntries: (params) => api.get('/dashboard/entries/recent', { params }).then(r => r.data),
  recentExits: (params) => api.get('/dashboard/exits/recent', { params }).then(r => r.data),
  topMovedProducts: (params) => api.get('/dashboard/products/top-moved', { params }).then(r => r.data),
  warehouses: () => api.get('/dashboard/warehouses').then(r => r.data),
}

export const analyticsApi = {
  overview: (params) => api.get('/analytics/overview', { params }).then(r => r.data),
  timeline: (params) => api.get('/analytics/sales/timeline', { params }).then(r => r.data),
  topProducts: (params) => api.get('/analytics/products/top', { params }).then(r => r.data),
  categories: (params) => api.get('/analytics/categories', { params }).then(r => r.data),
  payments: (params) => api.get('/analytics/payments', { params }).then(r => r.data),
  cashiers: (params) => api.get('/analytics/cashiers', { params }).then(r => r.data),
  customers: (params) => api.get('/analytics/customers', { params }).then(r => r.data),
  stockAlerts: (params) => api.get('/analytics/stock-alerts', { params }).then(r => r.data),
  businessAlerts: (params) => api.get('/analytics/business-alerts', { params }).then(r => r.data),
  exportCsv: (params) => api.get('/analytics/export', { params, responseType: 'blob' }).then(r => r.data),
}

export const alertsApi = {
  list: (params) => api.get('/alerts', { params }).then(r => r.data),
  getById: (id) => api.get(`/alerts/${id}`).then(r => r.data),
  acknowledge: (id) =>
    api.post(`/alerts/${id}/acknowledge`, {}).then(r => r.data),
  resolve: (id) =>
    api.post(`/alerts/${id}/resolve`, {}).then(r => r.data),
  ignore: (id) =>
    api.post(`/alerts/${id}/ignore`, {}).then(r => r.data),
}

export const notificationsApi = {
  list: () => api.get('/notifications').then(r => r.data),
  unreadCount: () =>
    api.get('/notifications/unread-count').then(r => r.data),
  markRead: (id) => api.post(`/notifications/${id}/read`).then(r => r.data),
}

export const usersApi = {
  list: () => api.get('/users').then(r => r.data),
  getById: (id) => api.get(`/users/${id}`).then(r => r.data),
  create: (data) => api.post('/users', data).then(r => r.data),
  update: (id, data) => api.put(`/users/${id}`, data).then(r => r.data),
  delete: (id) => api.delete(`/users/${id}`),
}

export const rolesApi = {
  list: () => api.get('/roles').then(r => r.data),
  getById: (id) => api.get(`/roles/${id}`).then(r => r.data),
  listPermissions: () => api.get('/roles/permissions').then(r => r.data),
  updatePermissions: (id, permissions) =>
    api.put(`/roles/${id}/permissions`, { permissions }).then(r => r.data),
}

function downloadBlob(data, filename, mime) {
  const blob = data instanceof Blob ? data : new Blob([data], { type: mime })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}

export const exportApi = {
  products: (format = 'CSV') =>
    api.get('/export/products', { params: { format }, responseType: 'blob' })
      .then(r => { downloadBlob(r.data, `products.${format === 'XLSX' ? 'xlsx' : 'csv'}`, r.headers['content-type']); return r.data }),
  stock: (format = 'CSV', params = {}) =>
    api.get('/export/stock', { params: { format, ...params }, responseType: 'blob' })
      .then(r => { downloadBlob(r.data, `stock.${format === 'XLSX' ? 'xlsx' : 'csv'}`, r.headers['content-type']); return r.data }),
  movements: (format = 'CSV', params = {}) =>
    api.get('/export/movements', { params: { format, ...params }, responseType: 'blob' })
      .then(r => { downloadBlob(r.data, `movements.${format === 'XLSX' ? 'xlsx' : 'csv'}`, r.headers['content-type']); return r.data }),
  entries: (format = 'CSV', params = {}) =>
    api.get('/export/entries', { params: { format, ...params }, responseType: 'blob' })
      .then(r => { downloadBlob(r.data, `entries.${format === 'XLSX' ? 'xlsx' : 'csv'}`, r.headers['content-type']); return r.data }),
  exits: (format = 'CSV', params = {}) =>
    api.get('/export/exits', { params: { format, ...params }, responseType: 'blob' })
      .then(r => { downloadBlob(r.data, `exits.${format === 'XLSX' ? 'xlsx' : 'csv'}`, r.headers['content-type']); return r.data }),
  alerts: (format = 'CSV', params = {}) =>
    api.get('/export/alerts', { params: { format, ...params }, responseType: 'blob' })
      .then(r => { downloadBlob(r.data, `alerts.${format === 'XLSX' ? 'xlsx' : 'csv'}`, r.headers['content-type']); return r.data }),
  inventories: (format = 'CSV', params = {}) =>
    api.get('/export/inventories', { params: { format, ...params }, responseType: 'blob' })
      .then(r => { downloadBlob(r.data, `inventories.${format === 'XLSX' ? 'xlsx' : 'csv'}`, r.headers['content-type']); return r.data }),
}

export const importApi = {
  downloadTemplate: (type, format = 'CSV') =>
    api.get(`/import/templates/${type}`, { params: { format }, responseType: 'blob' })
      .then(r => { downloadBlob(r.data, `template-${type}.${format === 'XLSX' ? 'xlsx' : 'csv'}`, r.headers['content-type']); return r.data }),
  previewProducts: (file, duplicateMode = 'REJECT') => {
    const form = new FormData()
    form.append('file', file)
    return api.post(`/import/products/preview?duplicateMode=${duplicateMode}`, form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }).then(r => r.data)
  },
  validateProducts: (file, duplicateMode = 'REJECT') => {
    const form = new FormData()
    form.append('file', file)
    return api.post(`/import/products/validate?duplicateMode=${duplicateMode}`, form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }).then(r => r.data)
  },
  previewInitialStock: (file) => {
    const form = new FormData()
    form.append('file', file)
    return api.post('/import/initial-stock/preview', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }).then(r => r.data)
  },
  validateInitialStock: (file) => {
    const form = new FormData()
    form.append('file', file)
    return api.post('/import/initial-stock/validate', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }).then(r => r.data)
  },
  previewPackagings: (file) => {
    const form = new FormData()
    form.append('file', file)
    return api.post('/import/packagings/preview', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }).then(r => r.data)
  },
  validatePackagings: (file) => {
    const form = new FormData()
    form.append('file', file)
    return api.post('/import/packagings/validate', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }).then(r => r.data)
  },
  history: () => api.get('/import/history').then(r => r.data),
}

export const settingsApi = {
  getPublic: () => api.get('/settings/public').then(r => r.data),
  getAll: () => api.get('/settings').then(r => r.data),
  update: (key, value) =>
    api.put(`/settings/${encodeURIComponent(key)}`, { value }).then(r => r.data),
  updateBulk: (settings) =>
    api.put('/settings', { settings }).then(r => r.data),
}

export const posApi = {
  context: () => api.get('/pos/context').then(r => r.data),
  openSession: (data) => api.post('/pos/sessions/open', data).then(r => r.data),
  currentSession: () => api.get('/pos/sessions/current').then(r => r.data),
  closeSession: (data) => api.post('/pos/sessions/close', data).then(r => r.data),
  sessionReport: (id) => api.get(`/pos/sessions/${id}/report`).then(r => r.data),
  catalog: (params) => api.get('/pos/catalog', { params }).then(r => r.data),
  search: (q, params = {}) => api.get('/pos/catalog/search', { params: { q, ...params } }).then(r => r.data),
  createSale: () => api.post('/pos/sales').then(r => r.data),
  getSale: (id) => api.get(`/pos/sales/${id}`).then(r => r.data),
  addLine: (id, data) => api.post(`/pos/sales/${id}/lines`, data).then(r => r.data),
  updateLine: (id, lineId, quantity) => api.put(`/pos/sales/${id}/lines/${lineId}`, { quantity }).then(r => r.data),
  lineDiscount: (id, lineId, discountAmount) =>
    api.put(`/pos/sales/${id}/lines/${lineId}/discount`, { discountAmount }).then(r => r.data),
  holdSale: (id, label) => api.post(`/pos/sales/${id}/hold`, { label }).then(r => r.data),
  resumeSale: (id) => api.post(`/pos/sales/${id}/resume`).then(r => r.data),
  listHold: () => api.get('/pos/sales/hold').then(r => r.data),
  deleteHold: (id) => api.delete(`/pos/sales/${id}`),
  validateSale: (id, data) => api.post(`/pos/sales/${id}/validate`, data).then(r => r.data),
  submitForPayment: (id) => api.post(`/pos/sales/${id}/send-to-payment`).then(r => r.data),
  sendToPayment: (id) => api.post(`/pos/sales/${id}/send-to-payment`).then(r => r.data),
  listPendingPayments: () => api.get('/pos/sales/pending-payment').then(r => r.data),
  cancelSale: (id) => api.post(`/pos/sales/${id}/cancel`).then(r => r.data),
  ticket: (id) => api.get(`/pos/sales/${id}/ticket`).then(r => r.data),
  refund: (id, data) => api.post(`/pos/sales/${id}/refund`, data).then(r => r.data),
  searchCustomers: (q) => api.get('/pos/customers/search', { params: { q } }).then(r => r.data),
  quickCreateCustomer: (data) => api.post('/pos/customers/quick', data).then(r => r.data),
  assignCustomer: (saleId, customerId) => api.put(`/pos/sales/${saleId}/customer`, { customerId }).then(r => r.data),
  removeCustomer: (saleId) => api.delete(`/pos/sales/${saleId}/customer`).then(r => r.data),
  redeemLoyalty: (saleId, points) => api.post(`/pos/sales/${saleId}/loyalty/redeem`, { points }).then(r => r.data),
  clearLoyaltyRedemption: (saleId) => api.delete(`/pos/sales/${saleId}/loyalty/redeem`).then(r => r.data),
}

export const customersApi = {
  list: () => api.get('/customers').then(r => r.data),
  search: (q) => api.get('/customers/search', { params: { q } }).then(r => r.data),
  get: (id) => api.get(`/customers/${id}`).then(r => r.data),
  history: (id) => api.get(`/customers/${id}/history`).then(r => r.data),
  create: (data) => api.post('/customers', data).then(r => r.data),
  update: (id, data) => api.put(`/customers/${id}`, data).then(r => r.data),
  delete: (id) => api.delete(`/customers/${id}`),
  transactions: (id) => api.get(`/customers/${id}/loyalty/transactions`).then(r => r.data),
  adjustPoints: (id, data) => api.post(`/customers/${id}/loyalty/adjust`, data).then(r => r.data),
}
