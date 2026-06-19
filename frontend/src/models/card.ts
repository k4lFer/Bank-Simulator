export interface CardAccountLink {
  cardId: string
  accountId: string
  accountNumber: string
  currency: string
  isPrimary: boolean
}

export interface Card {
  id: string
  userId: string
  pan: string
  expiryDate: string
  status: string
  dailyLimit: number
  createdAt: string
}

export interface CardDetail extends Card {
  accounts: CardAccountLink[]
}
