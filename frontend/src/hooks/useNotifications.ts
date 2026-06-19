import { useState, useEffect, useRef, useCallback } from 'react'
import { useAuth } from './useAuth'
import { notificationsApi } from '../api/notifications-api'
import type { Notification } from '../models/notification'

export function useNotifications() {
  const { token } = useAuth()
  const [notifications, setNotifications] = useState<Notification[]>([])
  const [unreadCount, setUnreadCount] = useState(0)
  const [connected, setConnected] = useState(false)
  const esRef = useRef<EventSource | null>(null)

  useEffect(() => {
    if (!token) return

    notificationsApi.list(0, 50).then((res) => {
      setNotifications(res.data.data?.results ?? [])
    }).catch(() => {})
    notificationsApi.unreadCount().then((res) => {
      setUnreadCount(res.data.data?.count ?? 0)
    }).catch(() => {})

    const es = new EventSource(`/api/notifications/stream?token=${encodeURIComponent(token)}`)
    esRef.current = es

    es.addEventListener('notification', (e) => {
      try {
        const notif: Notification = JSON.parse(e.data)
        setNotifications((prev) => [notif, ...prev])
        setUnreadCount((prev) => prev + 1)
      } catch {}
    })

    es.addEventListener('error', () => {
      setConnected(false)
    })

    es.onopen = () => setConnected(true)

    return () => {
      es.close()
      esRef.current = null
      setConnected(false)
    }
  }, [token])

  const markAsRead = useCallback(async (id: string) => {
    try {
      await notificationsApi.markAsRead(id)
      setNotifications((prev) => prev.map((n) => n.id === id ? { ...n, read: true } : n))
      setUnreadCount((prev) => Math.max(0, prev - 1))
    } catch {}
  }, [])

  const markAllAsRead = useCallback(async () => {
    try {
      await notificationsApi.markAllAsRead()
      setNotifications((prev) => prev.map((n) => ({ ...n, read: true })))
      setUnreadCount(0)
    } catch {}
  }, [])

  return { notifications, unreadCount, connected, markAsRead, markAllAsRead }
}
