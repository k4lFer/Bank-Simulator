import client from './client'
import type { ApiResponse, LoginResponse } from '../models/api'
import type { User } from '../models/user'

export const usersApi = {
  login(email: string, password: string) {
    return client.post<ApiResponse<LoginResponse>>('/auth/login', { email, password })
  },

  register(body: { firstName: string; lastName: string; email: string; password: string; phone?: string }) {
    return client.post<ApiResponse<string>>('/auth/register', body)
  },

  me() {
    return client.get<ApiResponse<User>>('/users/me')
  },
}
