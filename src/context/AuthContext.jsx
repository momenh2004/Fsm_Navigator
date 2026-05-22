import { createContext, useContext, useState, useEffect } from 'react'
import { authAPI } from '../services/api'

const AuthContext = createContext({
  admin: null, loading: true, otpEmail: null,
  login: async () => {}, verifyOtp: async () => {}, logout: () => {},
})

export function AuthProvider({ children }) {
  const [admin, setAdmin]       = useState(null)
  const [loading, setLoading]   = useState(true)
  const [otpEmail, setOtpEmail] = useState(null)

  useEffect(() => {
    const stored = localStorage.getItem('fsm_admin')
    const token  = localStorage.getItem('fsm_token')
    if (stored && token) {
      try { setAdmin(JSON.parse(stored)) } catch { localStorage.clear() }
    }
    setLoading(false)
  }, [])

  const login = async (email, password) => {
    const res = await authAPI.login(email, password)
    // Backend returns { requiresOtp: true, email } or { token, admin }
    if (res.data.requiresOtp) {
      setOtpEmail(email)
      return { requiresOtp: true }
    }
    _saveSession(res.data)
    return { requiresOtp: false }
  }

  const verifyOtp = async (otp) => {
    const res = await authAPI.verifyOtp(otpEmail, otp)
    _saveSession(res.data)
  }

  const logout = () => {
    authAPI.logout().catch(() => {})
    localStorage.removeItem('fsm_token')
    localStorage.removeItem('fsm_admin')
    setAdmin(null)
    setOtpEmail(null)
  }

  const _saveSession = (data) => {
    const adminObj = data.admin || data.user || { email: data.email, role: data.role }
    localStorage.setItem('fsm_token', data.token)
    localStorage.setItem('fsm_admin', JSON.stringify(adminObj))
    setAdmin(adminObj)
    setOtpEmail(null)
  }

  return (
    <AuthContext.Provider value={{ admin, loading, otpEmail, login, verifyOtp, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => useContext(AuthContext)
