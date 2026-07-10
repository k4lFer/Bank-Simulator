import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { toast } from 'sonner'
import { useAuth } from '../hooks/useAuth'
import Button from '../components/ui/Button'
import Input from '../components/ui/Input'
import { Shield, Mail, Lock, LogIn } from 'lucide-react'
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
      const msg = axios.isAxiosError(err) && err.response?.data
        ? ((err.response.data as Record<string, unknown>).messages as string[] | undefined)?.join(', ') ?? 'Credenciales inválidas'
        : 'Credenciales inválidas'
      toast.error(msg)
    } finally { setBusy(false) }
  }

  return (
    <div className="flex items-center justify-center min-h-screen bg-gradient-to-br from-slate-900 via-gray-800 to-slate-900">
      <form onSubmit={handleSubmit} className="bg-white/95 backdrop-blur-sm p-8 rounded-2xl shadow-2xl w-full max-w-sm flex flex-col gap-5 border border-white/10">
        <div className="flex flex-col items-center gap-3 mb-2">
          <div className="w-12 h-12 rounded-2xl bg-gray-800 flex items-center justify-center shadow-lg shadow-gray-800/30">
            <Shield className="w-6 h-6 text-white" />
          </div>
          <h1 className="text-center text-gray-900 text-xl font-bold">BankSim Admin</h1>
          <p className="text-center text-gray-500 text-sm -mt-1">Panel de Administración</p>
        </div>

        <Input
          label="Email"
          type="email"
          placeholder="admin@email.com"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          icon={<Mail className="w-4 h-4" />}
          required
        />
        <Input
          label="Contraseña"
          type="password"
          placeholder="••••••••"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          icon={<Lock className="w-4 h-4" />}
          required
        />

        <Button type="submit" disabled={busy} loading={busy}>
          <LogIn className="w-4 h-4" /> Ingresar
        </Button>

        <Link to="/" className="text-center text-xs text-gray-400 hover:text-gray-600 transition-colors">
          &larr; Volver
        </Link>
      </form>
    </div>
  )
}
