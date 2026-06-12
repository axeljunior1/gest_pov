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
