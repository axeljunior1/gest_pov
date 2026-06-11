import api from './client'

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
