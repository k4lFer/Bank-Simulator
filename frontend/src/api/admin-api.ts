import client from './client'
import type { ApiResponse } from '../models/api'
import type { User } from '../models/user'

export const adminApi = {
  listUsers() {
    return client.get<ApiResponse<User[]>>('/admin/users')
  },

  getUser(id: string) {
    return client.get<ApiResponse<User>>(`/admin/users/${id}`)
  },

  toggleUserStatus(id: string, active: boolean) {
    return client.put<ApiResponse<User>>(`/admin/users/${id}/status`, { active })
  },

  updateUserRole(id: string, role: string) {
    return client.put<ApiResponse<User>>(`/admin/users/${id}/role`, role, {
      headers: { 'Content-Type': 'text/plain' },
    })
  },
}
