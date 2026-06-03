import client from './client'
import type { ApiResponse } from '../models/api'

export interface TransferRequest {
  fromAccount: string
  toAccount: string
  amount: number
  currency: string
  description?: string
}

export interface TransferResponse {
  transferId: string
  fromAccount: string
  toAccount: string
  amount: number
  currency: string
  description: string
  status: string
  createdAt: string
}

export const transfersApi = {
  create(data: TransferRequest) {
    return client.post<ApiResponse<TransferResponse>>('/transfers', data)
  },

  get(transferId: string) {
    return client.get<ApiResponse<TransferResponse>>(`/transfers/${transferId}`)
  },

  byAccount(accountNumber: string) {
    return client.get<ApiResponse<TransferResponse[]>>(`/transfers/by-account/${accountNumber}`)
  },
}
