import client from './client'
import type { ApiResponse } from '../models/api'

export interface TransferRequest {
  fromAccount?: string
  toAccount: string
  amount: number
  currency: string
  description?: string
  pin4?: string
  cardId?: string
}

export interface TransferResponse {
  transferId: string
  userId: string
  toUserId: string | null
  fromAccount: string
  toAccount: string
  amount: number
  currency: string
  description: string
  status: string
  rejectionReason: string | null
  createdAt: string
}

export interface QueryResult<T> {
  results: T
  totalCount: number
  totalPages: number
  pageNumber: number
  pageSize: number
}

function idempotencyKey(): string {
  return crypto.randomUUID()
}

export const transfersApi = {
  internal(data: TransferRequest) {
    return client.post<ApiResponse<TransferResponse>>('/transfers/internal', data, {
      headers: { 'Idempotency-Key': idempotencyKey() }
    })
  },

  external(data: TransferRequest) {
    return client.post<ApiResponse<TransferResponse>>('/transfers/external', data, {
      headers: { 'Idempotency-Key': idempotencyKey() }
    })
  },

  get(transferId: string) {
    return client.get<ApiResponse<TransferResponse>>(`/transfers/${transferId}`)
  },

  byAccount(accountNumber: string) {
    return client.get<ApiResponse<QueryResult<TransferResponse[]>>>(`/transfers/by-account/${accountNumber}`)
  },

  cardPayment(data: TransferRequest) {
    return client.post<ApiResponse<TransferResponse>>('/transfers/card-payment', data, {
      headers: { 'Idempotency-Key': idempotencyKey() }
    })
  },
}
