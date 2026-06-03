import { Routes, Route, Navigate } from 'react-router-dom'
import ProtectedRoute from '../components/layout/ProtectedRoute'
import AdminRoute from '../components/layout/AdminRoute'
import PublicRoute from '../components/layout/PublicRoute'
import LoginPage from '../pages/Login'
import CustomerLoginPage from '../pages/CustomerLogin'
import AdminLoginPage from '../pages/AdminLogin'
import RegisterPage from '../pages/Register'
import DashboardPage from '../pages/Dashboard'
import AdminDashboardPage from '../pages/AdminDashboard'
import AccountDetailPage from '../pages/AccountDetailPage'

export default function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<PublicRoute><LoginPage /></PublicRoute>} />
      <Route path="/login" element={<Navigate to="/" replace />} />
      <Route path="/login/cliente" element={<PublicRoute><CustomerLoginPage /></PublicRoute>} />
      <Route path="/login/admin" element={<PublicRoute><AdminLoginPage /></PublicRoute>} />
      <Route path="/registro" element={<PublicRoute><RegisterPage /></PublicRoute>} />
      <Route path="/dashboard" element={<ProtectedRoute><DashboardPage /></ProtectedRoute>} />
      <Route path="/accounts/:id" element={<ProtectedRoute><AccountDetailPage /></ProtectedRoute>} />
      <Route path="/admin" element={<AdminRoute><AdminDashboardPage /></AdminRoute>} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
