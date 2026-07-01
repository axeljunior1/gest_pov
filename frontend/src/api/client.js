import axios from 'axios'
import { markSessionExpired } from '../utils/errors'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE ?? '/api',
  headers: { 'Content-Type': 'application/json' },
  timeout: 60_000,
})

api.interceptors.request.use((config) => {
  const url = config.url ?? ''
  const isPublicAuth = url.includes('/auth/login') || url.startsWith('/license/')
  if (!isPublicAuth) {
    const token = localStorage.getItem('erp_auth_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
  }
  if (config.data instanceof FormData) {
    delete config.headers['Content-Type']
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 && !error.config?.url?.includes('/auth/login')) {
      markSessionExpired()
      localStorage.removeItem('erp_auth_token')
      localStorage.removeItem('erp_auth_user')
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  },
)

export default api
