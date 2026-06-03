import type { User } from './user'

export interface ApiResponse<T> {
  data?: T
  messages?: string[]
  success: boolean
}

export interface LoginResponse {
  accessToken: string
  refreshToken?: string
  user: User
}

export interface Page<T> {
  pageNumber: number
  pageSize: number
  results: T[]
  totalCount: number
  totalPages: number
}
