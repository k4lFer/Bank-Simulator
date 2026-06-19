import { useAuth } from '../../hooks/useAuth'
import Button from '../ui/Button'
import NotificationBell from '../notifications/NotificationBell'
import type { ReactNode } from 'react'

export default function AppLayout({ children }: { children: ReactNode }) {
  const { user, logout } = useAuth()

  return (
    <div className="min-h-screen flex flex-col">
      <header className="bg-blue-600 text-white px-8 py-4 flex items-center gap-4">
        <h1 className="text-lg font-semibold mr-auto">Bank Simulator</h1>
        <NotificationBell />
        <span className="text-sm opacity-90">{user?.firstName} {user?.lastName} ({user?.role})</span>
        <Button variant="ghost" onClick={logout}>Salir</Button>
      </header>
      <main className="p-8 max-w-5xl mx-auto w-full">{children}</main>
    </div>
  )
}
