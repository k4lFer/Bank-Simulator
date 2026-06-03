import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { toast } from 'sonner'
import { useAuth } from '../hooks/useAuth'
import Button from '../components/ui/Button'
import Input from '../components/ui/Input'
import axios from 'axios'

export default function AdminLoginPage() {
  const { login } = useAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [busy, setBusy] = useState(false)

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setBusy(true)
    try {
      await login(email, password)
      toast.success('Sesión iniciada correctamente')
    } catch (err) {
      const msg =
        axios.isAxiosError(err) && err.response?.data
          ? ((err.response.data as Record<string, unknown>).messages as string[] | undefined)?.join(', ') ?? 'Credenciales inválidas'
          : 'Credenciales inválidas'
      toast.error(msg)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="flex items-center justify-center min-h-screen bg-gradient-to-br from-gray-800 to-gray-900">
      <form onSubmit={handleSubmit} className="bg-white p-10 rounded-lg shadow-xl w-full max-w-sm flex flex-col gap-4">
        <h1 className="text-center text-gray-800 text-2xl font-bold mb-2">Bank Simulator</h1>
        <p className="text-center text-gray-500 text-sm -mt-2 mb-2">Administración</p>
        <Input
          type="email"
          placeholder="Email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
        />
        <Input
          type="password"
          placeholder="Contraseña"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
        />
        <Button type="submit" disabled={busy}>
          {busy ? 'Ingresando...' : 'Ingresar'}
        </Button>
        <Link to="/" className="text-center text-sm text-gray-400 hover:text-gray-600">
          &larr; Volver
        </Link>
      </form>
    </div>
  )
}
