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

  create(currency: string, pin6: string, pin4: string) {
    return client.post<ApiResponse<{ id: string; accountNumber: string; balance: number; currency: string; status: string }>>('/accounts/create', { currency, pin6, pin4 })
  },

  deposit(accountNumber: string, currency: string, pin4: string, amount: number) {
    return client.post<ApiResponse<{ movementNumber: string; accountId: string; accountNumber: string; type: string; amount: number; balanceAfter: number }>>('/accounts/deposit', { accountNumber, currency, pin4, amount })
  },
}
