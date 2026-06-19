import { useState, useEffect } from 'react'
import { toast } from 'sonner'
import { adminApi } from '../api/admin-api'
import { useAuth } from '../hooks/useAuth'
import Button from '../components/ui/Button'
import Spinner from '../components/ui/Spinner'
import type { User } from '../models/user'

function RoleBadge({ role }: { role: string }) {
  const colors: Record<string, string> = { ADMIN: 'bg-purple-100 text-purple-700', CUSTOMER: 'bg-blue-100 text-blue-700' }
  return <span className={`text-xs font-medium px-2.5 py-0.5 rounded-full ${colors[role] ?? 'bg-gray-100 text-gray-600'}`}>{role}</span>
}

function StatusBadge({ active }: { active: boolean }) {
  return <span className={`text-xs font-medium px-2.5 py-0.5 rounded-full ${active ? 'bg-emerald-100 text-emerald-700' : 'bg-red-100 text-red-700'}`}>{active ? 'Activo' : 'Inactivo'}</span>
}

export default function AdminDashboardPage() {
  const { user: me, logout } = useAuth()
  const [users, setUsers] = useState<User[]>([])
  const [loading, setLoading] = useState(true)

  const loadUsers = () => {
    setLoading(true)
    adminApi.listUsers()
      .then((res) => setUsers(res.data.data ?? []))
      .catch(() => toast.error('Error al cargar usuarios'))
      .finally(() => setLoading(false))
  }

  useEffect(loadUsers, [])

  const handleToggleStatus = async (u: User) => {
    try {
      await adminApi.toggleUserStatus(u.id, !u.active)
      toast.success(`Usuario ${!u.active ? 'activado' : 'desactivado'}`)
      loadUsers()
    } catch {
      toast.error('Error al cambiar estado')
    }
  }

  const handleToggleRole = async (u: User) => {
    const newRole = u.role === 'ADMIN' ? 'CUSTOMER' : 'ADMIN'
    try {
      await adminApi.updateUserRole(u.id, newRole)
      toast.success(`Rol cambiado a ${newRole}`)
      loadUsers()
    } catch {
      toast.error('Error al cambiar rol')
    }
  }

  const stats = {
    total: users.length,
    admins: users.filter((u) => u.role === 'ADMIN').length,
    customers: users.filter((u) => u.role === 'CUSTOMER').length,
    active: users.filter((u) => u.active).length,
  }

  return (
    <div className="min-h-screen flex flex-col bg-gray-50">
      <header className="bg-gray-800 text-white px-8 py-4 flex items-center gap-4">
        <h1 className="text-lg font-semibold mr-auto">Bank Simulator — Admin</h1>
        <a href="/admin/ledger" className="text-sm underline opacity-80 hover:opacity-100">Libro Mayor</a>
        <span className="text-sm opacity-90">{me?.firstName} {me?.lastName}</span>
        <Button variant="ghost" onClick={logout}>Salir</Button>
      </header>

      <main className="p-8 max-w-6xl mx-auto w-full">
        {/* Stats */}
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 mb-8">
          {[
            { label: 'Total usuarios', value: stats.total, color: 'bg-blue-500' },
            { label: 'Administradores', value: stats.admins, color: 'bg-purple-500' },
            { label: 'Clientes', value: stats.customers, color: 'bg-emerald-500' },
            { label: 'Activos', value: stats.active, color: 'bg-teal-500' },
          ].map((s) => (
            <div key={s.label} className="bg-white rounded-xl shadow-sm border border-gray-100 p-5 flex flex-col gap-1">
              <span className="text-xs text-gray-400 uppercase tracking-wide">{s.label}</span>
              <span className={`text-2xl font-bold ${s.color} bg-clip-text text-transparent`}>{s.value}</span>
            </div>
          ))}
        </div>

        {/* Users table */}
        <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
          <div className="px-6 py-4 border-b border-gray-100">
            <h2 className="text-lg font-semibold text-gray-800">Usuarios</h2>
          </div>

          {loading ? (
            <div className="p-8"><Spinner text="Cargando usuarios..." /></div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="text-left text-gray-400 border-b border-gray-100 bg-gray-50">
                    <th className="px-6 py-3 font-medium">Nombre</th>
                    <th className="px-6 py-3 font-medium">Email</th>
                    <th className="px-6 py-3 font-medium">Rol</th>
                    <th className="px-6 py-3 font-medium">Estado</th>
                    <th className="px-6 py-3 font-medium text-right">Acciones</th>
                  </tr>
                </thead>
                <tbody>
                  {users.map((u) => (
                    <tr key={u.id} className="border-b border-gray-50 hover:bg-gray-50/50">
                      <td className="px-6 py-3 font-medium text-gray-800">
                        {u.firstName} {u.lastName}
                      </td>
                      <td className="px-6 py-3 text-gray-500">{u.email}</td>
                      <td className="px-6 py-3"><RoleBadge role={u.role} /></td>
                      <td className="px-6 py-3"><StatusBadge active={u.active} /></td>
                      <td className="px-6 py-3 text-right">
                        <div className="flex gap-2 justify-end">
                          <button
                            onClick={() => handleToggleRole(u)}
                            className="text-xs px-3 py-1.5 rounded border border-gray-200 hover:bg-gray-100 font-medium text-gray-600 transition-colors"
                          >
                            {u.role === 'ADMIN' ? 'Degradar' : 'Ascender'}
                          </button>
                          <button
                            onClick={() => handleToggleStatus(u)}
                            className={`text-xs px-3 py-1.5 rounded border font-medium transition-colors ${
                              u.active
                                ? 'border-red-200 text-red-600 hover:bg-red-50'
                                : 'border-emerald-200 text-emerald-600 hover:bg-emerald-50'
                            }`}
                          >
                            {u.active ? 'Desactivar' : 'Activar'}
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                  {users.length === 0 && (
                    <tr>
                      <td colSpan={5} className="px-6 py-8 text-center text-gray-400">Sin usuarios</td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </main>
    </div>
  )
}
