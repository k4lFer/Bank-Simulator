import { useState, useEffect } from 'react'
import { accountsApi } from '../api/accounts-api'
import type { Account } from '../models/account'

export function useAccounts() {
  const [accounts, setAccounts] = useState<Account[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    accountsApi.list()
      .then((res) => setAccounts(res.data.data ?? []))
      .catch((err) => setError(err instanceof Error ? err.message : 'Error al cargar cuentas'))
      .finally(() => setLoading(false))
  }, [])

  return { accounts, loading, error }
}
