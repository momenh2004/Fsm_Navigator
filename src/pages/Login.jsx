import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../components/ui/Toast'
import Spinner from '../components/ui/Spinner'
import logo from '../assets/logo.svg'

export default function Login() {
  const [email, setEmail]       = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading]   = useState(false)
  const { login } = useAuth()
  const toast = useToast()
  const navigate = useNavigate()

  const handleSubmit = async (e) => {
    e.preventDefault()
    setLoading(true)
    try {
      const res = await login(email, password)
      if (res.requiresOtp) navigate('/otp')
      else navigate('/')
    } catch (err) {
      toast(err?.response?.data?.message || 'Email ou mot de passe incorrect', 'error')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center p-4" style={{ background: '#070C18' }}>
      <div className="w-full max-w-sm">
        {/* Logo */}
        <div className="text-center mb-8">
          <img src={logo} alt="FSM Navigator" className="w-16 h-16 rounded-2xl mx-auto mb-4" />
          <h1 className="text-2xl font-bold text-white">FSM Navigator</h1>
          <p className="text-sm mt-1" style={{ color: '#3b6cbd' }}>Tableau de bord administrateur</p>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="card space-y-4">
          <h2 className="text-lg font-semibold text-white mb-2">Connexion</h2>

          <div>
            <label className="text-sm mb-1 block" style={{ color: '#3b6cbd' }}>Email</label>
            <input type="email" className="input" placeholder="admin@fsm.tn"
              value={email} onChange={e => setEmail(e.target.value)} required/>
          </div>

          <div>
            <label className="text-sm mb-1 block" style={{ color: '#3b6cbd' }}>Mot de passe</label>
            <input type="password" className="input" placeholder="••••••••"
              value={password} onChange={e => setPassword(e.target.value)} required/>
          </div>

          <button type="submit" disabled={loading}
            className="btn-primary w-full flex items-center justify-center gap-2 mt-2">
            {loading ? <Spinner size="sm"/> : null}
            {loading ? 'Connexion...' : 'Se connecter'}
          </button>
        </form>

        <p className="text-center text-xs mt-6" style={{ color: '#2d4a7a' }}>
          FSM Navigator — Faculté des Sciences de Monastir
        </p>
      </div>
    </div>
  )
}
