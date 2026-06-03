import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import Button from '../components/ui/Button'
import Input from '../components/ui/Input'
import { usersApi } from '../api/users-api'
import axios from 'axios'

export default function RegisterPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState({
    firstName: '',
    lastName: '',
    email: '',
    password: '',
    phone: '',
  })
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
      const msg =
        axios.isAxiosError(err) && err.response?.data
          ? ((err.response.data as Record<string, unknown>).messages as string[] | undefined)?.join(', ') ?? 'Error al crear la cuenta'
          : 'Error al crear la cuenta'
      toast.error(msg)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="flex items-center justify-center min-h-screen bg-gradient-to-br from-blue-600 to-blue-900">
      <form onSubmit={handleSubmit} className="bg-white p-10 rounded-lg shadow-xl w-full max-w-sm flex flex-col gap-4">
        <h1 className="text-center text-blue-600 text-2xl font-bold mb-2">Crear Cuenta</h1>

        <Input label="Nombre" value={form.firstName} onChange={update('firstName')} required />
        <Input label="Apellido" value={form.lastName} onChange={update('lastName')} required />
        <Input label="Email" type="email" value={form.email} onChange={update('email')} required />
        <Input label="Contraseña" type="password" value={form.password} onChange={update('password')} required />
        <Input label="Teléfono (opcional)" type="tel" value={form.phone} onChange={update('phone')} />

        <Button type="submit" disabled={busy}>
          {busy ? 'Creando...' : 'Crear cuenta'}
        </Button>

        <p className="text-center text-sm text-gray-500">
          ¿Ya tienes cuenta?{' '}
          <Link to="/login/cliente" className="text-blue-600 hover:underline">
            Inicia sesión
          </Link>
        </p>
      </form>
    </div>
  )
}
