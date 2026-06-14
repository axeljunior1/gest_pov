import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { notificationsApi } from '../api'
import { useAuth } from '../context/AuthContext'
import { getErrorMessage } from '../utils/errors'

const statusLabel = {
  SENT: 'Non lue',
  READ: 'Lue',
  PENDING: 'En attente',
  FAILED: 'Échec',
}

export default function NotificationBell() {
  const { hasPermission } = useAuth()
  const [open, setOpen] = useState(false)
  const [count, setCount] = useState(0)
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(false)
  const panelRef = useRef(null)

  const canRead = hasPermission('alerts.read')

  const refresh = async () => {
    if (!canRead) return
    setLoading(true)
    try {
      const [list, unread] = await Promise.all([
        notificationsApi.list(),
        notificationsApi.unreadCount(),
      ])
      setItems(list.slice(0, 15))
      setCount(unread.count ?? 0)
    } catch {
      /* silencieux en barre */
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (!canRead) return undefined
    refresh()
    const timer = setInterval(refresh, 60000)
    return () => clearInterval(timer)
  }, [canRead])

  useEffect(() => {
    if (!open) return undefined
    const onClick = (e) => {
      if (panelRef.current && !panelRef.current.contains(e.target)) {
        setOpen(false)
      }
    }
    document.addEventListener('mousedown', onClick)
    return () => document.removeEventListener('mousedown', onClick)
  }, [open])

  if (!canRead) return null

  const markRead = async (id) => {
    try {
      await notificationsApi.markRead(id)
      await refresh()
    } catch (e) {
      console.error(getErrorMessage(e))
    }
  }

  return (
    <div className="relative" ref={panelRef}>
      <button
        type="button"
        onClick={() => { setOpen((v) => !v); if (!open) refresh() }}
        className="relative p-2 rounded-lg hover:bg-gray-100 text-gray-600"
        title="Notifications"
        aria-label="Notifications"
      >
        <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.8}>
          <path strokeLinecap="round" strokeLinejoin="round" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6 6 0 10-12 0v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
        </svg>
        {count > 0 && (
          <span className="absolute -top-0.5 -right-0.5 min-w-[1.1rem] h-[1.1rem] px-1 rounded-full bg-red-500 text-white text-[10px] font-bold flex items-center justify-center">
            {count > 99 ? '99+' : count}
          </span>
        )}
      </button>

      {open && (
        <div className="absolute right-0 mt-2 w-80 max-h-96 overflow-y-auto bg-white border border-gray-200 rounded-xl shadow-lg z-50">
          <div className="px-4 py-3 border-b flex items-center justify-between">
            <p className="text-sm font-semibold">Notifications</p>
            <Link to="/alerts" className="text-xs text-emerald-600 hover:underline" onClick={() => setOpen(false)}>
              Voir tout
            </Link>
          </div>
          {loading ? (
            <p className="p-4 text-sm text-gray-400">Chargement…</p>
          ) : items.length === 0 ? (
            <p className="p-4 text-sm text-gray-400">Aucune notification</p>
          ) : (
            <ul className="divide-y">
              {items.map((n) => (
                <li key={n.id} className="px-4 py-3 hover:bg-gray-50">
                  <p className="text-xs text-gray-400">{n.alertType}</p>
                  <p className="text-sm mt-0.5">{n.alertMessage}</p>
                  <div className="flex items-center justify-between mt-2">
                    <span className="text-[10px] text-gray-400">{statusLabel[n.status] || n.status}</span>
                    {n.status === 'SENT' && (
                      <button type="button" className="text-xs text-emerald-600 hover:underline" onClick={() => markRead(n.id)}>
                        Marquer lue
                      </button>
                    )}
                  </div>
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </div>
  )
}
