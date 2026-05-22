import { Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider, useAuth } from './context/AuthContext'
import { ToastProvider } from './components/ui/Toast'
import Login      from './pages/Login'
import OtpVerify  from './pages/OtpVerify'
import Dashboard  from './pages/Dashboard'
import Coverage   from './pages/Coverage'
import Users      from './pages/Users'
import Blocs      from './pages/Blocs'
import Spinner    from './components/ui/Spinner'

function ProtectedRoute({ children }) {
  const { admin, loading } = useAuth()
  if (loading) return (
    <div className="min-h-screen flex items-center justify-center bg-slate-950">
      <Spinner size="lg"/>
    </div>
  )
  return admin ? children : <Navigate to="/login" replace/>
}

function OtpRoute({ children }) {
  const { otpEmail } = useAuth()
  return otpEmail ? children : <Navigate to="/login" replace/>
}

function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<Login/>}/>
      <Route path="/otp"   element={<OtpRoute><OtpVerify/></OtpRoute>}/>
      <Route path="/"          element={<ProtectedRoute><Dashboard/></ProtectedRoute>}/>
      <Route path="/coverage"  element={<ProtectedRoute><Coverage/></ProtectedRoute>}/>
      <Route path="/users"     element={<ProtectedRoute><Users/></ProtectedRoute>}/>
      <Route path="/blocs"     element={<ProtectedRoute><Blocs/></ProtectedRoute>}/>
      <Route path="*"          element={<Navigate to="/" replace/>}/>
    </Routes>
  )
}

export default function App() {
  return (
    <AuthProvider>
      <ToastProvider>
        <AppRoutes/>
      </ToastProvider>
    </AuthProvider>
  )
}
