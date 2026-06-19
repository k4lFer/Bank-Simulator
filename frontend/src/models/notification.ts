export interface Notification {
  id: string
  userId: string
  type: 'DEPOSIT' | 'CARD_DEPOSIT' | 'TRANSFER_SENT' | 'TRANSFER_RECEIVED' | 'TRANSFER_REJECTED'
  title: string
  message: string
  amount: number
  currency: string
  relatedAccount: string
  read: boolean
  createdAt: string
}
