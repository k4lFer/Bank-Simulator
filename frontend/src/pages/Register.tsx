import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import Button from '../components/ui/Button'
import Input from '../components/ui/Input'
import { Building2, UserPlus } from 'lucide-react'
import { usersApi } from '../api/users-api'
import axios from 'axios'

export default function RegisterPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState({ firstName: '', lastName: '', email: '', password: '', phone: '' })
  const [busy, setBusy] = useState(false)

  const update = (field: string) => (e: React.ChangeEvent<HTMLInputElement>) =>
    setForm((prev) => ({ ...prev, [field]: e.target.value }))

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setBusy(true)
    try {
      await usersApi.register(form)
      toast.success('Cuenta creada correctamente. Ahora inicia sesión.')
      navigate('/login/cliente')
    } catch (err) {
      const msg = axios.isAxiosError(err) && err.response?.data
        ? ((err.response.data as Record<string, unknown>).messages as string[] | undefined)?.join(', ') ?? 'Error al crear la cuenta'
        : 'Error al crear la cuenta'
      toast.error(msg)
    } finally { setBusy(false) }
  }

  return (
    <div className="flex items-center justify-center min-h-screen bg-gradient-to-br from-slate-900 via-blue-900 to-slate-900">
      <form onSubmit={handleSubmit} className="bg-white/95 backdrop-blur-sm p-8 rounded-2xl shadow-2xl w-full max-w-sm flex flex-col gap-4 border border-white/10">
        <div className="flex flex-col items-center gap-3 mb-1">
          <div className="w-12 h-12 rounded-2xl bg-blue-600 flex items-center justify-center shadow-lg shadow-blue-600/30">
            <Building2 className="w-6 h-6 text-white" />
          </div>
          <h1 className="text-center text-gray-900 text-xl font-bold">Crear Cuenta</h1>
        </div>

        <div className="grid grid-cols-2 gap-3">
          <Input label="Nombre" value={form.firstName} onChange={update('firstName')} required />
          <Input label="Apellido" value={form.lastName} onChange={update('lastName')} required />
        </div>
        <Input label="Email" type="email" placeholder="tu@email.com" value={form.email} onChange={update('email')} required />
        <Input label="Contraseña" type="password" placeholder="Mínimo 6 caracteres" value={form.password} onChange={update('password')} required />
        <Input label="Teléfono (opcional)" type="tel" placeholder="+51 999 999 999" value={form.phone} onChange={update('phone')} />

        <Button type="submit" disabled={busy} loading={busy}>
          <UserPlus className="w-4 h-4" /> Crear cuenta
        </Button>

        <p className="text-center text-sm text-gray-500">
          ¿Ya tienes cuenta?{' '}
          <Link to="/login/cliente" className="text-blue-600 hover:underline font-medium">Inicia sesión</Link>
        </p>
      </form>
    </div>
  )
}
