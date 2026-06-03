import { Navigate } from 'react-router-dom'
import { useAuth } from '../../hooks/useAuth'
import Spinner from '../ui/Spinner'
import type { ReactNode } from 'react'

export default function PublicRoute({ children }: { children: ReactNode }) {
  const { user, loading } = useAuth()
  if (loading) return <Spinner />
  if (!user) return children
  const redirect = user.role === 'ADMIN' ? '/admin' : '/dashboard'
  return <Navigate to={redirect} replace />
}
