import { useState, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../components/ui/Toast'
import Spinner from '../components/ui/Spinner'
import logo from '../assets/logo.svg'

export default function OtpVerify() {
  const [otp, setOtp]         = useState(['', '', '', '', '', ''])
  const [loading, setLoading] = useState(false)
  const r0 = useRef(), r1 = useRef(), r2 = useRef()
  const r3 = useRef(), r4 = useRef(), r5 = useRef()
  const refs = [r0, r1, r2, r3, r4, r5]
  const { verifyOtp, otpEmail } = useAuth()
  const toast = useToast()
  const navigate = useNavigate()

  const handleChange = (i, val) => {
    if (!/^\d?$/.test(val)) return
    const next = [...otp]; next[i] = val; setOtp(next)
    if (val && i < 5) refs[i + 1].current?.focus()
  }

  const handleKeyDown = (i, e) => {
    if (e.key === 'Backspace' && !otp[i] && i > 0) refs[i - 1].current?.focus()
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    const code = otp.join('')
    if (code.length < 6) return toast('Entrez les 6 chiffres', 'error')
    setLoading(true)
    try {
      await verifyOtp(code)
      navigate('/')
    } catch {
      toast('Code OTP invalide ou expiré', 'error')
      setOtp(['', '', '', '', '', ''])
      refs[0].current?.focus()
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center p-4" style={{ background: '#070C18' }}>
      <div className="w-full max-w-sm">
        <div className="text-center mb-8">
          <img src={logo} alt="FSM Navigator" className="w-16 h-16 rounded-2xl mx-auto mb-4" />
          <h1 className="text-2xl font-bold text-white">Vérification OTP</h1>
          <p className="text-sm mt-1" style={{ color: '#3b6cbd' }}>
            Code envoyé à <span className="text-blue-400">{otpEmail || 'votre email'}</span>
          </p>
        </div>

        <form onSubmit={handleSubmit} className="card space-y-6">
          <div className="flex gap-2 justify-center">
            {otp.map((v, i) => (
              <input key={i} ref={refs[i]} type="text" maxLength={1}
                className="w-11 h-12 text-center text-xl font-bold text-white rounded-lg focus:outline-none focus:border-blue-500 transition-colors"
                style={{ background: '#0D1530', border: '1px solid #1a2845' }}
                value={v}
                onChange={e => handleChange(i, e.target.value)}
                onKeyDown={e => handleKeyDown(i, e)}/>
            ))}
          </div>

          <button type="submit" disabled={loading}
            className="btn-primary w-full flex items-center justify-center gap-2">
            {loading ? <Spinner size="sm"/> : null}
            {loading ? 'Vérification...' : 'Vérifier le code'}
          </button>
        </form>
      </div>
    </div>
  )
}
