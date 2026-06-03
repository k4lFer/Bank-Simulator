import { createContext, useState, useEffect, type ReactNode } from 'react'
import { usersApi } from '../api/users-api'
import type { User } from '../models/user'

interface AuthContextType {
  user: User | null
  token: string | null
  login: (email: string, password: string) => Promise<void>
  logout: () => void
  loading: boolean
}

export const AuthContext = createContext<AuthContextType | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [token, setToken] = useState<string | null>(localStorage.getItem('accessToken'))
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!token) { setLoading(false); return }
    usersApi.me()
      .then((res) => setUser(res.data.data ?? null))
      .catch(() => localStorage.removeItem('accessToken'))
      .finally(() => setLoading(false))
  }, [token])

  const login = async (email: string, password: string) => {
    const res = await usersApi.login(email, password)
    const data = res.data.data
    if (!data?.accessToken) throw new Error('No accessToken in response')
    localStorage.setItem('accessToken', data.accessToken)
    setToken(data.accessToken)
    setUser(data.user)
  }

  const logout = () => {
    localStorage.removeItem('accessToken')
    setToken(null)
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{ user, token, login, logout, loading }}>
      {children}
    </AuthContext.Provider>
  )
}
