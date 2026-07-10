import { useState, useEffect } from 'react'
import { toast } from 'sonner'
import { Users, Shield, UserCheck, Activity, BookOpen, RefreshCw } from 'lucide-react'
import { adminApi } from '../api/admin-api'
import { useAuth } from '../hooks/useAuth'
import Button from '../components/ui/Button'
import Spinner from '../components/ui/Spinner'
import Card from '../components/ui/Card'
import Badge from '../components/ui/Badge'
import type { User } from '../models/user'

export default function AdminDashboardPage() {
  const { user: me, logout } = useAuth()
  const [users, setUsers] = useState<User[]>([])
  const [loading, setLoading] = useState(true)
  const [actionBusy, setActionBusy] = useState<string | null>(null)

  const loadUsers = () => {
    setLoading(true)
    adminApi.listUsers()
      .then((res) => setUsers(res.data.data ?? []))
      .catch(() => toast.error('Error al cargar usuarios'))
      .finally(() => setLoading(false))
  }

  useEffect(loadUsers, [])

  const handleToggleStatus = async (u: User) => {
    setActionBusy(u.id)
    try {
      await adminApi.toggleUserStatus(u.id, !u.active)
      toast.success(`Usuario ${!u.active ? 'activado' : 'desactivado'}`)
      loadUsers()
    } catch { toast.error('Error al cambiar estado') }
    finally { setActionBusy(null) }
  }

  const handleToggleRole = async (u: User) => {
    setActionBusy(u.id)
    const newRole = u.role === 'ADMIN' ? 'CUSTOMER' : 'ADMIN'
    try {
      await adminApi.updateUserRole(u.id, newRole)
      toast.success(`Rol cambiado a ${newRole}`)
      loadUsers()
    } catch { toast.error('Error al cambiar rol') }
    finally { setActionBusy(null) }
  }

  const stats = [
    { label: 'Total', value: users.length, icon: Users, color: 'text-blue-600' },
    { label: 'Administradores', value: users.filter((u) => u.role === 'ADMIN').length, icon: Shield, color: 'text-purple-600' },
    { label: 'Clientes', value: users.filter((u) => u.role === 'CUSTOMER').length, icon: UserCheck, color: 'text-emerald-600' },
    { label: 'Activos', value: users.filter((u) => u.active).length, icon: Activity, color: 'text-teal-600' },
  ]

  return (
    <div className="min-h-screen flex flex-col bg-gray-50">
      <header className="bg-white border-b border-gray-200 px-4 sm:px-8 py-4 flex items-center gap-4 sticky top-0 z-40">
        <Shield className="w-6 h-6 text-blue-600" />
        <h1 className="text-lg font-bold text-gray-900 mr-auto">Panel Admin</h1>
        <a href="/admin/ledger" className="inline-flex items-center gap-1 text-sm text-blue-600 hover:text-blue-700 font-medium">
          <BookOpen className="w-4 h-4" /> Libro Mayor
        </a>
        <span className="text-sm text-gray-500">{me?.firstName} {me?.lastName}</span>
        <Button variant="ghost" onClick={logout}>Salir</Button>
      </header>

      <main className="p-4 sm:p-8 max-w-6xl mx-auto w-full">
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 mb-8">
          {stats.map((s) => (
            <Card key={s.label} padding="md">
              <div className="flex items-start justify-between">
                <div>
                  <span className="text-xs text-gray-400 uppercase tracking-wide">{s.label}</span>
                  <p className={`text-2xl font-bold mt-1 ${s.color}`}>{s.value}</p>
                </div>
                <s.icon className={`w-5 h-5 ${s.color} opacity-50`} />
              </div>
            </Card>
          ))}
        </div>

        <Card padding="sm">
          <div className="px-5 py-4 border-b border-gray-100 flex items-center justify-between">
            <div>
              <h2 className="text-base font-semibold text-gray-900">Usuarios</h2>
              <p className="text-xs text-gray-400 mt-0.5">{users.length} registrados</p>
            </div>
            <button onClick={loadUsers} className="p-2 rounded-lg hover:bg-gray-100 text-gray-400 transition-colors" title="Recargar">
              <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
            </button>
          </div>

          {loading ? (
            <Spinner text="Cargando usuarios..." />
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="text-left text-gray-400 border-b border-gray-100 bg-gray-50/80">
                    <th className="px-5 py-3 font-medium text-xs uppercase tracking-wider">Nombre</th>
                    <th className="px-5 py-3 font-medium text-xs uppercase tracking-wider">Email</th>
                    <th className="px-5 py-3 font-medium text-xs uppercase tracking-wider">Rol</th>
                    <th className="px-5 py-3 font-medium text-xs uppercase tracking-wider">Estado</th>
                    <th className="px-5 py-3 font-medium text-xs uppercase tracking-wider text-right">Acciones</th>
                  </tr>
                </thead>
                <tbody>
                  {users.map((u) => (
                    <tr key={u.id} className="border-b border-gray-50 hover:bg-gray-50/50 transition-colors">
                      <td className="px-5 py-3 font-medium text-gray-800">{u.firstName} {u.lastName}</td>
                      <td className="px-5 py-3 text-gray-500">{u.email}</td>
                      <td className="px-5 py-3">
                        <Badge variant={u.role === 'ADMIN' ? 'purple' : 'info'} label={u.role} />
                      </td>
                      <td className="px-5 py-3">
                        <Badge variant={u.active ? 'success' : 'danger'} label={u.active ? 'Activo' : 'Inactivo'} />
                      </td>
                      <td className="px-5 py-3 text-right">
                        <div className="flex gap-2 justify-end">
                          <Button
                            size="sm"
                            variant="secondary"
                            onClick={() => handleToggleRole(u)}
                            disabled={actionBusy === u.id}
                          >
                            {u.role === 'ADMIN' ? 'Degradar' : 'Ascender'}
                          </Button>
                          <Button
                            size="sm"
                            variant={u.active ? 'danger' : 'success'}
                            onClick={() => handleToggleStatus(u)}
                            disabled={actionBusy === u.id}
                          >
                            {u.active ? 'Desactivar' : 'Activar'}
                          </Button>
                        </div>
                      </td>
                    </tr>
                  ))}
                  {users.length === 0 && (
                    <tr>
                      <td colSpan={5} className="px-5 py-8 text-center text-gray-400">Sin usuarios</td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          )}
        </Card>
      </main>
    </div>
  )
}
