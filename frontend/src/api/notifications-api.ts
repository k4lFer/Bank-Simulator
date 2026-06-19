import client from './client'
import type { ApiResponse } from '../models/api'
import type { Notification } from '../models/notification'

interface QueryResult<T> {
  results: T
  totalCount: number
  totalPages: number
  pageNumber: number
  pageSize: number
}

export const notificationsApi = {
  list(page = 0, size = 20) {
    return client.get<ApiResponse<QueryResult<Notification[]>>>(`/notifications?page=${page}&size=${size}`)
  },

  unreadCount() {
    return client.get<ApiResponse<{ count: number }>>('/notifications/unread-count')
  },

  markAsRead(id: string) {
    return client.patch<ApiResponse<Notification>>(`/notifications/${id}/read`)
  },

  markAllAsRead() {
    return client.patch<ApiResponse<void>>('/notifications/read-all')
  },
}
