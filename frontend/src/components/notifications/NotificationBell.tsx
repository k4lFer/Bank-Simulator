import { useState, useRef, useEffect } from 'react'
import { useNotifications } from '../../hooks/useNotifications'

const typeLabels: Record<string, string> = {
  DEPOSIT: 'Depósito',
  CARD_DEPOSIT: 'Depósito con tarjeta',
  TRANSFER_SENT: 'Transferencia enviada',
  TRANSFER_RECEIVED: 'Transferencia recibida',
  TRANSFER_REJECTED: 'Transferencia rechazada',
}

const typeColors: Record<string, string> = {
  DEPOSIT: 'text-emerald-600',
  CARD_DEPOSIT: 'text-emerald-600',
  TRANSFER_SENT: 'text-blue-600',
  TRANSFER_RECEIVED: 'text-purple-600',
  TRANSFER_REJECTED: 'text-red-600',
}

export default function NotificationBell() {
  const { notifications, unreadCount, connected, markAsRead, markAllAsRead } = useNotifications()
  const [open, setOpen] = useState(false)
  const ref = useRef<HTMLDivElement>(null)

  useEffect(() => {
    function handleClick(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) {
        setOpen(false)
      }
    }
    document.addEventListener('mousedown', handleClick)
    return () => document.removeEventListener('mousedown', handleClick)
  }, [])

  return (
    <div ref={ref} className="relative">
      <button
        onClick={() => setOpen(!open)}
        className="relative p-2 rounded-lg hover:bg-blue-500 transition-colors"
        title="Notificaciones"
      >
        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
            d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
        </svg>
        {unreadCount > 0 && (
          <span className="absolute -top-0.5 -right-0.5 bg-red-500 text-white text-[10px] font-bold rounded-full w-4 h-4 flex items-center justify-center">
            {unreadCount > 9 ? '9+' : unreadCount}
          </span>
        )}
      </button>
      {!connected && (
        <span className="absolute -bottom-1 -right-1 w-2 h-2 bg-gray-400 rounded-full" title="Desconectado" />
      )}
      {connected && (
        <span className="absolute -bottom-1 -right-1 w-2 h-2 bg-emerald-400 rounded-full" title="Conectado" />
      )}

      {open && (
        <div className="absolute right-0 mt-2 w-80 bg-white rounded-xl shadow-lg border border-gray-100 z-50 max-h-96 overflow-y-auto">
          <div className="sticky top-0 bg-white border-b border-gray-100 px-4 py-3 flex items-center justify-between rounded-t-xl">
            <span className="text-sm font-semibold text-gray-800">Notificaciones</span>
            {unreadCount > 0 && (
              <button onClick={markAllAsRead} className="text-xs text-blue-600 hover:text-blue-800">
                Marcar todo leído
              </button>
            )}
          </div>
          {notifications.length === 0 ? (
            <p className="text-center text-gray-400 text-sm py-8">Sin notificaciones</p>
          ) : (
            notifications.slice(0, 20).map((n) => (
              <button
                key={n.id}
                onClick={() => { markAsRead(n.id) }}
                className={`w-full text-left px-4 py-3 border-b border-gray-50 hover:bg-gray-50 transition-colors ${!n.read ? 'bg-blue-50/50' : ''}`}
              >
                <div className="flex items-start gap-2">
                  <div className="flex-1 min-w-0">
                    <p className={`text-xs font-medium ${typeColors[n.type] ?? 'text-gray-600'}`}>
                      {typeLabels[n.type] ?? n.type}
                    </p>
                    <p className="text-sm text-gray-800 truncate">{n.title}</p>
                    <p className="text-xs text-gray-500 line-clamp-2">{n.message}</p>
                    <p className="text-[10px] text-gray-400 mt-1">
                      {new Date(n.createdAt).toLocaleString()}
                    </p>
                  </div>
                  {!n.read && <span className="w-2 h-2 bg-blue-500 rounded-full mt-1.5 shrink-0" />}
                </div>
              </button>
            ))
          )}
        </div>
      )}
    </div>
  )
}
