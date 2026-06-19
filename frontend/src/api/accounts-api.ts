import client from './client'
import type { ApiResponse } from '../models/api'
import type { Account, Movement } from '../models/account'

export const accountsApi = {
  listFromAccounts() {
    return client.get<ApiResponse<Account[]>>('/accounts/me')
  },

  getById(id: string) {
    return client.get<ApiResponse<Account>>(`/accounts/${id}`)
  },

  movements(accountId: string) {
    return client.get<ApiResponse<Movement[]>>(`/accounts/${accountId}/movements`)
  },

  create(currency: string, cardId: string) {
    return client.post<ApiResponse<{ id: string; accountNumber: string; balance: number; currency: string; status: string }>>('/accounts/create', { currency, cardId })
  },

  deposit(accountNumber: string, currency: string, amount: number) {
    return client.post<ApiResponse<{ movementNumber: string; accountId: string; accountNumber: string; type: string; amount: number; balanceAfter: number }>>('/accounts/deposit', { accountNumber, currency, amount })
  },

  block(id: string) {
    return client.patch<ApiResponse<{ id: string; accountNumber: string; status: string; updatedAt: string }>>(`/accounts/${id}/block`)
  },
}
