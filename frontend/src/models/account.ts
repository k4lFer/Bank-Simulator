export interface Account {
  id: string
  accountNumber: string
  balance: number
  currency: string
  status: string
  userId?: string
  createdAt?: string
}

export interface Movement {
  id: number
  movementNumber: string
  type: 'CREDIT' | 'DEBIT'
  amount: number
  balanceAfter: number
  createdAt: string
}


