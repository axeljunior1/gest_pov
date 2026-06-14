import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import api from '../api/client'
import { isSuperAdmin } from '../utils/auth'

const TOKEN_KEY = 'erp_auth_token'
const USER_KEY = 'erp_auth_user'

const AuthContext = createContext(null)

function loadStoredUser() {
  try {
    const raw = localStorage.getItem(USER_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem(TOKEN_KEY))
  const [user, setUser] = useState(loadStoredUser)
  const [loading, setLoading] = useState(!!localStorage.getItem(TOKEN_KEY))

  const persistSession = useCallback((nextToken, nextUser) => {
    if (nextToken) {
      localStorage.setItem(TOKEN_KEY, nextToken)
    } else {
      localStorage.removeItem(TOKEN_KEY)
    }
    if (nextUser) {
      localStorage.setItem(USER_KEY, JSON.stringify(nextUser))
    } else {
      localStorage.removeItem(USER_KEY)
    }
    setToken(nextToken)
    setUser(nextUser)
  }, [])

  const logout = useCallback(() => {
    persistSession(null, null)
  }, [persistSession])

  const login = useCallback(async (email, password) => {
    const { data } = await api.post('/auth/login', { email, password })
    persistSession(data.token, data.user)
    return data
  }, [persistSession])

  const refreshUser = useCallback(async () => {
    if (!localStorage.getItem(TOKEN_KEY)) {
      return null
    }
    const { data } = await api.get('/auth/me')
    persistSession(localStorage.getItem(TOKEN_KEY), data)
    return data
  }, [persistSession])

  useEffect(() => {
    if (!token) {
      setLoading(false)
      return
    }
    let cancelled = false
    setLoading(true)
    api.get('/auth/me')
      .then(({ data }) => {
        if (!cancelled) {
          persistSession(token, data)
        }
      })
      .catch(() => {
        if (!cancelled) logout()
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => { cancelled = true }
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (!token) return undefined
    const refreshOnFocus = () => {
      refreshUser().catch(() => {})
    }
    window.addEventListener('focus', refreshOnFocus)
    return () => window.removeEventListener('focus', refreshOnFocus)
  }, [token, refreshUser])

  const value = useMemo(() => {
    const permissions = user?.permissions ?? []
    const superAdmin = isSuperAdmin(user)
    const hasPermission = (code) => superAdmin || permissions.includes(code)
    const hasAnyPermission = (...codes) => superAdmin || codes.some(hasPermission)

    return {
      token,
      user,
      loading,
      isAuthenticated: !!token && !!user,
      login,
      logout,
      refreshUser,
      userEmail: user?.email ?? null,
      permissions,
      hasPermission,
      hasAnyPermission,
    }
  }, [token, user, loading, login, logout, refreshUser])

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}

export function getStoredToken() {
  return localStorage.getItem(TOKEN_KEY)
}
