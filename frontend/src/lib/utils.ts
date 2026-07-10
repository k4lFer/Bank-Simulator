import { type ClassValue, clsx } from 'clsx'
import { twMerge } from 'tailwind-merge'

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

export const currencySymbol: Record<string, string> = { USD: '$', PEN: 'S/', EUR: '\u20AC' }
export const currencies = ['USD', 'PEN', 'EUR'] as const

export function formatCurrency(amount: number, currency = 'USD'): string {
  const symbol = currencySymbol[currency] ?? '$'
  return `${symbol}${amount.toLocaleString(undefined, { minimumFractionDigits: 2 })}`
}

export function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleString()
}

export function truncate(str: string, len = 8): string {
  return str.length > len ? str.slice(0, len) + '...' : str
}
