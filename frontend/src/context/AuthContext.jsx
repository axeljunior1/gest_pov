import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import api from '../api/client'

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

  const value = useMemo(() => ({
    token,
    user,
    loading,
    isAuthenticated: !!token && !!user,
    login,
    logout,
    userEmail: user?.email ?? null,
  }), [token, user, loading, login, logout])

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
