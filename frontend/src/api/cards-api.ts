import client from './client'
import type { ApiResponse } from '../models/api'
import type { Card, CardDetail } from '../models/card'

export const cardsApi = {
  list() {
    return client.get<ApiResponse<Card[]>>('/cards/me')
  },

  getById(id: string) {
    return client.get<ApiResponse<CardDetail>>(`/cards/${id}`)
  },

  issue(pin4: string, pin6: string) {
    return client.post<ApiResponse<Card>>('/cards/issue', { pin4, pin6 })
  },

  deposit(cardId: string, accountNumber: string, amount: number, pin4: string, pin6: string) {
    return client.post<ApiResponse<{ movementNumber: string; accountNumber: string; type: string; amount: number; balanceAfter: number }>>(`/cards/${cardId}/deposit`, { accountNumber, amount, pin4, pin6 })
  },

  changePin(cardId: string, currentPin4: string, newPin4: string) {
    return client.put<ApiResponse<Card>>(`/cards/${cardId}/pin`, { currentPin4, newPin4 })
  },

  block(cardId: string) {
    return client.patch<ApiResponse<Card>>(`/cards/${cardId}/block`)
  },
}
