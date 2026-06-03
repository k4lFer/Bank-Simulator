import { Link } from 'react-router-dom'

export default function LoginPage() {
  return (
    <div className="flex items-center justify-center min-h-screen bg-gradient-to-br from-blue-600 to-blue-900">
      <div className="bg-white p-10 rounded-lg shadow-xl w-full max-w-md flex flex-col gap-6">
        <h1 className="text-center text-blue-600 text-2xl font-bold">Bank Simulator</h1>
        <p className="text-center text-gray-500 text-sm">Selecciona el tipo de acceso</p>

        <Link
          to="/login/cliente"
          className="block text-center bg-blue-600 hover:bg-blue-700 text-white rounded px-4 py-3 font-medium transition-colors"
        >
          Acceso Clientes
        </Link>

        <Link
          to="/login/admin"
          className="block text-center border border-blue-600 text-blue-600 hover:bg-blue-50 rounded px-4 py-3 font-medium transition-colors"
        >
          Acceso Administradores
        </Link>
      </div>
    </div>
  )
}
