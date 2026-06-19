export interface LedgerEntry {
  id: number
  transferId: string | null
  accountNumber: string
  entryType: 'DR' | 'CR'
  amount: number
  currency: string
  createdAt: string
}

export interface DailyReportItem {
  accountNumber: string
  currency: string
  openingBalance: number
  totalDebits: number
  totalCredits: number
  closingBalance: number
}

export interface DailyReport {
  date: string
  totalAccounts: number
  totalEntries: number
  accounts: DailyReportItem[]
}

export interface AccountBalance {
  accountNumber: string
  currency: string
  balance: number
  calculatedAt: string
}
