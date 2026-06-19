import client from './client'
import type { ApiResponse } from '../models/api'
import type { AccountBalance, DailyReport, LedgerEntry } from '../models/ledger'

export const ledgerApi = {
  list() {
    return client.get<ApiResponse<LedgerEntry[]>>('/ledger')
  },

  getById(id: number) {
    return client.get<ApiResponse<LedgerEntry>>(`/ledger/${id}`)
  },

  byAccount(accountNumber: string) {
    return client.get<ApiResponse<LedgerEntry[]>>('/ledger/by-account', { params: { accountNumber } })
  },

  byTransfer(transferId: string) {
    return client.get<ApiResponse<LedgerEntry[]>>('/ledger/by-transfer', { params: { transferId } })
  },

  dailyReport(date: string) {
    return client.get<ApiResponse<DailyReport>>('/ledger/daily-report', { params: { date } })
  },

  balance(accountNumber: string) {
    return client.get<ApiResponse<AccountBalance>>('/ledger/balance', { params: { accountNumber } })
  },
}
