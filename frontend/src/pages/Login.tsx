import { Link } from 'react-router-dom'
import { Building2, LogIn, Shield } from 'lucide-react'

export default function LoginPage() {
  return (
    <div className="flex items-center justify-center min-h-screen bg-gradient-to-br from-slate-900 via-blue-900 to-slate-900">
      <div className="bg-white/95 backdrop-blur-sm p-10 rounded-2xl shadow-2xl w-full max-w-md flex flex-col gap-6 border border-white/10">
        <div className="flex flex-col items-center gap-3">
          <div className="w-14 h-14 rounded-2xl bg-blue-600 flex items-center justify-center shadow-lg shadow-blue-600/30">
            <Building2 className="w-7 h-7 text-white" />
          </div>
          <h1 className="text-center text-gray-900 text-2xl font-bold">BankSim</h1>
          <p className="text-center text-gray-500 text-sm">Plataforma bancaria simulada</p>
        </div>

        <div className="flex flex-col gap-3 mt-2">
          <Link
            to="/login/cliente"
            className="inline-flex items-center justify-center gap-2 bg-blue-600 hover:bg-blue-700 text-white rounded-xl px-4 py-3.5 font-medium transition-all duration-200 shadow-sm hover:shadow-md active:scale-[0.99]"
          >
            <LogIn className="w-4 h-4" />
            Acceso Clientes
          </Link>

          <Link
            to="/login/admin"
            className="inline-flex items-center justify-center gap-2 border-2 border-gray-200 text-gray-700 hover:bg-gray-50 hover:border-gray-300 rounded-xl px-4 py-3.5 font-medium transition-all duration-200 active:scale-[0.99]"
          >
            <Shield className="w-4 h-4" />
            Acceso Administradores
          </Link>
        </div>

        <p className="text-center text-xs text-gray-400">
          ¿No tienes cuenta?{' '}
          <Link to="/registro" className="text-blue-600 hover:underline font-medium">
            Regístrate
          </Link>
        </p>
      </div>
    </div>
  )
}
